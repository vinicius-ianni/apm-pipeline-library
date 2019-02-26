import com.lesfurets.jenkins.unit.BasePipelineTest
import org.junit.Before
import org.junit.Test
import static com.lesfurets.jenkins.unit.MethodCall.callArgsToString
import static org.junit.Assert.assertTrue

class GetVaultSecretStepTests extends BasePipelineTest {
  Map env = [:]

  def wrapInterceptor = { map, closure ->
    map.each { key, value ->
      if("varPasswordPairs".equals(key)){
        value.each{ it ->
          binding.setVariable("${it.var}", "${it.password}")
        }
      }
    }
    def res = closure.call()
    map.forEach { key, value ->
      if("varPasswordPairs".equals(key)){
        value.each{ it ->
          binding.setVariable("${it.var}", null)
        }
      }
    }
    return res
  }

  def withCredentialsInterceptor = { list, closure ->
    list.forEach {
      env[it.variable] = "dummyValue"
    }
    def res = closure.call()
    list.forEach {
      env.remove(it.variable)
    }
    return res
  }

  @Override
  @Before
  void setUp() throws Exception {
    super.setUp()

    binding.setVariable('env', env)
    helper.registerAllowedMethod('httpRequest', [Map.class], { m ->
      if(m?.url?.contains("v1/secret/apm-team/ci/secret")){
        return "{plaintext: '12345', encrypted: 'SECRET'}"
      }
      if(m?.url?.contains("v1/auth/approle/login")){
        return "{auth: {client_token: 'TOKEN'}}"
      }
    })
    helper.registerAllowedMethod("string", [Map.class], { m -> return m })
    helper.registerAllowedMethod("wrap", [Map.class, Closure.class], wrapInterceptor)
    helper.registerAllowedMethod("withCredentials", [List.class, Closure.class], withCredentialsInterceptor)
    helper.registerAllowedMethod("log", [Map.class], {m -> println "[${m.level}] ${m.text}"})
    helper.registerAllowedMethod("error", [String.class], {s ->
      printCallStack()
      throw new Exception(s)
      })
    helper.registerAllowedMethod("toJSON", [String.class], { s ->
      def script = loadScript("vars/toJSON.groovy")
      return script.call(s)
      })
  }

  @Test
  void test() throws Exception {
    def script = loadScript("vars/getVaultSecret.groovy")
    def jsonValue = script.call("secret")
    assertTrue(jsonValue.plaintext == '12345')
    printCallStack()
    assertJobStatusSuccess()
  }

  @Test
  void testNoSecret() throws Exception {
    def script = loadScript("vars/getVaultSecret.groovy")
    try {
      def jsonValue = script.call()
    } catch(e) {
      //NOOP
    }
    assertTrue(helper.callStack.findAll { call ->
        call.methodName == "error"
    }.any { call ->
        callArgsToString(call).contains("getVaultSecret: No valid secret to looking for.")
    })
  }

  @Test
  void testGetTokenError() throws Exception {
    helper.registerAllowedMethod('httpRequest', [Map.class], { m ->
      if(m?.url?.contains("v1/auth/approle/login")){
        return "{auth: ''}"
      }
    })

    def script = loadScript("vars/getVaultSecret.groovy")
    try {
      def jsonValue = script.call("secret")
    } catch(e) {
      //NOOP
    }
    printCallStack()
    assertTrue(helper.callStack.findAll { call ->
        call.methodName == "error"
    }.any { call ->
        callArgsToString(call).contains("getVaultSecret: Unable to get the token.")
    })
  }

  @Test
  void testGetSecretError() throws Exception {
    helper.registerAllowedMethod('httpRequest', [Map.class], { m ->
      if(m?.url?.contains("v1/secret/apm-team/ci/secret")){
        return ""
      }
      if(m?.url?.contains("v1/auth/approle/login")){
        return "{auth: {client_token: 'TOKEN'}}"
      }
    })

    def script = loadScript("vars/getVaultSecret.groovy")
    try {
      def jsonValue = script.call("secret")
    } catch(e) {
      //NOOP
    }
    printCallStack()
    assertTrue(helper.callStack.findAll { call ->
        call.methodName == "error"
    }.any { call ->
        callArgsToString(call).contains("getVaultSecret: Unable to get the secret.")
    })
  }

}