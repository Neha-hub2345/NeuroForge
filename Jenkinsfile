pipeline {
    agent any
    
    environment {
        DOCKER_IMAGE = 'payment-service'
        PORT = '8081'
    }
    
    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        
        stage('Build JAR') {
            steps {
                dir('Backend') {
                    bat 'mvn clean package -DskipTests'
                }
            }
        }
        
        stage('Docker Build & Deploy') {
            steps {
                dir('Backend') {
                    bat "docker build -t ${DOCKER_IMAGE} ."
                    bat "docker rm -f ${DOCKER_IMAGE} || cmd /c \"exit 0\""
                    bat "docker run -d -p ${PORT}:${PORT} --name ${DOCKER_IMAGE} ${DOCKER_IMAGE}"
                }
            }
        }
        
        stage('Track Deployment') {
            steps {
                // Windows substitute for sleep 15
                bat 'timeout /t 15 /nobreak'
                
                // Escaped double quotes required for Windows CMD JSON payloads
                bat "curl -X POST http://localhost:${PORT}/api/track-deployment -H \"Content-Type: application/json\" -d \"{\\\"build_id\\\": \\\"${BUILD_NUMBER}\\\", \\\"status\\\": \\\"SUCCESS\\\"}\""
            }
        }
    }
    
    post {
        failure {
            // Rollback commands executed directly in Windows CMD
            bat "docker stop ${DOCKER_IMAGE} || cmd /c \"exit 0\""
            bat "docker rm ${DOCKER_IMAGE} || cmd /c \"exit 0\""
        }
    }
}