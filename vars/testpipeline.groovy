def call (){
pipeline{
    agent {
        label ROBOSHOP
    }
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


   