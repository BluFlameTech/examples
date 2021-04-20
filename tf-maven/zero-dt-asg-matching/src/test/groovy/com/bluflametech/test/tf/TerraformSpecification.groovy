package com.bluflametech.test.tf

import com.deliveredtechnologies.terraform.api.TerraformApply
import com.deliveredtechnologies.terraform.api.TerraformDestroy
import com.deliveredtechnologies.terraform.api.TerraformInit
import com.deliveredtechnologies.terraform.api.TerraformOutput
import groovy.json.JsonSlurper
import spock.lang.Specification

/**
 * Spock Specification for testing Terraform modules.
 */
abstract class TerraformSpecification extends Specification {

  /**
   * Provides protected access to Terraform operations as follows.
   *
   * terraform.init()
   * terraform.apply([tfVars: tfvars])
   * terraform.output()
   * terraform.destroy()
   *
   * Notes on return values:
   *
   * terraform.apply([tfVars: tfvars]) returns a Json object containing the terraform output
   * terraform.init() returns the terraform object - so, terraform.init().apply([tfVars: tfvars]) is possible
   */
  private static Map<String, String> lastAppliedTfVars

  protected Map<String, Closure<?>> terraform

  {
    String tfRootDirUnderTest = tfRootDir
    def apply = new TerraformApply(tfRootDirUnderTest)
    def destroy = new TerraformDestroy(tfRootDirUnderTest)
    def output = new TerraformOutput(tfRootDirUnderTest)
    def init = new TerraformInit(tfRootDirUnderTest)

    terraform = [
        init: {
          init.execute([:])
          terraform
        },
        apply: { props ->
          setLastAppliedTfVars(props.clone()?.tfVars)
          apply.execute(props)
          terraform.output()
        },
        output: {
          JsonSlurper slurper = new JsonSlurper()
          slurper.parseText(output.execute([:]))
        },
        destroy: { destroy.execute([tfVars: getLastAppliedTfVars()]) }
    ]
  }

  protected synchronized static setLastAppliedTfVars(Map<String, String> tfVars) {
    lastAppliedTfVars = tfVars
  }

  protected synchronized static Map<String, String> getLastAppliedTfVars() {
    lastAppliedTfVars.clone() as Map<String, String>
  }

  /**
   * Determines the corresponding Terraform root module based on the name of the Specification.
   *
   * Specification class names are assumed to be named by convention as shown by example below.
   *
   * Specification name MyTfRootModule maps to the Terraform root module directory my_tf_root_module.
   *
   * The location of the Terraform root module can be either src/main/tf or src/main/tf-examples, with tf-examples
   * taking precedence.
   *
   * @return the String path of the matching Terraform root module under test
   */
  private String getTfRootDir() {
    String className = this.getClass().simpleName
    String stackName = className[0].toLowerCase() +
        className.dropRight(4)[1..-1].replaceAll('[A-Z]') { matcher ->
          "_${matcher[0].toLowerCase()}"
        }
    File example = new File("src/main/tf-examples/${stackName}")
    if (example.exists() && example.directory) {
      return "tf-examples/${stackName}"
    }

    stackName
  }
}
