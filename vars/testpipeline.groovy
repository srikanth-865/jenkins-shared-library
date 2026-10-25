def call (){
pipeline{
    agent any
    stages{
        stage('Build'){
            steps{
                echo 'Building stage'
            }
            }
        stage('Test'){
            steps{
                echo 'testing stage'
            }
        }
        stage('Deploy'){
            steps{
                echo 'deploying stage'
            }
            }
        }
    }
}


   