
def call() {
    pipeline {
        agent {
            label 'ROBOSHOP'
        }

        stages {
            stage('Build') {
                steps {
                    echo 'Building stage'
                }
            }

            stage('Test') {
                steps {
                    echo 'Testing stage'
                }
            }

            stage('Deploy') {
                steps {
                    echo 'Deploying stage'
                }
            }
        }
    }
}
