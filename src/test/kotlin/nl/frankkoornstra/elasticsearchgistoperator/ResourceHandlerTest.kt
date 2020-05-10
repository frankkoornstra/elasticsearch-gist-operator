package nl.frankkoornstra.elasticsearchgistoperator

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import io.mockk.every
import io.mockk.mockk
import java.io.IOException
import org.elasticsearch.ElasticsearchException
import org.elasticsearch.action.ActionRequestValidationException
import org.elasticsearch.action.support.master.AcknowledgedResponse
import org.elasticsearch.client.IndicesClient
import org.elasticsearch.client.RestHighLevelClient
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class ResourceHandlerTest {
    val handler = ResourceHandler()
    val clientSource = mockk<ClientSource>()
    val restClient = mockk<RestHighLevelClient>()
    val indicesClient = mockk<IndicesClient>()
    val successfulRequest: IndicesClient.() -> AcknowledgedResponse = {
        hasRun = true
        AcknowledgedResponse(true)
    }
    val failedRequest: IndicesClient.() -> AcknowledgedResponse = {
        hasRun = true
        AcknowledgedResponse(false)
    }
    var hasRun = false

    companion object {
        val logListener = ListAppender<ILoggingEvent>().apply { start() }

        @BeforeAll
        @JvmStatic
        fun `attach log listener`() {
            val logger = ResourceHandler.logger as Logger
            logger.addAppender(logListener)
        }
    }

    @BeforeEach
    fun `setup clients, request and clean log listener`() {
        every { clientSource.createClient() } returns restClient
        every { restClient.indices() } returns indicesClient
        logListener.list.clear()

        hasRun = false
    }

    @Test
    fun `runs request that is successful`() {
        val result = handler.handleIndexRequest(clientSource, successfulRequest)

        Assertions.assertTrue(hasRun)
        Assertions.assertTrue(result is SuccessResult)
        logListener.assertLevel(Level.INFO)
        logListener.assertMessageContains("success")
    }

    @Test
    fun `runs request that is not acknowledged`() {
        val result = handler.handleIndexRequest(clientSource, failedRequest)

        Assertions.assertTrue(hasRun)
        Assertions.assertTrue(result is FailedResult && result.reason.contains("not acknowledged"))
        logListener.assertLevel(Level.ERROR)
        logListener.assertMessageContains("not acknowledged")
    }

    @Test
    fun `client could not be created`() {
        val clientSource = mockk<ClientSource>()
        every { clientSource.createClient() } throws ClientSource.CouldNotCreateClient(RuntimeException())

        val result = handler.handleIndexRequest(clientSource, failedRequest)

        Assertions.assertFalse(hasRun)
        Assertions.assertTrue(result is FailedResult && result.reason.contains("client"))
        logListener.assertLevel(Level.ERROR)
        logListener.assertMessageContains("client")
    }

    @Test
    fun `request validation fails`() {
        val result = handler.handleIndexRequest(clientSource) {
            throw ActionRequestValidationException().apply {
                validationErrors().add(0, "foo")
            }
        }

        Assertions.assertTrue(result is FailedResult && result.reason.contains("validation", true))
        logListener.assertLevel(Level.ERROR)
        logListener.assertMessageContains("validation")
        logListener.assertMessageContains("foo")
    }

    @Test
    fun `IO fails`() {
        val result = handler.handleIndexRequest(clientSource) {
            throw IOException("foo", RuntimeException("bar"))
        }

        Assertions.assertTrue(result is FailedResult && result.reason.contains("io", true))
        logListener.assertLevel(Level.ERROR)
        logListener.assertMessageContains("io")
        logListener.assertMessageContains("foo")
        logListener.assertMessageContains("bar")
    }

    @Test
    fun `Elasticsearch fails`() {
        val result = handler.handleIndexRequest(clientSource) {
            throw ElasticsearchException("foo")
        }

        Assertions.assertTrue(result is FailedResult && result.reason.contains("elasticsearch", true))
        logListener.assertLevel(Level.ERROR)
        logListener.assertMessageContains("elasticsearch")
        logListener.assertMessageContains("foo")
    }
}
