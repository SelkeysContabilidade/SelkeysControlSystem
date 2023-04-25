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
        lateinit var identifier: String
        lateinit var baseFolderStructure: String
        lateinit var procedures: List<Procedure>
        lateinit var registryRegex: String
    }

    class Procedure {
        var order = 0
        lateinit var type: String
        lateinit var content: String
    }

    class Translation {
        lateinit var key: String
        lateinit var translation: String
    }

    private var clientContainer: CosmosContainer? = null
    private var documentsContainer: CosmosContainer? = null
    private var translationContainer: CosmosContainer? = null

    private fun init() = CosmosClientBuilder()
        .endpoint(props.getProperty("remoteDatabase"))
        .credential(InteractiveBrowserCredentialBuilder().build())
        .buildClient()
        .getDatabase("ControlSystemData").let {
            clientContainer = it.getContainer("Clients")
            documentsContainer = it.getContainer("Documents")
            translationContainer = it.getContainer("Translations")
        }


    fun queryRemoteClients(): CosmosPagedIterable<Client>? {
        clientContainer ?: init()
        return clientContainer?.queryItems("select * from c", CosmosQueryRequestOptions(), Client::class.java)
    }

    fun queryRemoteDocuments(): CosmosPagedIterable<Document>? {
        documentsContainer ?: init()
        return documentsContainer?.queryItems("select * from c", CosmosQueryRequestOptions(), Document::class.java)
    }

    fun queryRemoteTranslations(): CosmosPagedIterable<Translation>? {
        translationContainer ?: init()
        return translationContainer?.queryItems("select * from c", CosmosQueryRequestOptions(), Translation::class.java)
    }
}
