// Licensed to Elasticsearch B.V. under one or more contributor
// license agreements. See the NOTICE file distributed with
// this work for additional information regarding copyright
// ownership. Elasticsearch B.V. licenses this file to you under
// the Apache License, Version 2.0 (the "License"); you may
// not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

import org.junit.Before
import org.junit.Test
import static org.junit.Assert.assertTrue

class WithHubCredentialsStepTests extends ApmBasePipelineTest {
  String scriptName = 'vars/withHubCredentials.groovy'

  @Override
  @Before
  void setUp() throws Exception {
    super.setUp()
    env.GITHUB_USER = 'user'
    env.GITHUB_TOKEN = 'token'
    env.HOME = '/home'
  }

  @Test
  void test() throws Exception {
    def script = loadScript(scriptName)
    def isOK = false
    script.call {
      isOK = true
    }
    printCallStack()
    assertTrue(isOK)
    assertTrue(assertMethodCallContainsPattern('withCredentials', 'credentialsId'))
    assertTrue(assertMethodCallContainsPattern('writeFile', 'hub'))
    assertTrue(assertMethodCallOccurrences('deleteDir', 1))
    assertJobStatusSuccess()
  }

  @Test
  void test_windows() throws Exception {
    def script = loadScript(scriptName)
    helper.registerAllowedMethod('isUnix', [], { false })
    def isOK = false
    try {
      script.call() {
        //NOOP
      }
    } catch(e){
      //NOOP
    }
    printCallStack()
    assertTrue(assertMethodCallContainsPattern('error', 'withHubCredentials: windows is not supported yet.'))
    assertJobStatusFailure()
  }

  @Test
  void test_with_body_error() throws Exception {
    def script = loadScript(scriptName)
    try {
      script.call {
        throw new Exception('Mock an error')
      }
    } catch(e){
      //NOOP
      println e
    }
    printCallStack()
    assertTrue(assertMethodCallContainsPattern('withCredentials', 'credentialsId'))
    assertTrue(assertMethodCallContainsPattern('writeFile', 'hub'))
    assertTrue(assertMethodCallContainsPattern('error', 'withHubCredentials: error'))
    assertTrue(assertMethodCallOccurrences('deleteDir', 1))
    assertJobStatusFailure()
  }
}
