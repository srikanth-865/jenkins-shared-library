def call (){
pipeline{
    agent any
    stages{
        stage{
            steps('Build'){
                echo 'Building stage'
            }
            }
        stage{
            steps('test'){
                echo 'testing stage'
            }
        }
        stage{
            steps('deploy'){
                echo 'deploying stage'
            }
            }
        }
    }
}


   