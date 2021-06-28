import com.bluflametech.test.IntegrationTest
import com.bluflametech.test.UnitTest

def testAnnotations = [
    unit: UnitTest,
    integration: IntegrationTest
]
def testTypes = System.getProperty('testTypes')?.split(',\\s*')?.collect {testAnnotations[it.toLowerCase()]}

runner {
  testTypes?.each { testType ->
    include.annotations << testType
  }
  testTypes ?: include.annotations << UnitTest
}
