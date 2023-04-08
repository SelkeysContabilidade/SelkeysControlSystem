import DatabaseConnector.queryClients
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction


object Test {
    private val db = Database.connect("jdbc:h2:~/.selkeys/clients;MODE=MYSQL", driver = "org.h2.Driver", "Selkeys", "")

    fun main() {
        transaction(db) {
            SchemaUtils.create(Clients)
            queryClients().forEach {
                Client.new {
                    registry = it.registry
                    baseFolderStructure = it.baseFolderStructure
                    nickname = it.nickname
                }
            }
        }
    }
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
