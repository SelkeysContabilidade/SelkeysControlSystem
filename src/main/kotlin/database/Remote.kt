package database

import Preferences.authRecordFilePath
import Preferences.props
import com.azure.cosmos.CosmosClientBuilder
import com.azure.cosmos.CosmosContainer
import com.azure.cosmos.models.CosmosQueryRequestOptions
import com.azure.cosmos.util.CosmosPagedIterable
import com.azure.identity.AuthenticationRecord
import com.azure.identity.InteractiveBrowserCredential
import com.azure.identity.InteractiveBrowserCredentialBuilder
import com.azure.identity.TokenCachePersistenceOptions
import java.io.File

object RemoteConnector {
    class Client {
        lateinit var registry: List<String>
        lateinit var baseFolderStructure: String
        lateinit var nickname: String
    }

    class Document {
        lateinit var identifier: String
        lateinit var baseFolderStructure: String
        lateinit var procedures: List<Procedure>
        lateinit var registryRegex: String
        var secondaryStorage = false
    }

    class Procedure {
        var order = 0
        var folder = false
        lateinit var type: String
        lateinit var content: String
    }

    class Translation {
        lateinit var key: String
        lateinit var value: String
    }

    private var clientContainer: CosmosContainer? = null
    private var documentsContainer: CosmosContainer? = null
    private var translationContainer: CosmosContainer? = null

    private fun connect() {
        fun loadToken() = InteractiveBrowserCredentialBuilder()
            .tokenCachePersistenceOptions(TokenCachePersistenceOptions())
            .authenticationRecord(AuthenticationRecord.deserialize(File(authRecordFilePath).inputStream()))
            .build()

        fun buildClient(credential: InteractiveBrowserCredential?) = CosmosClientBuilder()
            .endpoint(props.getProperty("remoteDatabase"))
            .credential(credential)
            .buildClient()
            .getDatabase("ControlSystemData")
            .let {
                clientContainer = it.getContainer("Clients")
                documentsContainer = it.getContainer("Documents")
                translationContainer = it.getContainer("Translations")
            }


        try {
            buildClient(loadToken())
        } catch (_: Exception) {
            InteractiveBrowserCredentialBuilder()
                .tokenCachePersistenceOptions(TokenCachePersistenceOptions())
                .build().authenticate().block()
                .let { it?.serialize(File(authRecordFilePath).outputStream()) }
            buildClient(loadToken())
        }
    }

    fun queryRemoteClients(): CosmosPagedIterable<Client>? {
        clientContainer ?: connect()
        return clientContainer?.queryItems("select * from c", CosmosQueryRequestOptions(), Client::class.java)
    }

    fun queryRemoteDocuments(): CosmosPagedIterable<Document>? {
        documentsContainer ?: connect()
        return documentsContainer?.queryItems("select * from c", CosmosQueryRequestOptions(), Document::class.java)
    }

    fun queryRemoteTranslations(): CosmosPagedIterable<Translation>? {
        translationContainer ?: connect()
        return translationContainer?.queryItems("select * from c", CosmosQueryRequestOptions(), Translation::class.java)
    }
}
