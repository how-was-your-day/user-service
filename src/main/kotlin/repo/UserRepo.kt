package repo

import dao.*
import model.User
import org.bson.Document
import org.bson.types.ObjectId

interface UserRepo : Repo<User, UserCreation, ObjectId> {
    fun findByUsername(username: String) : User?
}

class UserRepoImpl(private val userDAO: UserDAO) : UserRepo {
    override fun findByUsername(username: String): User? {
        return userDAO.find(Document(
            mapOf(
                "name" to username
            )
        ))
    }

    override fun get(id: ObjectId): User? = userDAO.read(id)

    override fun all(): List<User> = userDAO.readAll()

    override fun removeAll(): Boolean {
        val users = userDAO.readAll()

        val status = mutableListOf<Boolean>()
        for (user in users) {
            status.add(userDAO.delete(user.id))
        }
        return status.all{ it }
    }

    override fun add(t: UserCreation): User = userDAO.create(t)

    override fun remove(id: ObjectId): Boolean = userDAO.delete(id)
}