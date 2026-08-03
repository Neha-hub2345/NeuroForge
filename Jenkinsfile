pipeline {
    agent any

    tools {
        maven 'Maven 3'
    }

    environment {
    CONTROLLER_URL = 'http://host.docker.internal:9000/api/pipelines/webhook'
    PROJECT_ID = '1' // Change from '1' to a valid ID
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
                    sh 'mvn clean package -DskipTests' 
                }
            }
        }

        stage('Build & Run Docker Container') {
            steps {
                dir('Backend') {
                    sh 'docker build -t neuroforge-service .'
                }
                sh 'docker rm -f neuroforge-container || true'
                sh 'docker run -d -p 9000:9000 --network neuroforge_default --name neuroforge-container neuroforge-service'
                
                // Add a small pause to let Spring Boot fully boot up and connect to Postgres
                sh 'sleep 25'
            }
        }

        stage('Notify API Controller') {
            steps {
                script {
                    sh '''
                        curl -X POST http://host.docker.internal:9000/api/pipelines/webhook \
                        -H "Content-Type: application/json" \
                        -d '{"projectId": 1, "status": "SUCCESS", "duration": 120, "commitHash": "'"${env.GIT_COMMIT}"'", "branch": "origin/main", "environment": "STAGING", "deploymentSuccess": true}'
                    '''
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