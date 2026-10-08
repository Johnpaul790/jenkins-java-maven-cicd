@Library('jenkins-shared-library') _

pipeline {
    agent any

    tools {
        maven 'maven-3.9.12'
    }

    environment {
        IMAGE_REPOSITORY = 'johnpaula/java-maven-app'
    }

    stages {
        stage('CI/CD') {
            when {
                anyOf {
                    changeset 'src/**'
                    changeset 'pom.xml'
                    changeset 'Dockerfile'
                    changeset 'Jenkinsfile'
                }
            }

            stages {
                stage('Set Application Version') {
                    steps {
                        script {
                            def version = sh(
                                script: 'mvn help:evaluate -Dexpression=project.version -q -DforceStdout',
                                returnStdout: true
                            ).trim()

                            def shortCommit = env.GIT_COMMIT.take(7)

                            env.IMAGE_NAME = "${env.IMAGE_REPOSITORY}:${version}-${env.BUILD_NUMBER}-${shortCommit}"

                            echo "Application version: ${version}"
                            echo "Docker image: ${env.IMAGE_NAME}"
                        }
                    }
                }

                stage('Build JAR') {
                    steps {
                        buildJar()
                    }
                }

                stage('Build Docker Image') {
                    steps {
                        buildImage(env.IMAGE_NAME)
                    }
                }

                stage('Docker Login') {
                    steps {
                        dockerLogin()
                    }
                }

                stage('Push Docker Image') {
                    steps {
                        dockerPush(env.IMAGE_NAME)
                    }
                }

                stage('Deploy to EC2') {
                    steps {
                        deployToEC2(env.IMAGE_NAME)
                    }
                }
            }
        }
    }
}
