package nl.frankkoornstra.elasticsearchgistoperator

import java.io.IOException
import org.elasticsearch.ElasticsearchStatusException
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.client.indices.CreateIndexRequest
import org.elasticsearch.client.indices.GetIndexRequest
import org.elasticsearch.client.indices.GetIndexResponse
import org.elasticsearch.rest.RestStatus
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@Tag("integration")
internal class IndexIntegrationTest {
    private val index = Index(
        status = IndexStatus(
            status = ""
        ),
        spec = IndexSpec(
            definition = IndexDefinition(
                settings = mapOf(
                    "number_of_shards" to "3"
                ),
                mappings = mapOf(
                    "properties" to mapOf(
                        "keywordField" to mapOf(
                            "type" to "keyword"
                        )
                    )
                ),
                aliases = mapOf(
                    "aliasTest" to mapOf<String, Any>()
                )
            ),
            hosts = listOf(System.getenv("ELASTICSEARCH_HOST") ?: "localhost:9200")
        )
    ).apply {
        metadata.name = "test"
    }
    private val controller = IndexController(ResourceHandler())
    private val client = index.spec.createClient()

    @BeforeEach
    fun `delete all indices before the tests are run`() {
        try {
            client.indices().delete(DeleteIndexRequest("*"), RequestOptions.DEFAULT)
        } catch (e: IOException) {
        }
    }

    @Test
    fun `create an index which respects the settings, mappings and aliases`() {
        Assertions.assertNull(getIndex("test"))

        controller.createOrUpdateResource(index)

        val newIndex = getIndex("test")
        Assertions.assertNotNull(newIndex)
        Assertions.assertEquals("3", newIndex?.settings?.get("test")?.get("index.number_of_shards"))
        Assertions.assertEquals(index.spec.definition.mappings, newIndex?.mappings?.get("test")?.sourceAsMap())
        Assertions.assertEquals("aliasTest", newIndex?.aliases?.get("test")?.get(0)?.alias)
    }

    @Test
    @Suppress("UNCHECKED_CAST")
    fun `update index mappings that are backwards compatible`() {
        val mapping = mapOf(
            "properties" to mapOf(
                "integerField" to mapOf(
                    "type" to "integer"
                )
            )
        )
        client
            .indices()
            .create(CreateIndexRequest("test").mapping(mapping), RequestOptions.DEFAULT)
        Assertions.assertEquals(mapping, getIndex("test")?.mappings?.get("test")?.sourceAsMap())

        controller.createOrUpdateResource(index)

        val newMapping = getIndex("test")?.mappings?.get("test")?.sourceAsMap()?.get("properties") as Map<String, *>
        Assertions.assertTrue(newMapping.containsKey("integerField"))
        Assertions.assertTrue(newMapping.containsKey("keywordField"))
    }

    @Test
    @Suppress("UNCHECKED_CAST")
    fun `update index mappings that are backwards incompatible`() {
        val mapping = mapOf(
            "properties" to mapOf(
                "keywordField" to mapOf(
                    "type" to "integer"
                )
            )
        )
        client
            .indices()
            .create(CreateIndexRequest("test").mapping(mapping), RequestOptions.DEFAULT)
        Assertions.assertEquals(mapping, getIndex("test")?.mappings?.get("test")?.sourceAsMap())

        val result = controller.createOrUpdateResource(index)
        Assertions.assertEquals(Status.FAILED.toString(), result.get().status?.status)
        Assertions.assertTrue(result.get().status?.message?.contains("different type", ignoreCase = true) ?: false)
    }

    @Test
    fun `update dynamic settings`() {
        controller.createOrUpdateResource(index)
        val originalNumberOfReplicas = getIndex("test")?.settings?.get("test")?.get("index.number_of_replicas")
        Assertions.assertNotEquals("3", originalNumberOfReplicas)

        val newIndexSettings = index.copy(
            spec = index.spec.copy(
                definition = index.spec.definition.copy(
                    settings = index.spec.definition.settings?.toMutableMap()?.apply {
                        put("number_of_replicas", "3")
                    }
                )
            )
        ).apply {
            metadata = index.metadata
        }

        controller.createOrUpdateResource(newIndexSettings)
        val numberOfReplicas = getIndex("test")?.settings?.get("test")?.get("index.number_of_replicas")

        Assertions.assertEquals("3", numberOfReplicas)
    }

    @Test
    fun `remove existing alias and add a new one`() {
        controller.createOrUpdateResource(index)

        val newIndexAliases = index.copy(
            spec = index.spec.copy(
                definition = index.spec.definition.copy(
                    aliases = mapOf(
                        "aliasReplacement" to mapOf<String, Any>()
                    )
                )
            )
        ).apply {
            metadata = index.metadata
        }
        controller.createOrUpdateResource(newIndexAliases)

        val aliases = getIndex("test")?.aliases?.get("test")
        Assertions.assertSame(1, aliases?.count())
        Assertions.assertEquals("aliasReplacement", aliases?.get(0)?.alias)
    }

    @Test
    fun `delete an existing index`() {
        client
            .indices()
            .create(CreateIndexRequest("test"), RequestOptions.DEFAULT)
        Assertions.assertNotNull(getIndex("test"))

        controller.deleteResource(index)

        Assertions.assertNull(getIndex("test"))
    }

    @Test
    fun `delete a non-existing index`() {
        Assertions.assertNull(getIndex("test"))

        controller.deleteResource(index)

        Assertions.assertNull(getIndex("test"))
    }

    private fun getIndex(name: String): GetIndexResponse? =
        try {
            client
                .indices()
                .get(GetIndexRequest(name), RequestOptions.DEFAULT)
        } catch (e: ElasticsearchStatusException) {
            if (e.status().equals(RestStatus.NOT_FOUND)) null else throw e
        }
}
