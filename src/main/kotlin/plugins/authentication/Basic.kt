package plugins.authentication

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.util.*
import java.util.Base64

typealias BasicAuthorizer = (username: String, password: String) -> Boolean

object BasicAuthentication {
    private const val AUTHORIZATION_HEADER = "Authorization"

    class AuthenticationPluginConfig {
        var realm = "/"
        var authorizer : BasicAuthorizer = { _,_ -> true }
    }

    val userID = AttributeKey<String>("user-id")
    val password = AttributeKey<String>("password")

    val plugin = createRouteScopedPlugin("Basic Auth", ::AuthenticationPluginConfig) {
        onCall {call ->
            val authorizationHeader = call.request.headers[AUTHORIZATION_HEADER]

            if (authorizationHeader != null) {

                val segments = authorizationHeader.split(" ")

                if (segments[0].toLowerCasePreservingASCIIRules() == "basic") {

                    val userAndPassword = String(Base64.getDecoder().decode(segments[1]))
                    val separatorIndex = userAndPassword.indexOf(':')

                    if (separatorIndex != -1) {
                        val user = userAndPassword.substring(0, separatorIndex)
                        val _password = userAndPassword.substring(separatorIndex + 1)

                        val authorized = pluginConfig.authorizer(user, _password)

                        if (authorized) {
                            call.attributes.put(userID, user)
                            call.attributes.put(password, _password)
                            return@onCall
                        }
                    }
                }
            }

            call.response.headers.append("WWW-Authenticate", "Basic realm=\"${pluginConfig.realm}\"")
            call.respond(HttpStatusCode.Unauthorized)
        }
    }
}