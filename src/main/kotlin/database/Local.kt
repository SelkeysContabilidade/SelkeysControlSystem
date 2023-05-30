package database

import Preferences.props
import com.azure.cosmos.util.CosmosPagedIterable
import database.RemoteConnector.queryRemoteClients
import database.RemoteConnector.queryRemoteDocuments
import database.RemoteConnector.queryRemoteTranslations
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction


object LocalDatabase {
    private val db = Database.connect(props.getProperty("localDatabase"), driver = "org.h2.Driver", "Selkeys", "")
    private val coroutineContext = newSingleThreadContext("localDatabase")


    fun resyncDatabase() {
        //fetch remote clients before doing anything
        val sourceClients: CosmosPagedIterable<RemoteConnector.Client>?
        val sourceDocuments: CosmosPagedIterable<RemoteConnector.Document>?
        val sourceTranslations: CosmosPagedIterable<RemoteConnector.Translation>?
        try {
            sourceClients = queryRemoteClients()
            sourceDocuments = queryRemoteDocuments()
            sourceTranslations = queryRemoteTranslations()
            if ((sourceClients == null) || (sourceDocuments == null) || (sourceTranslations == null)) {
                return
            }
        } catch (e: Exception) {
            return
        }

        runBlocking(coroutineContext) {
            //clear all local databases
            transaction {
                Procedures.dropStatement().forEach(::exec)
                Registries.dropStatement().forEach(::exec)
                Documents.dropStatement().forEach(::exec)
                Clients.dropStatement().forEach(::exec)
                Translations.dropStatement().forEach(::exec)
                SchemaUtils.create(Translations)
                SchemaUtils.create(Documents)
                SchemaUtils.create(Clients)
                SchemaUtils.create(Procedures)
                SchemaUtils.create(Registries)
            }
            //adding source data to the local
            launch(Dispatchers.Default) {
                transaction {
                    sourceClients.forEach {
                        val client =
                            Client.new {
                                baseFolderStructure = it.baseFolderStructure
                                nickname = it.nickname
                            }
                        it.registry.forEach {
                            Registry.new {
                                registry = it
                                this.client = client
                            }
                        }
                    }
                }
            }

            launch(Dispatchers.Default) {
                transaction {
                    sourceDocuments.forEach {
                        val document =
                            Document.new {
                                identifier = it.identifier
                                baseFolderStructure = it.baseFolderStructure
                                registryRegex = it.registryRegex
                            }
                        it.procedures.forEach {
                            Procedure.new {
                                type = it.type
                                isFolder = it.folder
                                content = it.content
                                order = it.order
                                this.document = document
                            }
                        }
                    }
                }
            }
            launch(Dispatchers.Default) {
                transaction {
                    sourceTranslations.forEach {
                        Translation.new {
                            key = it.key
                            value = it.value
                        }
                    }
                }
            }
        }
    }

    fun findByRegistry(registry: String): Client? = runBlocking(coroutineContext) {
        transaction(db) {
            Registry.find { Registries.registry eq registry }.firstOrNull()?.client
        }
    }

    fun findTranslation(key: String): String = runBlocking(coroutineContext) {
        transaction(db) {
            Translation.find { Translations.key eq key }.firstOrNull()?.value ?: key
        }
    }

    fun findAllDocuments(): List<Document> = runBlocking(coroutineContext) {
        transaction(db) {
            Documents.selectAll().map { transaction { Document.wrapRow(it) } }.toList()
        }
    }

    class Translation(id: EntityID<Int>) : IntEntity(id) {
        companion object : IntEntityClass<Translation>(Translations)

        var key by Translations.key
        var value by Translations.translation
    }

    private object Translations : IntIdTable() {
        val key = text("key")
        val translation = text("translation")
    }

    class Procedure(id: EntityID<Int>) : IntEntity(id) {
        companion object : IntEntityClass<Procedure>(Procedures)

        var isFolder by Procedures.isFolder
        var type by Procedures.type
        var content by Procedures.content
        var order by Procedures.order
        var document by Document referencedOn Procedures.document
    }

    class Registry(id: EntityID<Int>) : IntEntity(id) {
        companion object : IntEntityClass<Registry>(Registries)

        var registry by Registries.registry
        var client by Client referencedOn Registries.client
    }

    private object Procedures : IntIdTable() {
        val isFolder = bool("folder")
        val type = text("type")
        val content = text("content")
        val order = integer("order")
        val document = reference("document", Documents)
    }

    private object Registries : IntIdTable() {
        val registry = text("registry")
        val client = reference("client", Clients)
    }

    class Document(id: EntityID<Int>) : IntEntity(id) {
        companion object : IntEntityClass<Document>(Documents)

        var identifier by Documents.identifier
        var baseFolderStructure by Documents.baseFolderStructure
        var registryRegex by Documents.registryRegex
        private val procedures by Procedure referrersOn Procedures.document
        fun getProceduresOrdered() = transaction { procedures.sortedBy { it.order } }
    }

    private object Documents : IntIdTable() {
        val identifier = text("identifier")
        val baseFolderStructure = text("baseFolderStructure", eagerLoading = true)
        val registryRegex = text("registryIndex")
    }

    class Client(id: EntityID<Int>) : IntEntity(id) {
        companion object : IntEntityClass<Client>(Clients)

        var baseFolderStructure by Clients.baseFolderStructure
        var nickname by Clients.nickname
    }

    private object Clients : IntIdTable() {
        val baseFolderStructure = text("baseFolderStructure", eagerLoading = true)
        val nickname = text("nickname")
    }
}
