package nl.frankkoornstra.elasticsearchgistoperator

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@Tag("unit")
internal class MDCKeyTest {
    @Test
    fun `lowercase key for logs`() {
        Assertions.assertEquals("resource", MDCKey.RESOURCE.toString())
    }
}
