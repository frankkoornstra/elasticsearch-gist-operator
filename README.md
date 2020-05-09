# Elasticsearch Gist Operator

![Current build status](https://github.com/frankkoornstra/elasticsearch-gist-operator/workflows/Build%20and%20test%20code/badge.svg)
![Latest relese](https://img.shields.io/github/v/release/frankkoornstra/elasticsearch-gist-operator)
![Commit activity](https://img.shields.io/github/commit-activity/m/frankkoornstra/elasticsearch-gist-operator)
![Last commit](https://img.shields.io/github/last-commit/frankkoornstra/elasticsearch-gist-operator)

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

You can create and update templates with this operator by sticking to [the definition of the custom resource](crd/crd-template.yaml), an example is [also availabe](crd/template.yaml)

## Indices

The bread and butter of Elasticsearch; this will contain all your documents.
Indices have three core components: settings, a mapping and aliases.
All three of them can be managed by this operator, although there are some things to take into consideration, outlined below.

You can create and update indices with this operator by sticking to [the definition of the custom resource](crd/crd-index.yaml), an example is [also availabe](crd/index.yaml)

### Settings

Settings [come in two kinds](https://www.elastic.co/guide/en/elasticsearch/reference/current/index-modules.html#index-modules-settings): static or dynamic.

When you _create_ an index, all settings in your CRD will be applied.
When you _update_ and index, only the dynamic settings will be applied.

### Mapping

Mappings adhere to the concept of backwards compatibility (BC), for details browse the Elasticsearch documentation on what is and is not BC.

Whenever you update a mapping in an index in a non-BC manner, the update will fail.
This will be reflected by a `FAILED` status object in the CRD, the reason why it failed will also be in the status object.

### Aliases

Simple: it'll remove existing aliases that aren't in the CRD anymore, it will ad the ones that don't exist yet.

*This operator does not support filters on aliases (yet, PRs welcome)*

## Roadmap

- [x] Upsert/delete templates
- [x] Upsert/delete indices
- [x] Upsert index aliases (without filters or routing)
- [x] Integration tests with Elasticsearch
- [ ] Status subresource
- [ ] Github Actions to run tests for PRs
