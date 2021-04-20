package com.bluflametech.example

import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancing
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClientBuilder
import com.amazonaws.services.elasticloadbalancing.model.DescribeInstanceHealthRequest
import com.amazonaws.services.elasticloadbalancing.model.DescribeLoadBalancersRequest
import com.amazonaws.services.elasticloadbalancing.model.LoadBalancerDescription
import com.bluflametech.test.SyncInfraTest
import com.bluflametech.test.tf.TerraformSpecification
import spock.lang.Stepwise

import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.concurrent.Callable
import java.util.concurrent.CompletionService
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorCompletionService
import java.util.concurrent.Executors
import java.util.concurrent.Future

@Stepwise
@SyncInfraTest
class ZeroDtAsgMatchingSpec extends TerraformSpecification {

  private String[] messages = ['first', 'second']

  def 'asg for web app with classic elb provisioned'() {
    given:

    def region = System.getProperty('region', 'us-east-1')
    AmazonElasticLoadBalancing elb = AmazonElasticLoadBalancingClientBuilder.standard().withRegion(region).build()

    def tfVars = [
        region: region,
        vpc_id: System.properties['vpc_id'],
        app_name: 'test',
        message: messages[0],
        public_domain: System.properties['public_domain'],
        min_size: '2'
    ]

    when: 'autoscaling group web app server with elb is provisioned'

    def output = terraform.init().apply([tfVars: tfVars])

    then:

    output.elb.value.dns ==~ /\b${tfVars.app_name}-\b.{3,}\b.${tfVars.public_domain}\b/

    LoadBalancerDescription awsElbDescription = elb.describeLoadBalancers({
      def describeRequest = new DescribeLoadBalancersRequest()
      describeRequest.withLoadBalancerNames(output.elb.value.name)
    }.call()).loadBalancerDescriptions[0]

    awsElbDescription.instances.size() == (tfVars.min_size as Integer)
    awsElbDescription.listenerDescriptions.size() == 1
    awsElbDescription.listenerDescriptions[0].listener.loadBalancerPort == 443
    awsElbDescription.listenerDescriptions[0].listener.instancePort == 80

    waitForInstancesInService(elb, DescribeInstanceHealthRequest
        .declaredConstructor
        .newInstance()
        .withInstances(awsElbDescription.instances)
        .withLoadBalancerName(output.elb.value.name))

    httpGet("https://${output.elb.value.dns}").body() == tfVars.message
  }

  def 'asg is updated without downtime'() {
    given:

    def tfVars = lastAppliedTfVars

    when: 'autoscaling group is updated'

    tfVars.message = messages[1]
    def output = terraform.output()
    def job = AsyncJob.create {
      terraform.init().apply([tfVars: tfVars])
    }

    then: 'original asg is replaced under the load balancer with zero downtime'

    while (!job.done) {
      httpGet("https://${output.elb.value.dns}").body() ==~ /(\b${messages[0]}\b|\b${messages[1]}\b)/
    }

    httpGet("https://${output.elb.value.dns}").body() == messages[1]

    cleanup:

    terraform.destroy()
  }

  private static boolean waitForInstancesInService(AmazonElasticLoadBalancing elb, DescribeInstanceHealthRequest healthRequest) {
    for (int iteration : (1..10)) {
      def instanceHealth = elb.describeInstanceHealth(healthRequest)
      if (instanceHealth.instanceStates.stream().allMatch { instance -> instance.state == 'InService' }) {
        return true
      }
      sleep(30000)
    }
    false
  }

  private static HttpResponse<String> httpGet(String uri) {
    HttpClient client = HttpClient.newHttpClient()
    HttpRequest request = HttpRequest.newBuilder(URI.create(uri)).GET().build()
    client.send(request, HttpResponse.BodyHandlers.ofString())
  }
}

class AsyncJob<T> {
  private CompletionService<T> completionService
  private Future<T> future

  static <U> AsyncJob create(Callable<U> callable) {
    new AsyncJob(callable)
  }

  private AsyncJob(Callable<T> callable) {
    Executor executor = Executors.newSingleThreadExecutor()
    completionService = new ExecutorCompletionService<>(executor)
    future = completionService.submit(callable)
  }

  boolean isDone() {
    future.done
  }

  T getResult() {
    if (!done) {
      return null
    }
    completionService.take().get()
  }
}
