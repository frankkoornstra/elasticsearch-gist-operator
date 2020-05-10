package nl.frankkoornstra.elasticsearchgistoperator

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class IndexDefinitionTest {
    @Test
    fun `only returns dynaic settings`() {
        val definition = IndexDefinition(
            settings = mapOf(
                "number_of_shards" to "foo",
                "shard.check_on_startup" to "foo",
                "codec" to "foo",
                "routing_partition_size" to "foo",
                "load_fixed_bitset_filters_eagerly" to "foo",
                "number_of_replicas" to "foo"
            )
        )
        val expected = mapOf("number_of_replicas" to "foo")

        Assertions.assertEquals(expected, definition.getDynamicSettings())
    }
}
