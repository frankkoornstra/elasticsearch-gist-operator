# Elasticsearch Gist Operator

Operator for Kubernetes that will take care of the [gist](https://www.dictionary.com/browse/gist) of your Elasticsearch cluster, managing your:
* Templates
* Indices

This operator is the glue between the state of your Elasticsearch cluster and Kubernetes.
By using Elasticsearch's excellent [Java client](https://www.elastic.co/guide/en/elasticsearch/client/java-rest/current/index.html), it wil try to create, update and delete the custom resources defined in [this directory](crd/).

**The operator's alpha release will only be compatible with Elasticsearch 6.8.**

## Templates

Usually one of the first steps is to setup [templates](https://www.elastic.co/guide/en/elasticsearch/reference/6.8/indices-templates.html#indices-templates).
They contain mappings and settings as well as a pattern that can match to-be created indices.
Once an index gets created that matches one or more template patterns, all the contents of those templates will be applied to the new index.

You can create templates with this operator by sticking to [the definition of the custom resource](crd/crd-template.yaml), an example is [also availabe](crd/template.yaml)

## Indices

The bread and butter of Elasticsearch; this will contain all your documents.
Since the first iterations don't allow you to provide any (overriding) mappings or settings, you will need to create templates first to control those.

## Roadmap

- [x] Upsert/delete templates
- [x] Upsert/delete indices
- [x] Upsert index aliases (without filters or routing)
- [ ] Integration tests with Elasticsearch
- [ ] Status subresource
- [ ] Github Actions to run tests for PRs
