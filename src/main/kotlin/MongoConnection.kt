import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.MongoException
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import org.bson.Document
import org.bson.codecs.configuration.CodecRegistries.fromProviders
import org.bson.codecs.configuration.CodecRegistries.fromRegistries
import org.bson.codecs.configuration.CodecRegistry
import org.bson.codecs.pojo.PojoCodecProvider
import org.slf4j.LoggerFactory


fun <T> mongoConnection(block: MongoConnection.() -> T) : T = MongoConnection().let(block)

fun <T> MongoDatabase.collection(collectionName: String, block: MongoCollection<Document>.() -> T) : T = getCollection(collectionName).let(block)

class MongoConnection {
    companion object {
        private val logger = LoggerFactory.getLogger(MongoConnection::class.java)
        private var client: MongoClient? = null
        init {
            try {
                logger.info("Attempting to establish a MongoDB client")

                val conf: Config = ConfigFactory.load()

                val connectionString = "${conf.getString("mongo.address.host")}:${conf.getString("mongo.address.port")}"

                val pojoCodecRegistry: CodecRegistry =
                    fromProviders(PojoCodecProvider.builder().automatic(true).build())

                val codecRegistry: CodecRegistry = fromRegistries(
                    MongoClientSettings.getDefaultCodecRegistry(),
                    pojoCodecRegistry
                )

                val clientSettings: MongoClientSettings = MongoClientSettings.builder()
                    .applyConnectionString(ConnectionString(connectionString))
                    .codecRegistry(codecRegistry)
                    .retryWrites(true)
                    .build()

                client = MongoClients.create(clientSettings)

                logger.info("Established a MongoDB client")
            } catch (e: MongoException) {
                logger.error("An error occurred when connecting to MongoDB", e)
            }
        }

        fun close() {
            logger.info("Closing MongoDB connection")
            if (client != null) {
                try {
                    client!!.close() // Assuming there are no concurrent client.close calls
                    logger.debug("Nulling the connection dependency objects")
                    client = null
                } catch (e: Exception) {
                    logger.error("An error occurred when closing the MongoDB connection", e)
                }
            } else {
                logger.warn("mongo object was null, wouldn't close connection")
            }
        }
    }

    fun <T> database(databaseName: String, block: MongoDatabase.() -> T) : T {
        if (client == null) throw IllegalStateException("Using Mongo before creating client")

        return client!!.getDatabase(databaseName).let(block)
    }
}