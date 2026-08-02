pipeline {
    agent any

    tools {
        maven 'Maven 3'
    }

    environment {
        CONTROLLER_URL = 'http://host.docker.internal:9000/api/pipelines/webhook'        
        PROJECT_ID = '1'
        ENV_NAME = 'STAGING'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build Jar') {
            steps {
                dir('Backend') {
                    sh 'mvn clean package' 
                }
            }
        }

        stage('Build & Run Docker Container') {
            steps {
                dir('Backend') {
                    sh 'docker build -t neuroforge-service .'
                }
                sh 'docker rm -f neuroforge-container || true'
                sh 'docker run -d -p 9000:9000 --name neuroforge-container neuroforge-service'
            }
        }

        stage('Notify API Controller') {
            steps {
                script {
                    sh """
                    curl -X POST ${env.CONTROLLER_URL} \\
                         -H "Content-Type: application/json" \\
                         -d "{\\"projectId\\": ${env.PROJECT_ID}, \\"status\\": \\"SUCCESS\\", \\"duration\\": 120, \\"commitHash\\": \\"${env.GIT_COMMIT}\\", \\"branch\\": \\"${env.GIT_BRANCH}\\", \\"environment\\": \\"${env.ENV_NAME}\\", \\"deploymentSuccess\\": true}"
                    """
                }
            }
        }
    }
    
    post {
        failure {
            script {
                node('') {
                    sh """
                    curl -X POST ${env.CONTROLLER_URL} \\
                         -H "Content-Type: application/json" \\
                         -d "{\\"projectId\\": ${env.PROJECT_ID}, \\"status\\": \\"FAILED\\", \\"duration\\": 120, \\"commitHash\\": \\"${env.GIT_COMMIT}\\", \\"branch\\": \\"${env.GIT_BRANCH}\\", \\"environment\\": \\"${env.ENV_NAME}\\", \\"deploymentSuccess\\": false}"
                    """
                }
            }
        }
    }
}