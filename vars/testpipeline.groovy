def call(Map configMap) {
    pipeline {
         agent {
            node {
              label "ROBOSHOP"
               }
         }
        environment{
            project = configMap.get("project")
            component = configMap.get("component")
        }
        stages {
            stage('Build') {
                steps {
                    echo 'Building stage'
                    echo "project: is ${project} & component: is ${component}"
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
