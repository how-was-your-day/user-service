package dao

import com.mongodb.client.FindIterable
import org.bson.Document
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties

interface CRUD<T, CreateT, ID> {
    fun create(obj: CreateT) : T
    fun read(id: ID) : T?
    fun update(obj: T) : Boolean
    fun delete(id: ID) : Boolean
    fun readAll(): List<T>
}

interface Filtered<Filter, T> {
    fun find(filter: Filter) : T?
    fun filter(filter: Filter) : List<T>
}

fun documentFrom(obj: Any) : Document {
    val doc = Document()

    for (prop in obj::class.memberProperties) {
        @Suppress("UNCHECKED_CAST")
        doc[prop.name] = (prop as KProperty1<Any, *>).get(obj)
    }

    return doc
}

interface DocumentMapper<T> {
    fun map(doc: Document) : T
    fun unmap(obj: T): Document
}

fun <T> FindIterable<Document>.toList(mapper: DocumentMapper<out T>) : List<T> {
    val acc = mutableListOf<T>()
    for (doc in this) {
        acc.add(
            mapper.map(doc)
        )
    }
    return acc
}