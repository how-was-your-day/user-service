package plugins.authentication

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.util.*
import java.nio.charset.Charset
import java.util.Base64

private const val AUTHORIZATION_HEADER = "Authorization"

class AuthenticationPluginConfig {
    var realm = "/"
}

val userID = AttributeKey<String>("user-id")
val password = AttributeKey<String>("password")

val BasicAuthentication = createRouteScopedPlugin("Basic Auth", ::AuthenticationPluginConfig) {
    onCall {call ->
        val authorizationHeader = call.request.headers[AUTHORIZATION_HEADER]

        var responseBody : String? = null
        if (authorizationHeader != null) {
            val segments = authorizationHeader.split(" ")
            if (segments[0].toLowerCasePreservingASCIIRules() == "basic") {
                val userAndPassword = String(Base64.getDecoder().decode(segments[1]))
                val separatorIndex = userAndPassword.indexOf(':')

                if (separatorIndex != -1) {
                    val user = userAndPassword.substring(0, separatorIndex)
                    val _password = userAndPassword.substring(separatorIndex + 1)

                    call.attributes.put(userID, user)
                    call.attributes.put(password, _password)

                    println("User: $user -- Password: $_password")

                    return@onCall
                }


            } else {
                responseBody = "Invalid Scheme. Should use Basic."
            }
        }

        call.response.headers.append("WWW-Authenticate", "Basic realm=\"${pluginConfig.realm}\"")
        if (responseBody == null)
            call.respond(HttpStatusCode.Unauthorized)
        else
            call.respondText(responseBody, status = HttpStatusCode.Unauthorized)
    }
}