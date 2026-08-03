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
                
                sh '''
                for i in {1..15}; do
                    if curl -s http://host.docker.internal:9000/ > /dev/null; then
                        exit 0
                    fi
                    sleep 5
                done
                exit 1
                '''
            }
        }

        stage('Notify API Controller') {
            steps {
                script {
                    def successPayload = """{"projectId": ${env.PROJECT_ID}, "status": "SUCCESS", "duration": 120, "commitHash": "${env.GIT_COMMIT}", "branch": "origin/main", "environment": "${env.ENV_NAME}", "deploymentSuccess": true}"""
                    writeFile file: 'success_payload.json', text: successPayload
                    sh "curl -X POST ${env.CONTROLLER_URL} -H 'Content-Type: application/json' -d @success_payload.json"
                }
            }
        }
    }
    
    post {
        failure {
            script {
                def failurePayload = """{"projectId": ${env.PROJECT_ID}, "status": "FAILED", "duration": 120, "commitHash": "${env.GIT_COMMIT}", "branch": "origin/main", "environment": "${env.ENV_NAME}", "deploymentSuccess": false}"""
                writeFile file: 'failure_payload.json', text: failurePayload
                sh "curl -X POST ${env.CONTROLLER_URL} -H 'Content-Type: application/json' -d @failure_payload.json"
            }
        }
    }
}
