//The shared library contains the actual pipeline implementation. The main Jenkinsfile just calls it. by this file name ..practice()
def call(configMap){
pipeline{
    agent{
        node{
            label 'ROBOSHOP'
        }
    }
    environment{
        name = configMap.get("name")
        place = configMap.get("place")

    }
    stages{
        stage('Build stage'){
          steps{
            script{
                sh"""
                   echo "i am building stage"
                   echo "My name is : ${name}"
                """
               }
          }
    }
        stage('scan stage'){
          steps{
            script{
                sh"""
                   echo "i am scanninng stage"
                   echo "My place is : ${place}"
                """
               }
          }

    }
}
}
}