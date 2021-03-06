package nl.frankkoornstra.elasticsearchgistoperator

import java.io.IOException
import org.apache.http.HttpHost
import org.elasticsearch.ElasticsearchException
import org.elasticsearch.action.ActionRequestValidationException
import org.elasticsearch.action.support.master.AcknowledgedResponse
import org.elasticsearch.client.IndicesClient
import org.elasticsearch.client.RestClient
import org.elasticsearch.client.RestHighLevelClient
import org.slf4j.LoggerFactory
import org.springframework.boot.Banner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean

fun main(args: Array<String>) {
    runApplication<Application>(*args) {
        setBannerMode(Banner.Mode.OFF)
    }
}

@SpringBootApplication
class Application {
    @Bean
    fun createHandler() = ResourceHandler()
}

enum class MDCKey {
    RESOURCE;

    override fun toString(): String {
        return super.toString().toLowerCase()
    }
}

enum class Status {
    ACKNOWLEDGED,
    FAILED;

    override fun toString(): String {
        return super.toString().toLowerCase().capitalize()
    }
}

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
            val result = client.indices().run(request)
            if (!result.isAcknowledged) {
                throw ElasticsearchException("not acknowledged")
            }
        } catch (e: ClientSource.CouldNotCreateClient) {
            logger.error("Could not create client: $e")
            return FailedResult(
                "Could not create client $e"
            )
        } catch (e: ActionRequestValidationException) {
            logger.error("Validation failed: ${e.validationErrors()}")
            return FailedResult(
                "Validation failed: ${e.validationErrors()}"
            )
        } catch (e: IOException) {
            logger.error("IO failed: ${e.message} ${e.cause}")
            return FailedResult(
                "IO failed: ${e.message} ${e.cause}"
            )
        } catch (e: ElasticsearchException) {
            logger.error("Elasticsearch failed: ${e.detailedMessage}")
            return FailedResult(
                "Elasticsearch failed: ${e.detailedMessage}"
            )
        }

        logger.info("Success")
        return SuccessResult()
    }
}
