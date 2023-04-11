package database

import Preferences.props
import database.RemoteConnector.queryRemoteClients
import database.RemoteConnector.queryRemoteDocuments
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction


object LocalDatabase {
    private val db = Database.connect(props.getProperty("localDatabase"), driver = "org.h2.Driver", "Selkeys", "")

    fun updateDatabase() {
        //fetch remote clients before doing anything
        val sourceClients = queryRemoteClients()
        val sourceDocuments = queryRemoteDocuments()
        if (sourceClients == null || sourceDocuments == null) {
            return
        }
        //clear all local databases
        transaction(db) {
            Procedures.dropStatement().forEach(::exec)
            Documents.dropStatement().forEach(::exec)
            Clients.dropStatement().forEach(::exec)
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
    }

    fun findByRegistry(registry: String) {
        return transaction(db) { Clients.registry eq registry }
    }

    fun findAllClients(): List<Client> {
        return transaction(db) {
            Clients.selectAll().map { transaction { Client.wrapRow(it) } }.toList()
        }
    }

    fun findAllDocuments(): List<Document> {
        return transaction(db) {
            Documents.selectAll().map { transaction { Document.wrapRow(it) } }.toList()
        }
    }
}

object Documents : IntIdTable() {
    val identifier = text("identifier")
    val baseFolderStructure = text("baseFolderStructure", eagerLoading = true)
}

class Procedure(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Procedure>(Procedures)

    var type by Procedures.type
    var content by Procedures.content
    var order by Procedures.order
    var document by Document referencedOn Procedures.document
}

object Procedures : IntIdTable() {
    val type = text("type")
    val content = text("content")
    val order = integer("order")
    val document = reference("document", Documents)
}

class Document(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Document>(Documents)

    var identifier by Documents.identifier
    var baseFolderStructure by Documents.baseFolderStructure
    val procedures by Procedure referrersOn Procedures.document
}

class Client(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Client>(Clients)

    var registry by Clients.registry
    var baseFolderStructure by Clients.baseFolderStructure
    var nickname by Clients.nickname
}

object Clients : IntIdTable() {
    val registry = text("registry").uniqueIndex()
    val baseFolderStructure = text("baseFolderStructure", eagerLoading = true)
    val nickname = text("nickname")
}
