# Zero-Downtime Autoscaling Web App Capacity Matching Example

_This example is associated with the [Testing Terraform For More Than Just Compliance](https://www.bluflametech.com/blog/working-tests-spock/) post._ 

This Terraform configuration is a working example that builds upon the [Adding Capacity Matching to Terraform ASG Updates](https://www.bluflametech.com/blog/asg-capacity-matching/) post.
This example adds capacity matching for zero-downtime asg updates. Say, for example, the provisioned autoscaling group had its minimum capacity and
its desired capacity set to 2. But additional load caused it to scale up to 4 instances. With the previous example, executing an update
would scale it back down to 2, which might make the autoscaling group unable to satisfy the load. However, this example
fixes that issue by matching the desired capacity to the number of instances it is replacing.

## Prerequisites

* Python v3+ installed on the system applying the Terraform configuration
* AWS Boto3 installed on the system (i.e. ```pip3 install boto3```)
* A VPC with at least one private subnet (tagged with tier = 'private') and one public subnet (tagged with tier = 'public')
* A NAT instance or a NAT gateway configured
* A public hosted zone configured in Route53 with a "*.{public domain}" certificate available in ACM

## Deployments

Deployments are triggered either through a change in the _message_ terraform input variable or an AMI change. 

Deployment steps are as follows:

1. the [userdata script](src/main/tf/zer_dt_asg_matching/.scripts/userdata.sh) creates an Express server on port 8080 that returns the message specified as an HTTP GET response
5. the reverse proxy (configured using Apache web server) points port 80 to port 8080 (80 -> 8080)
6. once the new autoscaling group's instances are available, the old autoscaling group and its launch configuration are destroyed

## Directory Structure

* ```src/main/tf/zero_dt_asg_matching``` - the Terraform configuration under test
* ```src/test/groovy``` - the Spock test Specifications and supporting Groovy code
* ```src/test/resources``` - the CodeNarc and Spock Groovy configuration files

## Terraform Configuration Details

## Requirements

| Name | Version |
|------|---------|
| aws | ~> 3.28 |
| random | >= 3.0.1 |

## Providers

| Name | Version |
|------|---------|
| external | n/a |
| random | >= 3.0.1 |

## Modules

| Name | Source | Version |
|------|--------|---------|
| asg | ./web_asg |  |
| elb | ./web_elb |  |

## Inputs

| Name | Description | Type | Default | Required |
|------|-------------|------|---------|:--------:|
| app\_name | the name of the app | `string` | n/a | yes |
| message | the message to return on an HTTP GET request | `string` | n/a | yes |
| public\_domain | the public domain name (i.e. 'example.com') | `string` | n/a | yes |
| vpc\_id | the id of the vpc in which to target | `string` | n/a | yes |
| ami | the ami used to provision the ec2 instance | `string` | `"ami-0be2609ba883822ec"` | no |
| desired\_capacity | the desired number of instances in the asg (defaults to min\_size if not specified) | `string` | `""` | no |
| instance\_type | the type of instance (e.g. t3.medium) | `string` | `"t3.medium"` | no |
| keypair\_name | the keypair that will be used for ssh access into the ec2 instances, if supplied | `string` | `""` | no |
| max\_size | the maximum number of instances in the asg | `string` | `"4"` | no |
| min\_size | the minimum number of instances in the asg | `string` | `"2"` | no |
| region | the region in which to provision | `string` | `"us-east-1"` | no |
| volume\_size | the root volume size of the asg instances | `string` | `"64"` | no |

## Outputs

| Name | Description |
|------|-------------|
| asg | the autoscaling group details |
| elb | the classic elb details (used to route web requests to the instances in the autoscaling group) |
