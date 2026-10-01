# Java Maven Application — Jenkins CI/CD

## Overview

A personal hands-on DevOps project that uses Jenkins and a reusable Jenkins Shared Library to package a Java / Spring Boot application with Maven, build a runnable Docker image, and publish it to Docker Hub. The application serves a static welcome page over HTTP on port `8080`.

Application repository: [jenkins-java-maven-cicd](https://github.com/Johnpaul790/jenkins-java-maven-cicd).

## Architecture

```text
GitHub
  ↓
Jenkins
  ↓
Jenkins Shared Library
  ↓
Maven clean package
  ↓
Docker image build
  ↓
Docker Hub authentication
  ↓
Docker Hub push
```

## Pipeline Stages

The declarative pipeline runs on `agent any` and uses the Jenkins Maven installation named `maven-3.9.12`.

| Stage | Shared-library function | Purpose |
| --- | --- | --- |
| Build JAR | `buildJar()` | Runs `mvn clean package` to produce an executable Spring Boot JAR. |
| Build Docker Image | `buildImage(env.IMAGE_NAME)` | Builds the application image using the repository's Dockerfile. |
| Docker Login | `dockerLogin()` | Authenticates to Docker Hub using Jenkins Credentials. |
| Push Docker Image | `dockerPush(env.IMAGE_NAME)` | Publishes the image to Docker Hub. |

![Jenkins pipeline](pictures/Jenkins-pipeline.png)

## Jenkins Shared Library

Reusable CI/CD logic is maintained in the separate [jenkins-shared-library repository](https://github.com/Johnpaul790/jenkins-shared-library). The Jenkinsfile loads it with:

```groovy
@Library('jenkins-shared-library') _
```

The application repository defines the pipeline stages and image name; the shared library implements the build, authentication, and publishing functions listed above.

## Repository Structure

```text
.
├── Jenkinsfile                         # Declarative pipeline and shared-library integration
├── Dockerfile                          # Java runtime image and JAR entrypoint
├── pom.xml                             # Maven dependencies and executable JAR packaging
├── README.md
├── .gitignore
├── pictures/
│   └── Jenkins-pipeline.png            # Jenkins pipeline screenshot
└── src/main/
    ├── java/com/example/Application.java
    └── resources/static/index.html
```

## Technologies

- **Automation:** Jenkins, Jenkins Shared Libraries, Groovy.
- **Application and build:** Java 8, Spring Boot, Maven.
- **Containers and registry:** Docker, Amazon Corretto 8 runtime, Docker Hub.
- **Version control and hosting:** Git, GitHub.

## Docker Image

The pipeline sets `IMAGE_NAME` to `johnpaula/demo-app-2.2`; the published image uses the default `latest` tag: [`johnpaula/demo-app-2.2:latest`](https://hub.docker.com/r/johnpaula/demo-app-2.2). The `2.2` is part of the image repository name.

Maven packages `com.example:java-maven-app:1.1.1` as `target/java-maven-app-1.1.1.jar`. The Dockerfile uses `amazoncorretto:8-alpine3.17-jre`, exposes port `8080`, and copies the generated JAR to a stable filename:

```dockerfile
COPY ./target/java-maven-app-*.jar /usr/app/app.jar
WORKDIR /usr/app
ENTRYPOINT ["java", "-jar", "app.jar"]
```

## Credentials and Security

Docker Hub authentication uses Jenkins Credentials with credential ID `docker-hub-repo`. Secrets are managed in Jenkins and are not stored in this repository.

## Runtime Verification

The resulting Docker image was manually run as a container. The Spring Boot application started successfully and responded over HTTP. This verification was performed manually and is not an automated pipeline stage.

## Current Scope

**Implemented:**

- GitHub checkout through the Jenkins SCM workflow.
- Maven clean packaging.
- Jenkins Shared Library integration.
- Docker image build.
- Jenkins credential-based Docker Hub authentication.
- Docker image publishing.
- Manual runtime verification.

**Not yet implemented:**

- Automated application tests: no test sources are present; Maven reports "No tests to run."
- Automated deployment: the pipeline builds and publishes a runnable Docker image but does not deploy it to a runtime environment.

## Project Background

This project was implemented as a hands-on DevOps project during technical training and further developed to practice Jenkins pipelines, reusable Shared Libraries, Maven builds, Docker workflows, credential management, and GitHub-based CI/CD.
