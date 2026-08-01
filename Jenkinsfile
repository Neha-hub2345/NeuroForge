pipeline {
    agent any

    environment {
        API_TOKEN = credentials('pipeline-controller-token')
        CONTROLLER_URL = 'http://https://uneven-greedy-vendetta.ngrok-free.dev/api/pipelines/webhook'
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
                sh 'mvn clean package -DskipTests'
            }
        }

        stage('Notify API Controller') {
            steps {
                script {
                    sh """
                    curl -X POST ${CONTROLLER_URL} \
                         -H 'Content-Type: application/json' \
                         -H "Authorization: Bearer ${API_TOKEN}" \
                         -d '{
                             "projectId": ${PROJECT_ID},
                             "status": "SUCCESS",
                             "duration": 120,
                             "commitHash": "${env.GIT_COMMIT}",
                             "branch": "${env.GIT_BRANCH}",
                             "environment": "${ENV_NAME}",
                             "deploymentSuccess": true
                         }'
                    """
                }
            }
        }
    }
    
    post {
        failure {
            script {
                sh """
                curl -X POST ${CONTROLLER_URL} \
                     -H 'Content-Type: application/json' \
                     -H "Authorization: Bearer ${API_TOKEN}" \
                     -d '{
                         "projectId": ${PROJECT_ID},
                         "status": "FAILED",
                         "duration": 120,
                         "commitHash": "${env.GIT_COMMIT}",
                         "branch": "${env.GIT_BRANCH}",
                         "environment": "${ENV_NAME}",
                         "deploymentSuccess": false
                     }'
                """
            }
        }
    }
}