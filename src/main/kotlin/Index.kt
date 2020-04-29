package nl.frankkoornstra.elasticsearchgistoperator

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.github.containersolutions.operator.api.Controller
import com.github.containersolutions.operator.api.ResourceController
import io.fabric8.kubernetes.client.CustomResource
import java.util.Optional
import org.elasticsearch.ElasticsearchStatusException
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequest
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest
import org.elasticsearch.action.admin.indices.settings.put.UpdateSettingsRequest
import org.elasticsearch.action.support.master.AcknowledgedResponse
import org.elasticsearch.client.IndicesClient
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.client.indices.CreateIndexRequest
import org.elasticsearch.client.indices.GetIndexRequest
import org.elasticsearch.client.indices.PutMappingRequest
import org.elasticsearch.rest.RestStatus
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.stereotype.Service

@Controller(
    customResourceClass = Index::class,
    crdName = "elasticsearch-indices.frankkoornstra.nl"
)
@Service
class IndexController(val handler: ResourceHandler) : ResourceController<Index> {
    companion object {
        val log = LoggerFactory.getLogger(IndexController::class.toString())
    }

    override fun createOrUpdateResource(resource: Index): Optional<Index> {
        MDC.put(MDCKey.RESOURCE.toString(), resource.metadata.name)

        val result = handler.handleIndexRequest(resource.spec) {
            val name = resource.metadata.name
            log.info("Upserting index")

            val exists = exists(GetIndexRequest(name), RequestOptions.DEFAULT)
            if (exists) {
                log.info("Index already exists, updating")
                return@handleIndexRequest AcknowledgedResponse(updateIndex(resource))
            }

            log.info("Creating index")
            create(CreateIndexRequest(name).source(resource.spec.definitionAsMap), RequestOptions.DEFAULT)
        }

        return Optional.of(
            when (result) {
                is SuccessResult -> resource.also {
                    it.status = IndexStatus(status = Status.ACKNOWLEDGED.toString(), message = "")
                }
                is FailedResult -> resource.also {
                    it.status = IndexStatus(status = Status.FAILED.toString(), message = result.reason)
                }
            }

        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun IndicesClient.updateIndex(resource: Index): Boolean {
        val indexName = resource.metadata.name
        val definition = resource.spec.definition

        if (definition.mappings != null) {
            log.info("Updating mapping")
            val result = putMapping(PutMappingRequest(indexName).source(definition.mappings), RequestOptions.DEFAULT)

            if (!result.isAcknowledged) {
                log.info("Updating mapping not acknowledged")
                return false
            }
        }

        if (definition.settings != null) {
            log.info("Updating settings")

            val result = putSettings(UpdateSettingsRequest(indexName).settings(definition.settings), RequestOptions.DEFAULT)

            if (!result.isAcknowledged) {
                log.info("Updating settings not acknowledged")
                return false
            }
        }

        if (definition.aliases != null) {
            log.info("Updating aliases")

            val aliasesRequest = IndicesAliasesRequest()
            val existingAliases = get(GetIndexRequest(indexName), RequestOptions.DEFAULT).aliases

            // Remove extra aliases from the index
            existingAliases.filter { !definition.aliases.keys.contains(it.key) }
                .filter { it.key != indexName }
                .forEach {
                    log.info("Removing alias ${it.key}")
                    val action = IndicesAliasesRequest
                        .AliasActions
                        .remove()
                        .alias(it.key)
                        .index(indexName)
                    aliasesRequest.addAliasAction(action)
                }
            // Add non-existing aliases
            definition.aliases.filter { !existingAliases.keys.contains(it.key) }
                .filter { it.key != indexName }
                .forEach {
                    log.info("Adding alias ${it.key}")
                    val action = IndicesAliasesRequest
                        .AliasActions
                        .add()
                        .alias(it.key)
                        .index(indexName)
                    aliasesRequest.addAliasAction(action)
                }

            if (aliasesRequest.aliasActions.isNotEmpty()) {
                val result = updateAliases(aliasesRequest, RequestOptions.DEFAULT)
                if (!result.isAcknowledged) {
                    log.info("Update aliases not acknowledged")
                    return false
                }
            }
        }

        return true
    }

    override fun deleteResource(resource: Index): Boolean {
        MDC.put(MDCKey.RESOURCE.toString(), resource.metadata.name)

        val result = handler.handleIndexRequest(resource.spec) {
            log.info("Deleting index")

            try {
                delete(
                    DeleteIndexRequest(resource.metadata.name),
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

data class Index @JsonCreator constructor(
    @JsonProperty("spec") val spec: IndexSpec,
    @JsonProperty("status") var status: IndexStatus?
) : CustomResource()

data class IndexSpec @JsonCreator constructor(
    @JsonProperty("definition") val definition: IndexDefinition,
    @JsonProperty("hosts") override val hosts: List<String>
) : ClientSource {
    val definitionAsMap get() = mapOf(
        "settings" to definition.settings,
        "aliases" to definition.aliases,
        "mappings" to definition.mappings
    )
}

data class IndexDefinition @JsonCreator constructor(
    @JsonProperty("settings") val settings: Map<String, Any?>? = mapOf(),
    @JsonProperty("aliases") val aliases: Map<String, Any?>? = mapOf(),
    @JsonProperty("mappings") val mappings: Map<String, Any?>? = mapOf()
)

data class IndexStatus @JsonCreator constructor(
    @JsonProperty("status") var status: String,
    @JsonProperty("message") var message: String? = ""
)
