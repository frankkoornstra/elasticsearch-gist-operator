# Contributing

:tada: Thank you for taking the time to contribute to this project! :tada:

There's a few things to keep in mind, outlined below, but first and foremost: use your common sense

![Gordon Ramsey saying: I can't teach you that, 'cause that's common sense](https://media2.giphy.com/media/5z83tLvEy5iXH25uJP/source.gif)

## Glossary

| Term | Description                |
|------|----------------------------|
| CR   | Custom Resource            |
| CRD  | Custom Resource Definition |

## Issues

After you used your common sense to look through [the existing issues](https://github.com/frankkoornstra/elasticsearch-gist-operator/issues) to see if it's not already reported, you can use and fill in the appropriate issue template.

Make sure your issue gets the attention it deserves: issues with missing information may be ignored or punted back to you, delaying a fix. Try to be as explicit and elaborate as you can so someone who picks it up can immediately get to work.

## Pull requests

You're going to help develop this project, that's amazing!

![crowd cheering](https://media1.giphy.com/media/srg19CG0cKMuI/200.gif)

Let's talk about the framework this project is based on, how to get your development environment up and running, how to control the CRs and CRDS, touch upon use of Docker, and finally some general coding guidelines.

### The framework

This project is based on [Container Solution's Java Operator SDK](https://github.com/ContainerSolutions/java-operator-sdk). It's a framework for Kubernetes operators that lets this project only deal with the actual business logic of handling [the CRs](https://kubernetes.io/docs/concepts/extend-kubernetes/api-extension/custom-resources/). Basically that means creating CRDs and handling CR changes (inserts, updates, deletions) based on the state of the Elasticsearch cluster.

### Development environment

You can use whatever Kubernetes cluster you can get your hands on to run this operator in, but it's recommended to set up [Minikube](https://kubernetes.io/docs/setup/learning-environment/minikube/), a local Kubernetes cluster, so you can make all the mistakes we know are eventually inevitable.

To make Elasticseach available in the development environment, [Docker compose](https://docs.docker.com/compose/) is used. You don't _need_ it if you have another cluster that you can use, but again: it's probably a safe bet to use a local running cluster so the blast radius of the inevitable mistakes isn't too big.

Along with this project comes a [Dockerfile](/Dockerfile) and [Gradle build file](/build.gradle.kts) that tell you exactly what Java version is targeted. Any decent IDE (like IntelliJ's IDEA) will pick up this version from the Gradle file, so you shouldn't have to worry about that.

Once you've got Minikube and Docker compose installed, simply run
```bash
make
```

That will:
1. Start your Minkube Kubernetes cluster
1. Update the CRDs in the cluster
1. Start your Docker compose environment which contains Elasticsearch

Now you're ready to run the Operator in whatever IDE you like by running the `nl.frankkoornstra.elasticsearchgistoperator.Operator.main` function. It will automatically pick whatever it needs to access the Kubernetes API inside Minikube.

### Custom Resources and Kubernetes

Now that you have your Minikube provisioned with the CRDs, you might want to change a thing or two about them. All CRDs can be found in the [`/crd` directory](/crd), along with their example CRs.

As soon as you change something about a CRD (and the classes they deserialize into), you need to notify Kubernetes of that change. Simply run the following to update all CRDs
```bash
make definitionUpdate
```

Creating, updating or deleting an actual CR can be done with `kubectl`
```bash
kubectl apply -f path/to/cr.yaml
```

Or to update the example CRs
```bash
make templateUpdate
make templateDelete
make indexUpdate
make indexDelete
```

### Docker

This project uses Docker multi stage builds to create its artifacts. You can create both the runtime (`make dockerizeJar`) and the image to run tests in (`make dockerizeTest`). These are the exact images that will be distributed and used by Github Action's CI to run tests in.

### Coding guidelines

It's very simple: if it passes the CI, you're good :smirk:

The project applies the following tools and guidelines:
* **KTLint**: ensures everyone sticks to the Kotlin guidelines. If you autoformat your code with IDEs like IntelliJ IDEA, that's already enough to pass the bar.
* **JaCoCo**: 100% code coverage with unit tests for everything that has behavior. Exceptions should be few and far between. Sometimes it's an exercise in futility, humor me :kissing_heart:
* **Integration tests**: things that touch Elasticsearch can't be properly tested with unit tests. That leaves it up to integration tests to ensure the sought after results are achieved. Please create them if you create behavior that influences Elaticsearch.
