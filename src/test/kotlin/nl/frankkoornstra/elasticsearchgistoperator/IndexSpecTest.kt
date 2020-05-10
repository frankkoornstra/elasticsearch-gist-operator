package nl.frankkoornstra.elasticsearchgistoperator

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class IndexSpecTest {
    @Test
    fun `definition to ES map`() {
        val spec = IndexSpec(
            definition = IndexDefinition(
                settings = mapOf("s" to "A"),
                aliases = mapOf("a" to "B"),
                mappings = mapOf("m" to "C")
            ),
            hosts = listOf("foo")
        )
        val expected = mapOf(
            "settings" to mapOf("s" to "A"),
            "aliases" to mapOf("a" to "B"),
            "mappings" to mapOf("m" to "C")
        )

        Assertions.assertEquals(expected, spec.definitionAsMap)
    }
}
