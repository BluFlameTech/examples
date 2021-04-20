import com.bluflametech.test.UnitTest
import com.bluflametech.test.AlwaysTest
import org.reflections.Reflections

import java.lang.annotation.Annotation
import java.util.stream.Collectors

/**
 * Default configuration for Spock.
 *
 * Specifications annotated with @AlwaysTest are always executed. The CodeNarcSpec for CodeNarc linting is included
 * in that test set. Additionally, any test types included included as comma separated values in the 'testTypes'
 * System property are also included in the test set.
 *
 * For example:
 *
 * mvn test -DtestTypes="unit,integration"
 *
 * Will execute all Specifications annotated with @AlwaysTest, @UnitTest or @IntegrationTest.
 *
 * If no testTypes System property is found then Specifications annotated with @AlwaysTest and Specifications
 * annotated with @UnitTest will be executed by default.
 */

def testAnnotations = (new Reflections('com.bluflametech.test'))
      .getSubTypesOf(Annotation)
      .stream()
      .filter({ clazz -> !clazz.simpleName.toLowerCase().startsWith('always')})
      .collect(
          Collectors.toMap(
              {Class<?> clazz -> clazz.simpleName.toLowerCase().dropRight(4)},
              {Class<?> clazz -> clazz}))

def testTypes = System.getProperty('testTypes')?.split(',\\s*')?.collect {testAnnotations[it.toLowerCase()]}

runner {

  testTypes?.each { testType ->
      include.annotations << testType
  }
  include.annotations << AlwaysTest
  testTypes ?: include.annotations << UnitTest
}

