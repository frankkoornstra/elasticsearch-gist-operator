package nl.frankkoornstra.elasticsearchgistoperator

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

@Tag("unit")
internal class StatusTest {
    @Test
    fun `pretty status for k8s`() {
        Assertions.assertEquals("Failed", Status.FAILED.toString())
    }
}
