package plugins

import dao.UserCreation
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.html.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import kotlinx.html.*
import org.bson.types.ObjectId
import repo.UserRepo
import plugins.authentication.BasicAuthentication

fun Application.configureRouting(userRepo: UserRepo) {

    routing {
        get("login") {
            val redirectLink = call.request.queryParameters["redirect"]

            call.respondHtml {
                head {
                    title {
                        +"Login"
                    }
                }
                body {
                    h1 {
                        +"Login"
                    }
                    form(action = "/login${if (redirectLink == null) "" else "?redirect=$redirectLink"}", encType = FormEncType.applicationXWwwFormUrlEncoded, method = FormMethod.post) {
                        p {
                            +"Username:"
                            textInput(name = "username")
                        }
                        p {
                            +"Password:"
                            passwordInput(name = "password")
                        }
                        p {
                            submitInput { value = "Login" }
                        }
                    }
                }
            }
        }
        post("login") {
            val redirectLink = call.request.queryParameters["redirect"]

            val parameters = call.receiveParameters()

            val user = userRepo.findByUsername(parameters.getOrFail("username"))

            if (user == null) {
                call.respondText("User not found.", status=HttpStatusCode.NotFound)
            } else {
                if (user.password == parameters.getOrFail("password")) {
                    if (redirectLink == null) {
                        call.respond(HttpStatusCode.OK)
                    } else {
                        call.respondRedirect("continue?from=login&redirect=$redirectLink")
                    }
                }
            }
        }
        get("register") {
            val redirectLink = call.request.queryParameters["redirect"]

            call.respondHtml {
                head {
                    title {
                        +"Register"
                    }
                }
                body {
                    h1 {
                        +"Register"
                    }
                    form(action = "/register${if (redirectLink == null) "" else "?redirect=$redirectLink"}", encType = FormEncType.applicationXWwwFormUrlEncoded, method = FormMethod.post) {
                        p {
                            +"Username:"
                            textInput(name = "username")
                        }
                        p {
                            +"Email:"
                            emailInput(name = "email")
                        }
                        p {
                            +"Password:"
                            passwordInput(name = "password")
                        }
                        p {
                            submitInput { value = "Register" }
                        }
                    }
                }
            }
        }
        post("register") {
            val redirectLink = call.request.queryParameters["redirect"]

            val parameters = call.receiveParameters()

            val user = userRepo.add(
                UserCreation(
                    parameters.getOrFail("username"),
                    parameters.getOrFail("email"),
                    parameters.getOrFail("password"),
                )
            )

            if (redirectLink == null) {
                call.respond(HttpStatusCode.Created)
            } else {
                call.respondRedirect("continue?from=register&redirect=$redirectLink")
            }
        }
        get("continue") {
            val redirectLink = call.request.queryParameters.getOrFail("redirect")
            val from = call.request.queryParameters.getOrFail("from")

            if (from == "login") {
                call.respondHtml {
                    head {
                        title {
                            +"Login Successful"
                        }
                    }
                    body {
                        h1 {
                            +"You are now logged in!"
                        }
                        a {
                            href = redirectLink
                            +"Continue"
                        }
                    }
                }
            } else {
                call.respondHtml {
                    head {
                        title {
                            +"Successfully Registered"
                        }
                    }
                    body {
                        h1 {
                            +"You are now registered!"
                        }
                        a {
                            href = redirectLink
                            +"Continue"
                        }
                    }
                }
            }
        }

        route("v1") {
            install(BasicAuthentication.plugin) {
                realm = "Access to v1"
                authorizer = { username, password ->
                    val user = userRepo.findByUsername(username)
                    user != null && user.password == password
                }
            }
            route("users") {
                get {
                    val users = userRepo.all()

                    call.respond(HttpStatusCode.OK, users)
                }

                post {
                    val userDTO = call.receive<UserCreation>()

                    val user = userRepo.add(userDTO)

                    call.respond(HttpStatusCode.Created, user)
                }

                get("/{id}") {
                    val id = call.parameters["id"]

                    if (ObjectId.isValid(id)) {
                        val user = userRepo.get(ObjectId(id))
                        if (user == null)
                            call.respond(HttpStatusCode.NotFound)
                        else
                            call.respond(HttpStatusCode.OK, user)
                    }
                    else
                        call.respond(HttpStatusCode.BadRequest)
                }

                delete {
                    val success = userRepo.removeAll()

                    call.respond(HttpStatusCode.OK, success)
                }

                delete("/{id}") {
                    val id = call.parameters["id"]

                    if (ObjectId.isValid(id))
                        call.respond(HttpStatusCode.OK, userRepo.remove(ObjectId(id)))
                    else
                        call.respond(HttpStatusCode.BadRequest)
                }
            }
        }
    }
}
