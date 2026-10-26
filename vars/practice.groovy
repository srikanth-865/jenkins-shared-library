def call(){
pipeline{
    agent{
        node{
            label 'ROBOSHOP'
        }
    }
    stages{
        stage('Build stage'){
          steps{
            script{
                sh"""
                   echo "i am building stage"
                """
               }
          }
    }
        stage('scan stage'){
          steps{
            script{
                sh"""
                   echo "i am scanninng stage"
                """
               }
          }

    }
}
}
}