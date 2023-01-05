package model

import kotlinx.serialization.Serializable
import org.bson.types.ObjectId

@Serializable
data class User(
    @Serializable(ObjectIdSerializer::class) val id: ObjectId,
    val name: String,
    val email: String,
    val password: String,
    val dateCreated: Long)

