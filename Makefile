# set all to phony
SHELL=bash

.PHONY: *

mkfile_path := $(abspath $(lastword $(MAKEFILE_LIST)))
current_dir := $(abspath $(patsubst %/,%,$(dir $(mkfile_path))))

reset:
	kubectl delete daemonsets,replicasets,services,deployments,pods,rc,ingresses,elasticsearch-indices.frankkoornstra.nl,elasticsearch-templates.frankkoornstra.nl --all --grace-period=0 --force || true

definitionUpdate:
	kubectl apply -f ${current_dir}/crd/crd-template.yaml
	kubectl apply -f ${current_dir}/crd/crd-index.yaml

templateUpdate:
	kubectl apply -f ${current_dir}/crd/template.yaml

templateDelete:
	kubectl delete -f ${current_dir}/crd/template.yaml || true

indexUpdate:
	kubectl apply -f ${current_dir}/crd/index.yaml

indexDelete:
	kubectl delete -f ${current_dir}/crd/index.yaml || true

watch:
	kubectl get pods --watch

up:
	docker-compose up -d

dockerizeJar:
	docker build --target=runtime -t elasticsearch-gist-operator .

dockerizeTest:
	docker build --target=test -t elasticsearch-gist-operator .
