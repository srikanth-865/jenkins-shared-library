def call (){
pipeline{
    agent {
         node {
            label 'ROBOSHOP' //we gave in jenkins UI for to match label for this name to run this pipeline
        }
    }
    stages{
        stage{
            steps('Build')
                echo "Building stage"
            }
        stage{
            steps('test')
                echo "testing stage"
            }
            stage{
            steps('deploy')
                echo "deploying stage"
            }
        }
    }
}


   