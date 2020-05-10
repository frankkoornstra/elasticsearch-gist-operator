package nl.frankkoornstra.elasticsearchgistoperator

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.github.containersolutions.operator.api.Controller
import com.github.containersolutions.operator.api.ResourceController
import io.fabric8.kubernetes.client.CustomResource
import java.util.Optional
import org.elasticsearch.ElasticsearchStatusException
import org.elasticsearch.action.admin.indices.template.delete.DeleteIndexTemplateRequest
import org.elasticsearch.action.support.master.AcknowledgedResponse
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.client.indices.PutIndexTemplateRequest
import org.elasticsearch.rest.RestStatus
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.stereotype.Service

@Controller(
    customResourceClass = Template::class,
    crdName = "elasticsearch-templates.frankkoornstra.nl"
)
@Service
class TemplateController(val handler: ResourceHandler) : ResourceController<Template> {
    companion object {
        val log = LoggerFactory.getLogger(TemplateController::class.toString())
    }

    override fun createOrUpdateResource(resource: Template): Optional<Template> {
        MDC.put(MDCKey.RESOURCE.toString(), resource.metadata.name)

        val result = handler.handleIndexRequest(resource.spec) {
            log.info("Upserting template")

            putTemplate(
                PutIndexTemplateRequest(resource.metadata.name).source(resource.spec.definition),
                RequestOptions.DEFAULT
            )
        }

        return Optional.of(
            when (result) {
                is SuccessResult -> resource.also {
                    it.status =
                        TemplateStatus(
                            status = Status.ACKNOWLEDGED.toString(),
                            message = ""
                        )
                }
                is FailedResult -> resource.also {
                    it.status =
                        TemplateStatus(
                            status = Status.FAILED.toString(),
                            message = result.reason
                        )
                }
            }
        )
    }

    override fun deleteResource(resource: Template): Boolean {
        MDC.put(MDCKey.RESOURCE.toString(), resource.metadata.name)

        val result = handler.handleIndexRequest(resource.spec) {
            log.info("Deleting template ${resource.metadata.name}")
            try {
                return@handleIndexRequest deleteTemplate(
                    DeleteIndexTemplateRequest(resource.metadata.name),
                    RequestOptions.DEFAULT
                )
            } catch (e: ElasticsearchStatusException) {
                if (e.status() != RestStatus.NOT_FOUND) {
                    throw e
                }
            }

            AcknowledgedResponse(true)
        }

        return when (result) {
            is SuccessResult -> true
            is FailedResult -> false
        }
    }
}

data class Template @JsonCreator constructor(
    @JsonProperty("spec") val spec: TemplateSpec,
    @JsonProperty("status") var status: TemplateStatus?
) : CustomResource()

data class TemplateSpec @JsonCreator constructor(
    @JsonProperty("definition") val definition: Map<String, *>,
    @JsonProperty("hosts") override val hosts: List<String>
) : ClientSource

data class TemplateStatus @JsonCreator constructor(
    @JsonProperty("status") var status: String,
    @JsonProperty("message") var message: String? = ""
)
