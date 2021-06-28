package com.bluflametech.lint

import groovy.ant.AntBuilder
import spock.lang.Specification

class CodeNarcSpec extends Specification {
  static moveCodeNarcReport(String dir) {
    "mkdir -p ${dir.dropRight(dir.size() - dir.lastIndexOf('/'))}".execute()
    "mv CodeNarcReport.html ${dir}".execute()
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
          maxPriority1Violations: 0, maxPriority2Violations: 0, maxPriority3Violations: 0) {
        fileset(dir: 'src/test/groovy') {
          include(name: narcedFiles)
        }

        report(type: 'html')
      }
    } catch (Exception exception) {
      !System.getProperty('failOnCodeNarcError')
    }

    cleanup:

    moveCodeNarcReport('target/site/codenarc/report.html')
  }
}
