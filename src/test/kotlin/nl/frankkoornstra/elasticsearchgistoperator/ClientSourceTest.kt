package nl.frankkoornstra.elasticsearchgistoperator

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

internal class ClientSourceTest {
    private class Implementation(override val hosts: List<String>) :
        ClientSource

    companion object {
        @JvmStatic
        fun provideImplementations() =
            listOf(
                Arguments.of(IndexSpec(IndexDefinition(), listOf("localhost:9200"))),
                Arguments.of(TemplateSpec(mapOf<String, Any>(), listOf("localhost:9200")))
            )
    }

    @ParameterizedTest
    @MethodSource("provideImplementations")
    fun `hostname is used`(source: ClientSource) {
        val host = source.createClient()
            .lowLevelClient
            .nodes
            .first()
            .host

        Assertions.assertEquals("localhost:9200", host.toHostString())
    }

    @Test
    fun `invalid hostname throws exception`() {
        assertThrows<ClientSource.CouldNotCreateClient> {
            Implementation(listOf("no spaces in hostname allowed")).createClient()
        }
    }
}
