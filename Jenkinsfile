pipeline {
    agent any

    tools {
        maven 'M3' 
    }
    
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
                    // Changed from mvn to mvnw.cmd
                    bat 'mvnw.cmd clean package -DskipTests'
                }
            }
        }
        
        stage('Docker Build & Deploy') {
            steps {
                dir('Backend') {
                    bat 'docker build -t payment-service .'
                    bat 'docker rm -f payment-service || cmd /c "exit 0"'
                    bat 'docker run -d -p 8081:9000 --name payment-service -e KEYCLOAK_JWK_SET_URI=http://host.docker.internal:8080/realms/neuroforge-nexus/protocol/openid-connect/certs -e KEYCLOAK_ISSUER=http://host.docker.internal:8080/realms/neuroforge-nexus -e KAFKA_BOOTSTRAP_SERVERS=host.docker.internal:9092 -e DB_URL=jdbc:postgresql://host.docker.internal:5432/neuroforge_nexus -e DB_USERNAME=postgres -e DB_PASSWORD=student123 payment-service'
                }
            }
        }
        
        stage('Track Deployment') {
            steps {
                sleep time: 45, unit: 'SECONDS'
                bat "curl -X POST http://localhost:8081/api/track-deployment -H \"Content-Type: application/json\" -d \"{\\\"build_id\\\": \\\"${BUILD_NUMBER}\\\", \\\"status\\\": \\\"SUCCESS\\\"}\""
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