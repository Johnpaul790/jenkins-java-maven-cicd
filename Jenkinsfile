@Library('jenkins-shared-library') _

pipeline {
    agent any

    tools {
        maven 'maven-3.9.12'
    }

    environment {
        IMAGE_NAME = 'johnpaula/demo-app-2.2'
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
