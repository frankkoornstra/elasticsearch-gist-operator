package nl.frankkoornstra.elasticsearchgistoperator

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import io.mockk.Called
import io.mockk.clearMocks
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.io.IOException
import org.elasticsearch.ElasticsearchStatusException
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequest
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest
import org.elasticsearch.action.admin.indices.settings.put.UpdateSettingsRequest
import org.elasticsearch.action.support.master.AcknowledgedResponse
import org.elasticsearch.client.IndicesClient
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.client.indices.CreateIndexRequest
import org.elasticsearch.client.indices.CreateIndexResponse
import org.elasticsearch.client.indices.GetIndexRequest
import org.elasticsearch.client.indices.GetIndexResponse
import org.elasticsearch.client.indices.PutMappingRequest
import org.elasticsearch.cluster.metadata.AliasMetaData
import org.elasticsearch.rest.RestStatus
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.slf4j.MDC

internal class IndexControllerTest {
    val client = mockk<IndicesClient>()
    val lambdaSlot = slot<IndicesClient.() -> AcknowledgedResponse>()
    val handler = mockk<ResourceHandler>()
    val resource = Index(
        IndexSpec(
            IndexDefinition(),
            listOf("foo")
        ),
        IndexStatus("Pending")
    ).apply {
        metadata.name = "test"
    }
    val controller = IndexController(handler)

    companion object {
        val logListener = ListAppender<ILoggingEvent>().apply { start() }

        @BeforeAll
        @JvmStatic
        fun `attach log listener`() {
            val logger = IndexController.log as Logger
            logger.addAppender(logListener)
        }
    }

    @BeforeEach
    fun `reset state and setup default answers`() {
        logListener.list.clear()

        val indexResponse = mockk<GetIndexResponse>()
        every {
            client.get(ofType(GetIndexRequest::class), ofType(RequestOptions::class))
        } returns indexResponse
        every {
            indexResponse.aliases
        } returns mapOf()
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
    fun `upsert updates mapping successfully if index exists`() {
        `setup handler request mocking`()
        `setup update request mocking`()

        val resource = `create resource copy with`(mappings = mapOf("foo" to "bar"))
        controller.createOrUpdateResource(resource)

        verify {
            client.exists(ofType(GetIndexRequest::class), ofType(RequestOptions::class))
            client.get(ofType(GetIndexRequest::class), ofType(RequestOptions::class))
            client.putMapping(ofType(PutMappingRequest::class), ofType(RequestOptions::class))
        }
        confirmVerified(client)
        logListener.assertHasMessageThatContains("upserting index")
        logListener.assertHasMessageThatContains("updating mapping")
    }

    @Test
    fun `does not upsert mapping if null`() {
        `setup handler request mocking`()
        `setup update request mocking`()

        val resource = `create resource copy with`(mappings = null)
        controller.createOrUpdateResource(resource)

        logListener.assertNoMessageThatContains("updating mapping")
    }

    @Test
    fun `upsert updates mapping and logs if unsuccessful`() {
        `setup handler request mocking`()
        `setup update request mocking`(mappingResponse = AcknowledgedResponse(false))

        val resource = `create resource copy with`(mappings = mapOf("foo" to "bar"))
        controller.createOrUpdateResource(resource)

        logListener.assertHasMessageThatContains("mapping not acknowledged")
    }

    @Test
    fun `upsert updates settings successfully if index exists`() {
        `setup handler request mocking`()
        `setup update request mocking`()

        val resource = `create resource copy with`(settings = mapOf("foo" to "bar"))
        controller.createOrUpdateResource(resource)

        verify {
            client.exists(ofType(GetIndexRequest::class), ofType(RequestOptions::class))
            client.get(ofType(GetIndexRequest::class), ofType(RequestOptions::class))
            client.putSettings(ofType(UpdateSettingsRequest::class), ofType(RequestOptions::class))
        }
        confirmVerified(client)
        logListener.assertHasMessageThatContains("upserting index")
        logListener.assertHasMessageThatContains("updating settings")
    }

    @Test
    fun `does not upsert settings if null`() {
        `setup handler request mocking`()
        `setup update request mocking`()

        val resource = `create resource copy with`(settings = null)
        controller.createOrUpdateResource(resource)

        logListener.assertNoMessageThatContains("updating settings")
    }

    @Test
    fun `upsert updates settings and logs if unsuccessful`() {
        `setup handler request mocking`()
        `setup update request mocking`(settingsResponse = AcknowledgedResponse(false))

        val resource = `create resource copy with`(settings = mapOf("foo" to "bar"))
        controller.createOrUpdateResource(resource)

        logListener.assertHasMessageThatContains("settings not acknowledged")
    }

    @Test
    fun `upsert updates aliases successfully if index exists`() {
        clearMocks(client)
        `setup handler request mocking`()
        `setup update request mocking`()

        val toBeRemoved = mockk<AliasMetaData>()
        every { toBeRemoved.alias } returns "oldAlias"
        val keepsExisting = mockk<AliasMetaData>()
        every { keepsExisting.alias } returns "keep"

        val indexResponse = mockk<GetIndexResponse>()
        every { indexResponse.aliases } returns mapOf("test" to listOf(toBeRemoved, keepsExisting))
        every { client.get(ofType(GetIndexRequest::class), ofType(RequestOptions::class)) } returns indexResponse

        val resource = `create resource copy with`(
            aliases = mapOf(
                "keep" to mapOf<String, Any>(),
                "create" to mapOf<String, Any>()
            )
        )
        controller.createOrUpdateResource(resource)

        verify {
            client.exists(ofType(GetIndexRequest::class), ofType(RequestOptions::class))
            client.get(ofType(GetIndexRequest::class), ofType(RequestOptions::class))
            client.updateAliases(ofType(IndicesAliasesRequest::class), ofType(RequestOptions::class))
        }
        confirmVerified(client)
        logListener.assertHasMessageThatContains("upserting index")
        logListener.assertHasMessageThatContains("adding alias")
        logListener.assertHasMessageThatContains("deleting alias")
    }

    @Test
    fun `upsert does nothing if no aliases exist and aliases null`() {
        `setup handler request mocking`()
        `setup update request mocking`()

        val resource = `create resource copy with`(aliases = null)
        controller.createOrUpdateResource(resource)

        verify {
            client.exists(ofType(GetIndexRequest::class), ofType(RequestOptions::class))
            client.get(ofType(GetIndexRequest::class), ofType(RequestOptions::class))
            client.updateAliases(ofType(IndicesAliasesRequest::class), ofType(RequestOptions::class)) wasNot Called
        }
        confirmVerified(client)
    }

    @Test
    fun `upsert updates aliases and logs if unsuccessful`() {
        `setup handler request mocking`()
        `setup update request mocking`(aliasResponse = AcknowledgedResponse(false))

        val resource = `create resource copy with`(aliases = mapOf("foo" to mapOf<String, Any>()))
        controller.createOrUpdateResource(resource)

        logListener.assertHasMessageThatContains("aliases not acknowledged")
    }

    @Test
    fun `upsert creates index if it doesn't exist`() {
        `setup handler request mocking`()
        every {
            client.exists(ofType(GetIndexRequest::class), ofType(RequestOptions::class))
        } returns false
        every {
            client.create(ofType(CreateIndexRequest::class), ofType(RequestOptions::class))
        } returns CreateIndexResponse(true, true, "foo")

        controller.createOrUpdateResource(resource)

        verify {
            client.exists(ofType(GetIndexRequest::class), ofType(RequestOptions::class))
            client.create(ofType(CreateIndexRequest::class), ofType(RequestOptions::class))
        }
        confirmVerified(client)
        logListener.assertMessageContains("upserting index")
        logListener.assertMessageContains("creating index", 1)
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
            client.delete(ofType(DeleteIndexRequest::class), ofType(RequestOptions::class))
        } returns AcknowledgedResponse(true)

        controller.deleteResource(resource)

        verify { client.delete(ofType(DeleteIndexRequest::class), ofType(RequestOptions::class)) }
        confirmVerified(client)
        logListener.assertMessageContains("deleting index")
    }

