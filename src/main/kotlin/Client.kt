import Preferences.props
import com.azure.cosmos.CosmosClientBuilder
import com.azure.cosmos.CosmosContainer
import com.azure.cosmos.models.CosmosQueryRequestOptions
import com.azure.cosmos.util.CosmosPagedIterable
import com.azure.identity.InteractiveBrowserCredentialBuilder


class Document {
    lateinit var registry: String
    lateinit var baseFolderStructure: String
    lateinit var nickname: String

}

object DatabaseConnector {
    class Client {
        lateinit var registry: String
        lateinit var baseFolderStructure: String
        lateinit var nickname: String

        @Override
        override fun toString(): String {
            return "CNPJ: $registry, Apelido: $nickname, Pasta: $baseFolderStructure "
        }
    }

    private var clientContainer: CosmosContainer
    private var documentsContainer: CosmosContainer

    init {
        val database = CosmosClientBuilder()
            .endpoint(props.getProperty("endpoint"))
            .credential(InteractiveBrowserCredentialBuilder().build())
            .buildClient()
            .getDatabase("ControlSystemData")
        clientContainer = database.getContainer("Clients")
        documentsContainer = database.getContainer("Documents")
    }

    fun queryClients(): CosmosPagedIterable<Client> {
        return clientContainer.queryItems("select * from c", CosmosQueryRequestOptions(), Client::class.java)
    }
}
