package com.bluflametech.test

import groovy.ant.AntBuilder
import spock.lang.Specification

/**
 * Executes CodeNarc as a Specification.
 *
 * The decision to make CodeNarc execute as part of a Specification was made due to the lack of a current maintained
 * plugin for Maven.
 *
 * Upon execution, HTML reports are generated at target/site/codenarc/(main | test)_report.html.
 * CodeNarc violations do not fail the build by default. However, if the System property failOnCodeNarcError is set then
 * any CodeNarc violation will fail the build. This can also be adjusted by specifying additional System properties
 * as follows.
 *
 * Example:
 *
 * mvn test -DfailOnCodeNarcError=true -DmaxCodeNarcP1=2 -DmaxCodeNarcP2=5 -DmaxCodeNarcP3=10
 *
 * In the above example, CodeNarc violations will fail the build if there are more than 2 priority one violations,
 * more than 5 priority 2 violations or more than 10 priority 3 violations.
 */
@AlwaysTest
class CodeNarcSpec extends Specification {
  static moveCodeNarcReport(String reportFileName) {
    "mkdir -p ${reportFileName.dropRight(reportFileName.size() - reportFileName.lastIndexOf('/'))}".execute()
    "mv CodeNarcReport.html ${reportFileName}".execute()
  }

  def 'CodeNarc linting'() {
    given:

    String narcedFiles = '**/*.groovy'
    String codeNarcRulesetFiles = ['CodeNarcConfig.groovy'].join(',')

    AntBuilder ant = new AntBuilder()

    expect:

    ant.taskdef(name: 'codenarc', classname: 'org.codenarc.ant.CodeNarcTask')

    try {
      ant.codenarc(ruleSetFiles: codeNarcRulesetFiles,
          maxPriority1Violations: System.getProperty('maxCodeNarcP1', '0') as Integer,
          maxPriority2Violations: System.getProperty('maxCodeNarcP2', '0') as Integer,
          maxPriority3Violations: System.getProperty('maxCodeNarcP3', '0') as Integer) {
        fileset(dir: 'src/main/groovy') {
          include(name: narcedFiles)
        }
        report(type: 'html')
      }
    } catch (Exception exception) {
      !(System.getProperty('failOnCodeNarcError') && (new File('src/main/groovy')).exists())
    }
    finally {
      moveCodeNarcReport('target/site/codenarc/main_report.html')
    }

    try {
      ant.codenarc(ruleSetFiles: codeNarcRulesetFiles,
          maxPriority1Violations: System.getProperty('maxCodeNarcP1', '0') as Integer,
          maxPriority2Violations: System.getProperty('maxCodeNarcP2', '0') as Integer,
          maxPriority3Violations: System.getProperty('maxCodeNarcP3', '0') as Integer) {
        fileset(dir: 'src/test/groovy') {
          include(name: narcedFiles)
        }
        report(type: 'html')
      }
    } catch (Exception exception) {
      !(System.getProperty('failOnCodeNarcError') && (new File('src/main/groovy')).exists())    }
    finally {
      moveCodeNarcReport('target/site/codenarc/test_report.html')
    }
  }
}
