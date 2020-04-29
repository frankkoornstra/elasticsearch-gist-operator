package nl.frankkoornstra.elasticsearchgistoperator

import java.io.IOException
import org.elasticsearch.ElasticsearchStatusException
import org.elasticsearch.action.admin.indices.template.delete.DeleteIndexTemplateRequest
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.client.indices.GetIndexTemplatesRequest
import org.elasticsearch.client.indices.IndexTemplateMetaData
import org.elasticsearch.client.indices.PutIndexTemplateRequest
import org.elasticsearch.rest.RestStatus
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@Tag("integration")
class TemplateIntegrationTest {
    private val template = Template(
        status = TemplateStatus(status = ""),
        spec = TemplateSpec(
            definition = mapOf(
                "index_patterns" to "test*"
            ),
            hosts = listOf("localhost:9200")
        )
    ).apply {
        metadata.name = "test"
    }
    private val controller = TemplateController(ResourceHandler())
    private val client = template.spec.createClient()

    @BeforeEach
    fun `delete all templates before the tests are run`() {
        try {
            client.indices().deleteTemplate(DeleteIndexTemplateRequest("*"), RequestOptions.DEFAULT)
        } catch (e: IOException) {
        }
    }

    @Test
    fun `create a template`() {
        Assertions.assertNull(getTemplate("test"))

        controller.createOrUpdateResource(template)

        Assertions.assertNotNull(getTemplate("test"))
    }

    @Test
    fun `upsert a template`() {
        Assertions.assertNull(getTemplate("test"))

        client
            .indices()
            .putTemplate(PutIndexTemplateRequest("test").patterns(listOf("*")), RequestOptions.DEFAULT)

        Assertions.assertEquals("*", getTemplate("test")?.patterns()?.first())

        controller.createOrUpdateResource(template)

        Assertions.assertEquals("test*", getTemplate("test")?.patterns()?.first())
    }

    @Test
    fun `delete an existing template`() {
        client
            .indices()
            .putTemplate(PutIndexTemplateRequest("test").patterns(listOf("*")), RequestOptions.DEFAULT)
        Assertions.assertNotNull(getTemplate("test"))

        controller.deleteResource(template)

        Assertions.assertNull(getTemplate("test"))
    }

    @Test
    fun `delete a non-existing template`() {
        Assertions.assertNull(getTemplate("test"))

        controller.deleteResource(template)

        Assertions.assertNull(getTemplate("test"))
    }

    private fun getTemplate(name: String): IndexTemplateMetaData? =
        try {
            client
                .indices()
                .getIndexTemplate(GetIndexTemplatesRequest(name), RequestOptions.DEFAULT)
                .indexTemplates
                .first { it.name().equals(name) }
        } catch (e: ElasticsearchStatusException) {
            if (e.status().equals(RestStatus.NOT_FOUND)) null else throw e
        }
}
