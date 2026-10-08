# Jenkins CI/CD Pipeline | Java, Maven & Docker

## Overview

A Jenkins-based CI/CD project for a Java / Spring Boot application. GitHub pushes automatically trigger a Jenkins Multibranch Pipeline that builds and tests the application, publishes a Docker image to Docker Hub, and deploys it to Amazon EC2.

After deployment, an HTTP smoke test verifies that the application is available on port `8080`.

Application repository: [jenkins-java-maven-cicd](https://github.com/Johnpaul790/jenkins-java-maven-cicd).

## Architecture

```text
Developer pushes to GitHub
        ↓
GitHub Webhook
        ↓
Jenkins Multibranch Pipeline
        ↓
Jenkins Shared Library
        ↓
Maven clean package
        ├── Compile application
        ├── Run Spring Boot integration test
        └── Package executable JAR
        ↓
Docker image build
        ↓
Docker Hub authentication
        ↓
Docker Hub push
        ↓
SSH deployment to Amazon EC2
        ↓
Pull exact versioned Docker image
        ↓
Stop and remove previous container
        ↓
Start new application container
        ↓
HTTP smoke test with retries
        ↓
Pipeline success or failure
```
A push to the application repository sends a GitHub webhook to Jenkins, which detects the updated `main` branch and starts the Multibranch Pipeline automatically.

The pipeline loads the reusable Jenkins Shared Library and executes the build, test, Docker image publication, and EC2 deployment workflow. During deployment, Jenkins authenticates to EC2 using an SSH key stored in Jenkins Credentials. After the new container starts, the deployment step performs repeated HTTP checks against `http://localhost:8080/` on the EC2 instance and fails if the application does not become available within the retry window.

## Pipeline Stages

The complete Jenkins execution includes Jenkins-generated Declarative stages, the parent `CI/CD` stage, and the nested application build and deployment stages. The pipeline runs on `agent any` and uses the Jenkins Maven installation named `maven-3.9.12`.

| Stage | Type | Description |
| --- | --- | --- |
| Declarative: Checkout SCM | Jenkins-generated | Checks out the application repository and makes the selected Git revision available in the Jenkins workspace. |
| Declarative: Tool Install | Jenkins-generated | Prepares the configured Maven installation `maven-3.9.12` for the pipeline. |
| CI/CD | Parent stage | Evaluates the path-based `when` condition and controls whether the application CI/CD workflow should run. |
| Set Application Version | Pipeline stage | Reads the application version from `pom.xml` and combines it with the Jenkins build number and short Git commit SHA to create a traceable Docker image tag. |
| Build JAR | Pipeline stage | Runs `mvn clean package`, executes the automated integration test, and packages the executable JAR. |
| Build Docker Image | Pipeline stage | Builds the Docker image using the generated versioned image name. |
| Docker Login | Pipeline stage | Authenticates to Docker Hub using credentials stored in Jenkins. |
| Push Docker Image | Pipeline stage | Pushes the versioned Docker image to Docker Hub. |
| Deploy to EC2 | Pipeline stage | Connects to EC2 over SSH, pulls the exact versioned image, replaces the running container, and verifies the application over HTTP. |

## Pipeline Execution

A push to the application repository automatically triggers the Jenkins Multibranch Pipeline through the GitHub webhook.

Changes to the application source, `pom.xml`, `Dockerfile`, or `Jenkinsfile` run the CI/CD workflow. Documentation-only changes still trigger Jenkins, but the build and deployment stages are skipped.

![Successful Jenkins pipeline](pictures/Jenkins-pipeline.png)


## Automated Testing

The Maven build includes an automated Spring Boot integration test located at:

`src/test/java/com/example/ApplicationIntegrationTest.java`

The test starts the Spring Boot application using a random available port and sends HTTP requests to the root endpoint (`/`).

It verifies that:

- the application responds with HTTP `200 OK`;
- the returned page contains `Welcome to Java Maven Application`.

The test runs automatically as part of:

```bash
mvn clean package
```

If the integration test fails, Maven returns a failed build result and Jenkins stops the pipeline before the Docker image build and push stages.

This acts as a CI quality gate, ensuring that a Docker image is only built and published when the application successfully passes its automated test.

## Jenkins Shared Library

Reusable pipeline logic is maintained in the separate [jenkins-shared-library repository](https://github.com/Johnpaul790/jenkins-shared-library).

The Jenkinsfile loads the Shared Library with:

```groovy
@Library('jenkins-shared-library') _
```

The application repository defines the high-level pipeline stages, while the Shared Library contains the reusable implementation logic.

The pipeline uses the following Shared Library steps:

- `buildJar()` — runs `mvn clean package`.
- `buildImage(env.IMAGE_NAME)` — builds the Docker image.
- `dockerLogin()` — authenticates to Docker Hub using Jenkins Credentials.
- `dockerPush(env.IMAGE_NAME)` — publishes the Docker image.
- `deployToEC2(env.IMAGE_NAME)` — connects to the EC2 deployment target over SSH, pulls the exact versioned image, replaces the application container, and verifies the deployment.

The Shared Library separates pipeline-facing functions from their implementation:

```text
jenkins-shared-library/
├── vars/
│   ├── buildJar.groovy
│   ├── buildImage.groovy
│   ├── dockerLogin.groovy
│   ├── dockerPush.groovy
│   └── deployToEC2.groovy
│
└── src/com/example/
    └── Docker.groovy
```

Files under `vars/` expose reusable Jenkins pipeline steps, while Docker and deployment logic is implemented in the `Docker` class under `src/com/example/`.

The Jenkinsfile invokes the deployment through the Shared Library step:

```groovy
deployToEC2(env.IMAGE_NAME)
```

The Shared Library then handles the underlying SSH connection, Docker image pull, container replacement, and deployment verification.

Pipeline orchestration is defined in the Jenkinsfile, while reusable implementation logic is maintained in the Shared Library.

## Repository Structure

```text
.
├── Jenkinsfile
├── Dockerfile
├── pom.xml
├── README.md
├── .gitignore
├── pictures/
│   └── Jenkins-pipeline.png
└── src/
    ├── main/
    │   ├── java/com/example/Application.java
    │   └── resources/static/index.html
    └── test/
        └── java/com/example/ApplicationIntegrationTest.java
```

## Technologies

- **CI/CD:** Jenkins, Jenkins Shared Libraries, Groovy.
- **Application and build:** Java 8, Spring Boot, Maven.
- **Containers and registry:** Docker, Amazon Corretto 8, Docker Hub.
- **Cloud and deployment:** AWS EC2, SSH.
- **Version control:** Git, GitHub.

## Docker Image

The Jenkins pipeline builds and publishes versioned Docker images using the following format:

```text
johnpaula/java-maven-app:<application-version>-<jenkins-build-number>-<git-commit>
```

For example:

```text
johnpaula/java-maven-app:1.1.1-34-4d6ce3b
```

The image tag is generated from three values:

- `1.1.1` — the application version read from `pom.xml`
- `34` — the Jenkins build number
- `4d6ce3b` — the shortened Git commit SHA

This makes each published image traceable to both the application version and the exact Jenkins build and Git revision that produced it.

Maven packages the application as:

```text
target/java-maven-app-1.1.1.jar
```

Instead of hardcoding that versioned JAR filename in the container runtime configuration, the Dockerfile copies the generated artifact to the stable name `app.jar`:

```dockerfile
COPY ./target/java-maven-app-*.jar /usr/app/app.jar
WORKDIR /usr/app
ENTRYPOINT ["java", "-jar", "app.jar"]
```

This keeps the Docker runtime configuration independent of the Maven application version. If the Maven artifact version changes later, the Dockerfile does not need to be updated just to match the new JAR filename.

The image uses `amazoncorretto:8-alpine3.17-jre` as the Java runtime and exposes port `8080`.

## Credentials and Security

Docker Hub credentials are stored in Jenkins Credentials under the ID `docker-hub-repo` and injected at runtime using `withCredentials`.

EC2 deployment uses a separate SSH credential, `ec2-deploy-key`, loaded during the deployment stage with the SSH Agent plugin. The EC2 target is provided through the Jenkins environment variable `EC2_HOST` instead of being hardcoded in the repository.

SSH host verification is enforced with `StrictHostKeyChecking=yes`, and the verified EC2 host key is stored in the Jenkins user's `known_hosts` file.

The EC2 Security Group restricts SSH access on port `22` to the Jenkins server's public IP address.

## Deployment Verification

The published Docker image was first verified manually on the EC2 instance before deployment automation was added.

The current pipeline performs automated verification after deployment. The deployment stage starts the new container and checks the application over HTTP on port `8080`.

The deployment stage retries the request while the Spring Boot application is starting. The pipeline only completes successfully when the application responds successfully.

## Implemented Features

- Automatic pipeline triggering from GitHub pushes using a repository webhook.
- Jenkins Multibranch Pipeline for branch-aware execution.
- Reusable Jenkins Shared Library for Maven, Docker, and deployment operations.
- Maven build using `mvn clean package`.
- Automated Spring Boot integration testing during the Maven build.
- Failed tests stop the pipeline before Docker image build and publication.
- Docker image creation using the packaged Spring Boot JAR.
- Docker Hub authentication using Jenkins Credentials.
- Automated Docker image publication to Docker Hub.
- Automated deployment to an Amazon EC2 instance over SSH.
- EC2 deployment authentication using an SSH private key stored in Jenkins Credentials.
- Automatic replacement of the existing application container with the exact versioned image.
- Container restart policy using `--restart unless-stopped`.
- Automated post-deployment HTTP verification with retries.


## Project Background

A hands-on DevOps project focused on Jenkins CI/CD, reusable Shared Libraries, automated Spring Boot integration testing, Maven builds, Docker image publishing, secure credential management, and automated deployment to Amazon EC2.
