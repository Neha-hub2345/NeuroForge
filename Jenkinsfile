pipeline {
    agent any

    tools {
        maven 'Maven 3'
    }

    environment {
        // Change this from the ngrok URL to localhost
        CONTROLLER_URL = 'http://localhost:9000/api/pipelines/webhook'
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
                    bat 'mvn clean package -DskipTests'
                }
            }
        }

        stage('Build & Run Docker Container') {
            steps {
                dir('Backend') {
                    bat 'docker build -t neuroforge-service .'
                }
                bat 'docker rm -f neuroforge-container || true'
                bat 'docker run -d -p 9000:9000 --name neuroforge-container neuroforge-service'
            }
        }

        stage('Notify API Controller') {
            steps {
                script {
                    bat """
                    curl -X POST ${env.CONTROLLER_URL} ^
                         -H "Content-Type: application/json" ^
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
                    bat """
                    curl -X POST ${env.CONTROLLER_URL} ^
                         -H "Content-Type: application/json" ^
                         -d "{\\"projectId\\": ${env.PROJECT_ID}, \\"status\\": \\"FAILED\\", \\"duration\\": 120, \\"commitHash\\": \\"${env.GIT_COMMIT}\\", \\"branch\\": \\"${env.GIT_BRANCH}\\", \\"environment\\": \\"${env.ENV_NAME}\\", \\"deploymentSuccess\\": false}"
                    """
                }
            }
        }
    }
}