# set all to phony
SHELL=bash

.PHONY: *

mkfile_path := $(abspath $(lastword $(MAKEFILE_LIST)))
current_dir := $(abspath $(patsubst %/,%,$(dir $(mkfile_path))))

install: upsert-github-credentials-secret update-crds deploy

upsert-github-credentials-secret: # Creates a secret inside Kubernetes to be able to pull the operator image
	kubectl get secret github-docker-secret > /dev/null || $(MAKE) create-github-credentials-secret

GITHUB_USERNAME?=$(error GITHUB_USERNAME variable must be set)
GITHUB_TOKEN?=$(error GITHUB_TOKEN variable must be set. The token must have read:packages capabilities)
create-github-credentials-secret: #--namespace=kube-system
	@kubectl create secret docker-registry github-docker-secret --docker-server=docker.pkg.github.com --docker-username=${GITHUB_USERNAME} --docker-password=${GITHUB_TOKEN}

update-crds: # Updates the Custom Resource Definitions of the operator
	for f in ${current_dir}/crd/crd-*.yaml; do kubectl apply -f $${f}; done

deploy: # Deploys the operator
	kubectl apply -f ${current_dir}/deployment.yaml
