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
                    sh 'mvn clean package -DskipTests'
                }
            }
        }
        
        stage('Docker Build & Deploy') {
            steps {
                dir('Backend') {
                    sh 'docker build -t ${DOCKER_IMAGE} .'
                    sh 'docker rm -f ${DOCKER_IMAGE} || true'
                    sh 'docker run -d -p ${PORT}:${PORT} --name ${DOCKER_IMAGE} ${DOCKER_IMAGE}'
                }
            }
        }
        
        stage('Track Deployment') {
            steps {
                sh '''
                curl -X POST http://localhost:8081/api/track-deployment \
                -H "Content-Type: application/json" \
                -d '{"build_id": "'${BUILD_NUMBER}'", "status": "SUCCESS"}'
                '''
            }
        }
    }
    
    post {
        failure {
            sh 'bash rollback.sh'
        }
    }
}