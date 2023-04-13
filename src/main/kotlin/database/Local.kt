package database

import Preferences.props
import database.RemoteConnector.queryRemoteClients
import database.RemoteConnector.queryRemoteDocuments
import database.RemoteConnector.queryRemoteTranslations
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction


object LocalDatabase {
    private val db = Database.connect(props.getProperty("localDatabase"), driver = "org.h2.Driver", "Selkeys", "")

    fun updateDatabase() {
        //fetch remote clients before doing anything
        val sourceClients = queryRemoteClients()
        val sourceDocuments = queryRemoteDocuments()
        val sourceTranslations = queryRemoteTranslations()
        if (sourceClients == null || sourceDocuments == null || sourceTranslations == null) {
            return
        }
        //clear all local databases
        transaction(db) {
            Procedures.dropStatement().forEach(::exec)
            Documents.dropStatement().forEach(::exec)
            Clients.dropStatement().forEach(::exec)
            Translations.dropStatement().forEach(::exec)
            SchemaUtils.create(Translations)
            SchemaUtils.create(Documents)
            SchemaUtils.create(Clients)
            SchemaUtils.create(Procedures)
        }
        //adding source data to the local
        sourceClients.forEach {
            transaction {
                Client.new {
                    registry = it.registry
                    baseFolderStructure = it.baseFolderStructure
                    nickname = it.nickname
                }
            }
        }
        sourceDocuments.forEach {
            val document = transaction {
                Document.new {
                    identifier = it.identifier
                    baseFolderStructure = it.baseFolderStructure
                    registryRegex = it.registryRegex
                }
            }
            it.procedures.map {
                transaction {
                    Procedure.new {
                        type = it.type
                        content = it.content
                        order = it.order
                        this.document = document
                    }
                }
            }
        }
        sourceTranslations.forEach {
            transaction {
                Translation.new {
                    key = it.key
                    translation = it.translation
                }
            }
        }
    }

    fun findByRegistry(registry: String): Client? = transaction(db) {
        Client.find { Clients.registry eq registry.replace("[^0-9]".toRegex(), "") }.firstOrNull()
    }

    fun findTranslation(key: String): String = transaction(db) {
        Translation.find { Translations.key eq key }.firstOrNull()?.translation.orEmpty()
    }

    fun findAllClients(): List<Client> = transaction(db) {
        Clients.selectAll().map { transaction { Client.wrapRow(it) } }.toList()
    }


    fun findAllDocuments(): List<Document> = transaction(db) {
        Documents.selectAll().map { transaction { Document.wrapRow(it) } }.toList()
    }


    private object Translations : IntIdTable() {
        val key = text("key")
        val translation = text("translation")
    }

    class Translation(id: EntityID<Int>) : IntEntity(id) {
        companion object : IntEntityClass<Translation>(Translations)

        var key by Translations.key
        var translation by Translations.translation
    }

    private object Documents : IntIdTable() {
        val identifier = text("identifier")
        val baseFolderStructure = text("baseFolderStructure", eagerLoading = true)
        val registryRegex = text("registryIndex")
    }

    class Procedure(id: EntityID<Int>) : IntEntity(id) {
        companion object : IntEntityClass<Procedure>(Procedures)

        var type by Procedures.type
        var content by Procedures.content
        var order by Procedures.order
        var document by Document referencedOn Procedures.document
    }

    private object Procedures : IntIdTable() {
        val type = text("type")
        val content = text("content")
        val order = integer("order")
        val document = reference("document", Documents)
    }

    class Document(id: EntityID<Int>) : IntEntity(id) {
        companion object : IntEntityClass<Document>(Documents)

        var identifier by Documents.identifier
        var baseFolderStructure by Documents.baseFolderStructure
        var registryRegex by Documents.registryRegex
        private val procedures by Procedure referrersOn Procedures.document
        fun getProceduresOrdered() = transaction { procedures.sortedBy { it.order } }
    }

    class Client(id: EntityID<Int>) : IntEntity(id) {
        companion object : IntEntityClass<Client>(Clients)

        var registry by Clients.registry
        var baseFolderStructure by Clients.baseFolderStructure
        var nickname by Clients.nickname
    }

    private object Clients : IntIdTable() {
        val registry = text("registry").uniqueIndex()
        val baseFolderStructure = text("baseFolderStructure", eagerLoading = true)
        val nickname = text("nickname")
    }
}
