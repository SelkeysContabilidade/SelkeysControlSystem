package database

import Preferences.props
import com.azure.cosmos.CosmosClientBuilder
import com.azure.cosmos.CosmosContainer
import com.azure.cosmos.models.CosmosQueryRequestOptions
import com.azure.cosmos.util.CosmosPagedIterable
import com.azure.identity.InteractiveBrowserCredentialBuilder

object RemoteConnector {
    class Client {
        lateinit var registry: String
        lateinit var baseFolderStructure: String
        lateinit var nickname: String
    }

    class Document {
        lateinit var type: String
        lateinit var baseFolderStructure: String
        lateinit var regexMatch: String
    }

    private var clientContainer: CosmosContainer? = null
    private var documentsContainer: CosmosContainer? = null

    private fun init() = CosmosClientBuilder()
        .endpoint(props.getProperty("remoteDatabase"))
        .credential(InteractiveBrowserCredentialBuilder().build())
        .buildClient()
        .getDatabase("ControlSystemData").let {
            clientContainer = it.getContainer("Clients")
            documentsContainer = it.getContainer("Documents")
        }


    fun queryRemoteClients(): CosmosPagedIterable<Client> {
        if (clientContainer == null) init()
        return clientContainer!!.queryItems("select * from c", CosmosQueryRequestOptions(), Client::class.java)
    }

    fun queryRemoteDocuments(): CosmosPagedIterable<Document> {
        if (documentsContainer == null) init()
        return documentsContainer!!.queryItems("select * from c", CosmosQueryRequestOptions(), Document::class.java)
    }
}
