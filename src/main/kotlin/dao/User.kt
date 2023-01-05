package dao

import collection
import com.mongodb.client.MongoCollection
import mongoConnection
import kotlinx.serialization.Serializable
import model.User
import org.bson.Document
import org.bson.types.ObjectId

interface UserDAO : CRUD<User, UserCreation, ObjectId>, Filtered<Document, User>

@Serializable
data class UserCreation(val name: String,
                        val email: String,
                        val password: String)

class UserDAOImpl(private val userMapper: DocumentMapper<User>) : UserDAO {
    private fun <T> useUsersCollection(block: MongoCollection<Document>.() -> T) =
        mongoConnection {
            database("user-service") {
                collection("users") {
                    block()
                }
            }
        }

    override fun create(obj: UserCreation): User = useUsersCollection {
        val doc = documentFrom(obj)

        val unixNow = System.currentTimeMillis()
        doc["dateCreated"] = unixNow

        val insertResult = insertOne(doc)

        val id = insertResult.insertedId?.asObjectId()?.value ?: throw Exception("Not able to create for some reason.")

        return@useUsersCollection User(
            id,
            obj.name,
            obj.email,
            obj.password,
            unixNow
        )
    }

    override fun read(id: ObjectId): User? = useUsersCollection {
        val doc = Document()

        doc["_id"] = id

        val findIterable = find(doc)

        when(val userDoc = findIterable.first()) {
            null -> null
            else -> userMapper.map(userDoc)
        }
    }

    override fun update(obj: User): Boolean = useUsersCollection {
        val updateResult = updateOne(
            Document(mapOf("_id" to obj.id)),
            userMapper.unmap(obj)
        )

        updateResult.matchedCount == 1L && updateResult.modifiedCount == 1L
    }

    override fun delete(id: ObjectId): Boolean = useUsersCollection {
        val deleteResult = deleteOne(Document(
            mapOf(
                "_id" to id
            )
        ))

        return@useUsersCollection deleteResult.deletedCount > 0
    }

    override fun readAll(): List<User> = useUsersCollection {
        find().toList(userMapper)
    }

    override fun find(filter: Document): User? = useUsersCollection {
        val first = find(filter).first() ?: return@useUsersCollection null

        userMapper.map(first)
    }

    override fun filter(filter: Document): List<User> = useUsersCollection {
        find(filter).toList(userMapper)
    }
}

class UserMapper : DocumentMapper<User> {
    override fun map(doc: Document): User =
        User(
            doc.getObjectId("_id"),
            doc.getString("name"),
            doc.getString("email"),
            doc.getString("password"),
            doc.getLong("dateCreated")
        )

    override fun unmap(obj: User): Document =
        Document(
            mapOf(
                "_id" to obj.id,
                "name" to obj.name,
                "email" to obj.email,
                "password" to obj.password,
                "dateCreated" to obj.dateCreated
            )
        )
}