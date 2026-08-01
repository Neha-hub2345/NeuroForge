pipeline {
    agent any

    environment {
        CONTROLLER_URL = 'https://uneven-greedy-vendetta.ngrok-free.dev/api/pipelines/webhook'
        PROJECT_ID = '1'
        ENV_NAME = 'STAGING'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build') {
            steps {
                // Using mvn.cmd to ensure Windows command prompt finds it
                bat 'mvn.cmd clean package -DskipTests'
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