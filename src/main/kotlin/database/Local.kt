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
        transaction(db) {
            SchemaUtils.create(Clients)
            commit()
            Clients.deleteAll()
            queryRemoteClients().forEach {
                Client.new {
                    registry = it.registry
                    baseFolderStructure = it.baseFolderStructure
                    nickname = it.nickname
                }
            }
            commit()
            SchemaUtils.create(Documents)
            commit()
            Documents.deleteAll()
            queryRemoteDocuments().forEach {
                Document.new {
                    type = it.type
                    regexMatch = it.regexMatch
                    baseFolderStructure = it.baseFolderStructure
                }
            }
        }
    }

    fun findByRegistry(registry: String) {
        return transaction(db) { Clients.registry eq registry }
    }

    fun findAll(): List<ResultRow> {
        return transaction(db) {
            Clients.selectAll().forEach(::println)
            Clients.selectAll().toList()
        }
    }
}

object Documents : IntIdTable() {
    val type = text("type")
    val baseFolderStructure = text("baseFolderStructure", eagerLoading = true)
    val regexMatch = text("regexMatch")

}

class Document(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Document>(Documents)

    var type by Documents.type
    var baseFolderStructure by Documents.baseFolderStructure
    var regexMatch by Documents.regexMatch
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
