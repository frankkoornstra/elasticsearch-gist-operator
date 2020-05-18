# set all to phony
SHELL=bash

.PHONY: *

mkfile_path := $(abspath $(lastword $(MAKEFILE_LIST)))
current_dir := $(abspath $(patsubst %/,%,$(dir $(mkfile_path))))

up: minikube definitionUpdate environment # Your one stop shop to start the project

minikube:
	minikube start

reset: # Forcefully removes all and any traces of existing custom resources
	kubectl delete elasticsearch-indices.frankkoornstra.nl,elasticsearch-templates.frankkoornstra.nl --all --grace-period=0 --force || true

definitionUpdate: # Updates the custom resource definitions
	for f in ${current_dir}/crd/crd-*.yaml; do kubectl apply -f $${f}; done

templateUpdate: # Updates the example template custom resource
	kubectl apply -f ${current_dir}/crd/template.yaml

templateDelete: # Deletes the example template custom resource
	kubectl delete -f ${current_dir}/crd/template.yaml || true

indexUpdate: # Updates the example index custom resource
	kubectl apply -f ${current_dir}/crd/index.yaml

indexDelete: # Deletes the example index custom resource
	kubectl delete -f ${current_dir}/crd/index.yaml || true

environment: # Creates the Elasticsearch cluster and tooling around it
	docker-compose up -d

dockerizeJar: # Creates the Docker runtime image
	docker build --target=runtime -t elasticsearch-gist-operator .

dockerizeTest: # Creates the Docker test image
	docker build --target=test -t elasticsearch-gist-operator .
