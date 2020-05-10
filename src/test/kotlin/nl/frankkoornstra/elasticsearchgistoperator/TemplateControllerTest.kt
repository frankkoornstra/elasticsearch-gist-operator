package nl.frankkoornstra.elasticsearchgistoperator

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.io.IOException
import org.elasticsearch.ElasticsearchStatusException
import org.elasticsearch.action.admin.indices.template.delete.DeleteIndexTemplateRequest
import org.elasticsearch.action.support.master.AcknowledgedResponse
import org.elasticsearch.client.IndicesClient
import org.elasticsearch.client.indices.PutIndexTemplateRequest
import org.elasticsearch.rest.RestStatus
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.slf4j.MDC

internal class TemplateControllerTest {
    val client = mockk<IndicesClient>()
    val lambdaSlot = slot<IndicesClient.() -> AcknowledgedResponse>()
    val handler = mockk<ResourceHandler>()
    val resource = Template(
        TemplateSpec(
            mapOf<String, Any>(),
            listOf()
        ),
        TemplateStatus(
            "Pending"
        )
    ).apply {
        metadata.name = "test"
    }
    val controller = TemplateController(handler)

    companion object {
        val logListener = ListAppender<ILoggingEvent>().apply { start() }

        @BeforeAll
        @JvmStatic
        fun `attach log listener`() {
            val logger = TemplateController.log as Logger
            logger.addAppender(logListener)
        }
    }

    @BeforeEach
    fun `reset state`() {
        logListener.list.clear()
    }

    @Test
    fun `successful upsert returns right status`() {
        every { handler.handleIndexRequest(resource.spec, any()) } returns SuccessResult()

        val result = controller.createOrUpdateResource(resource)

        Assertions.assertEquals(Status.ACKNOWLEDGED.toString(), result.get().status?.status)
        Assertions.assertTrue(result.get().status?.message?.isEmpty() ?: false)
    }

    @Test
    fun `failed upsert returns right status`() {
        every { handler.handleIndexRequest(resource.spec, any()) } returns FailedResult("reason")

        val result = controller.createOrUpdateResource(resource)

        Assertions.assertEquals(Status.FAILED.toString(), result.get().status?.status)
        Assertions.assertEquals("reason", result.get().status?.message)
    }

    @Test
    fun `upsert sets resource name in MDC`() {
        every { handler.handleIndexRequest(resource.spec, any()) } returns SuccessResult()

        controller.createOrUpdateResource(resource)

        Assertions.assertEquals("test", MDC.get(MDCKey.RESOURCE.toString()))
    }

    @Test
    fun `upsert calls handler correctly`() {
        `setup handler request mocking`()
        every {
            client.putTemplate(ofType(PutIndexTemplateRequest::class), any())
        } returns AcknowledgedResponse(true)

        controller.createOrUpdateResource(resource)

        verify { client.putTemplate(ofType(PutIndexTemplateRequest::class), any()) }
        logListener.assertMessageContains("upserting template")
    }

    @Test
    fun `successful delete returns true`() {
        every { handler.handleIndexRequest(resource.spec, any()) } returns SuccessResult()

        Assertions.assertTrue(controller.deleteResource(resource))
    }

    @Test
    fun `failed delete returns false`() {
        every { handler.handleIndexRequest(resource.spec, any()) } returns FailedResult("foo")

        Assertions.assertFalse(controller.deleteResource(resource))
    }

    @Test
    fun `delete calls handler correctly`() {
        `setup handler request mocking`()
        every {
            client.deleteTemplate(ofType(DeleteIndexTemplateRequest::class), any())
        } returns AcknowledgedResponse(true)

        controller.deleteResource(resource)

        verify { client.deleteTemplate(ofType(DeleteIndexTemplateRequest::class), any()) }
        confirmVerified(client)
        logListener.assertMessageContains("deleting template")
    }

    @Test
    fun `delete calls handler with lambda that ignores a template not found exception`() {
        `setup handler request mocking`()
        every {
            client.deleteTemplate(ofType(DeleteIndexTemplateRequest::class), any())
        } throws ElasticsearchStatusException("", RestStatus.NOT_FOUND, null)

        controller.deleteResource(resource)
    }

    @Test
    fun `delete calls handler with lambda that forwards status exceptions with a different cause`() {
        `setup handler request mocking`()
        every {
            client.deleteTemplate(ofType(DeleteIndexTemplateRequest::class), any())
        } throws ElasticsearchStatusException("", RestStatus.BAD_REQUEST, null)

        assertThrows<ElasticsearchStatusException> {
            controller.deleteResource(resource)
        }
    }

    @Test
    fun `delete calls handler with lambda that forwards all other exceptions`() {
        `setup handler request mocking`()
        every {
            client.deleteTemplate(ofType(DeleteIndexTemplateRequest::class), any())
        } throws IOException()

        assertThrows<IOException> {
            controller.deleteResource(resource)
        }
    }

    private fun `setup handler request mocking`() {
        every {
            handler.handleIndexRequest(any(), capture(lambdaSlot))
        } answers {
            lambdaSlot.captured(client)
            SuccessResult()
        }
    }
}
