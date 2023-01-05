import dao.UserDAOImpl
import dao.UserMapper
import io.ktor.server.application.*
import plugins.configureRouting
import plugins.configureSerialization
import repo.UserRepoImpl

fun main(args: Array<String>): Unit =
    io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused") // application.conf references the main function. This annotation prevents the IDE from marking it as unused.
fun Application.module() {
    configureSerialization()
    configureRouting(UserRepoImpl(UserDAOImpl(UserMapper())))
}
