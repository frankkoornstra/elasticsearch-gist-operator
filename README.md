# Elasticsearch Gist Operator

[![Current build status](https://github.com/frankkoornstra/elasticsearch-gist-operator/workflows/Build%20and%20test%20code/badge.svg)](https://github.com/frankkoornstra/elasticsearch-gist-operator/actions?query=branch%3Amaster)
[![codecov](https://codecov.io/gh/frankkoornstra/elasticsearch-gist-operator/branch/master/graph/badge.svg)](https://codecov.io/gh/frankkoornstra/elasticsearch-gist-operator)
[![Latest relese](https://img.shields.io/github/v/release/frankkoornstra/elasticsearch-gist-operator?include_prereleases)](https://github.com/frankkoornstra/elasticsearch-gist-operator/releases)
[![Commit activity](https://img.shields.io/github/commit-activity/m/frankkoornstra/elasticsearch-gist-operator)](https://github.com/frankkoornstra/elasticsearch-gist-operator/pulse)
[![Last commit](https://img.shields.io/github/last-commit/frankkoornstra/elasticsearch-gist-operator)](https://github.com/frankkoornstra/elasticsearch-gist-operator/commits/master)

Operator for Kubernetes that will take care of the [gist](https://www.dictionary.com/browse/gist) of your Elasticsearch cluster, managing your:
* Templates
* Indices

This operator is the glue between the state of your Elasticsearch cluster and Kubernetes.
By using Elasticsearch's excellent [Java client](https://www.elastic.co/guide/en/elasticsearch/client/java-rest/current/index.html), it wil try to create, update and delete the custom resources defined in [this directory](crd/).

## Run it

**Always make sure only 1 instance of the Operator is running at the same time to prevent concurrency issues**

The Docker image for this repository is available at `docker.pkg.github.com/frankkoornstra/elasticsearch-gist-operator/elasticsearch-gist-operator:{version tag}`. To use Github's Docker repository, you will [need to authenticate](https://help.github.com/en/packages/using-github-packages-with-your-projects-ecosystem/configuring-docker-for-use-with-github-packages#authenticating-to-github-packages).

You'll probably want to deploy the Operator inside Kubernetes. If you want to see a simplistic example of how to do that, clone this repository and - _only when connected to your development cluster!_ - run:
```bash
cd kubernetes-resources
make GITHUB_USERNAME=<your github username> GITHUB_TOKEN=<github token>
```

It will:
1. Create a secret, needed to pull the docker image from Github's Docker repository. You'll need to [create a token](https://github.com/settings/tokens) with `read:packages` capabilities to do this.
1. Update the CRDs that the operator defines
1. Create a deployment for the Operator

By using the [`Recreate`](https://kubernetes.io/docs/concepts/workloads/controllers/deployment/#recreate-deployment) deployment strategy in Kubernetes, you make sure only one instance of the Operator is running at the same time.

## Compatibility and versioning

The Operator is semantically versioned. It obviously has a huge dependency on Elasticsearch so versioning of the Operator is tightly bound to the Elasticsearch version.

Any bumps in parts of the Elasticsearch library version will always result in a bump in the same part of that version in the Operator; ie a major version bump in the Elasticsearch library results in a major version bump of the Operator version, same with minor and patch.

Bumps in the Operator major, minor and patch versions can still happen independently so multiple major versions of the operator might be compatible with the same major Elatiscsearch version (although we'll do our best to avoid this).

In the table below you can find the compatibility between versions of this operator and Elasticsearch.

| Operator    | Elasticsearch |
|-------------|---------------|
| 0.1 (alpha) | 6.8           |

## Resources

The Operator controls the following resources:
* Templates
* Indices

### Templates

Usually one of the first steps is to setup [templates](https://www.elastic.co/guide/en/elasticsearch/reference/6.8/indices-templates.html#indices-templates).
They contain mappings and settings as well as a pattern that can match to-be created indices.
Once an index gets created that matches one or more template patterns, all the contents of those templates will be applied to the new index.

You can create and update templates with this operator by sticking to [the definition of the custom resource](crd/crd-template.yaml), an example is [also availabe](crd/template.yaml)

### Indices

The bread and butter of Elasticsearch; this will contain all your documents.
Indices have three core components: settings, a mapping and aliases.
All three of them can be managed by this operator, although there are some things to take into consideration, outlined below.

You can create and update indices with this operator by sticking to [the definition of the custom resource](crd/crd-index.yaml), an example is [also availabe](crd/index.yaml)

#### Settings

Settings [come in two kinds](https://www.elastic.co/guide/en/elasticsearch/reference/current/index-modules.html#index-modules-settings): static or dynamic.

When you _create_ an index, all settings in your CRD will be applied.
When you _update_ and index, only the dynamic settings will be applied.

#### Mapping

Mappings adhere to the concept of backwards compatibility (BC), for details browse the Elasticsearch documentation on what is and is not BC.

Whenever you update a mapping in an index in a non-BC manner, the update will fail.
This will be reflected by a `FAILED` status object in the CRD, the reason why it failed will also be in the status object.

#### Aliases

Simple: it'll remove existing aliases that aren't in the CRD anymore, it will ad the ones that don't exist yet.

*This operator does not support filters on aliases (yet, PRs welcome)*

## Roadmap

- [x] Upsert/delete templates
- [x] Upsert/delete indices
- [x] Upsert index aliases (without filters or routing)
- [x] Integration tests with Elasticsearch
- [x] Github Actions to run tests for PRs
- [x] Release process
- [ ] Status subresource
