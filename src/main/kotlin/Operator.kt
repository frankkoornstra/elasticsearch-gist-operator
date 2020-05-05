package nl.frankkoornstra.elasticsearchgistoperator

import org.apache.http.HttpHost
import org.elasticsearch.ElasticsearchException
import org.elasticsearch.action.ActionRequestValidationException
import org.elasticsearch.action.support.master.AcknowledgedResponse
import org.elasticsearch.client.IndicesClient
import org.elasticsearch.client.RestClient
import org.elasticsearch.client.RestHighLevelClient
import org.slf4j.LoggerFactory
import java.io.IOException

sealed class GenericResult()
class SuccessResult() : GenericResult()
class FailedResult(val reason: String) : GenericResult()

interface ClientSource {
    val hosts: List<String>

    fun createClient(): RestHighLevelClient {
        try {
            return RestHighLevelClient(
                RestClient
                    .builder(*hosts.map { HttpHost.create(it) }.toTypedArray())
                    .setRequestConfigCallback {
                        it.setConnectTimeout(1000)
                        it.setSocketTimeout(1000)
                    }
            )
        } catch (e: Throwable) {
            throw CouldNotCreateClient(e)
        }
    }

    class CouldNotCreateClient(e: Throwable) : RuntimeException(e.message, e)
}

class ResourceHandler {
    companion object {
        val logger = LoggerFactory.getLogger(ResourceHandler::class.java)
    }

    fun handleIndexRequest(
        clientSource: ClientSource,
        request: IndicesClient.() -> AcknowledgedResponse
    ): GenericResult {
        try {
            val client = clientSource.createClient()
            val result = client.indices().run { request() }
            if (!result.isAcknowledged) {
                throw IOException("not acknowledged")
            }
        } catch (e: ClientSource.CouldNotCreateClient) {
            logger.error("Could not create client: $e")
            return FailedResult("Could not create client $e")
        } catch (e: ActionRequestValidationException) {
            logger.info("Validation failed: ${e.validationErrors()}")
            return FailedResult("Validation failed: ${e.validationErrors()}")
        } catch (e: IOException) {
            logger.info("IO failed: ${e.cause}")
            return FailedResult(e.cause.toString())
        } catch (e: ElasticsearchException) {
            logger.info("Elasticsearch threw a fit: ${e.detailedMessage}")
            return FailedResult(e.detailedMessage)
        }

        logger.info("Success")
        return SuccessResult()
    }
}