    @Test
    fun `delete calls handler with lambda that ignores a template not found exception`() {
        `setup handler request mocking`()
        every {
            client.delete(ofType(DeleteIndexRequest::class), ofType(RequestOptions::class))
        } throws ElasticsearchStatusException("", RestStatus.NOT_FOUND, null)

        controller.deleteResource(resource)
    }

    @Test
    fun `delete calls handler with lambda that forwards status exceptions with a different cause`() {
        `setup handler request mocking`()
        every {
            client.delete(ofType(DeleteIndexRequest::class), ofType(RequestOptions::class))
        } throws ElasticsearchStatusException("", RestStatus.BAD_REQUEST, null)

        assertThrows<ElasticsearchStatusException> {
            controller.deleteResource(resource)
        }
    }

    @Test
    fun `delete calls handler with lambda that forwards all other exceptions`() {
        `setup handler request mocking`()
        every {
            client.delete(ofType(DeleteIndexRequest::class), ofType(RequestOptions::class))
        } throws IOException()

        assertThrows<IOException> {
            controller.deleteResource(resource)
        }
    }

    private fun `create resource copy with`(
        settings: Map<String, Any>? = mapOf(),
        mappings: Map<String, Any>? = mapOf(),
        aliases: Map<String, Any>? = mapOf()
    ) = resource.copy(
        spec = resource.spec.copy(
            definition = resource.spec.definition.copy(
                settings = settings,
                mappings = mappings,
                aliases = aliases
            )
        )
    ).apply {
        metadata.name = "test"
    }

    private fun `setup update request mocking`(
        mappingResponse: AcknowledgedResponse = AcknowledgedResponse(true),
        settingsResponse: AcknowledgedResponse = AcknowledgedResponse(true),
        aliasResponse: AcknowledgedResponse = AcknowledgedResponse(true)
    ) {
        every {
            client.exists(ofType(GetIndexRequest::class), ofType(RequestOptions::class))
        } returns true
        every {
            client.putMapping(ofType(PutMappingRequest::class), ofType(RequestOptions::class))
        } returns mappingResponse
        every {
            client.putSettings(ofType(UpdateSettingsRequest::class), ofType(RequestOptions::class))
        } returns settingsResponse
        every {
            client.updateAliases(ofType(IndicesAliasesRequest::class), ofType(RequestOptions::class))
        } returns aliasResponse
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
