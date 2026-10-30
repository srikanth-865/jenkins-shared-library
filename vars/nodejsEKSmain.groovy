def call (Map configMap){
    pipeline{
      agent {
        node {
            label "ROBOSHOP"   // we gave in jenkins UI for to match label for this name to run this pipeline.
         }
      }
        environment{
            def appversion = ""   // variable globally to access wvwerystage
            acc_id = "349627593718"
            project =  configMap.get("project")
            component =  configMap.get("component")
           }


    stages{
         stage( 'Read Version'){   //stage 1
            steps{
                script{
                    
                      def  packageJson = readJSON file: 'package.json'
                      appversion =  packageJson.version
                      echo "The application version is: ${appversion}"

                }
            }
        }
       stage('Dev-Deploy') {    //stage1
                steps {
                    script {
                        try {
                            withAWS(credentials: 'aws-creds', region: 'us-east-1') {
                                sh """
                                    aws eks update-kubeconfig --name roboshop --region us-east-1
                                    cd helm
                                    helm upgrade --install ${component} -f values-dev.yaml -n  roboshop-dev \
                                        --set deployment.imageVersion=${appversion} \
                                        --wait --timeout 5m .

                                    kubectl rollout status deployment/${component} -n roboshop-dev --timeout=120s
                                """
                            }
                            utils.updateCommitStatus('success', 'Deployed to roboshop-dev', 'dev-deploy')
                        }
                        catch (Exception e) {
                            utils.updateCommitStatus('failure', 'Deploy to roboshop-dev failed', 'dev-deploy')
                            throw e
                        }
                    }
                }
            }
        stage('api-tests') {   //stage 2
                steps {
                    script {
                        try {
                            build job: 'ROBOSHOP/catalogue-api-tests', parameters: [
                                string(name: 'NAMESPACE', value: 'roboshop-dev'),
                                string(name: 'COMMIT_ID', value: env.GIT_COMMIT)
                            ], wait: true, propagate: true //considering upstreamm errors are also downstream errors
                            utils.updateCommitStatus('success', 'catalogue-api-tests passed', 'api-tests')
                        }
                        catch (Exception e) {
                            utils.updateCommitStatus('failure', 'catalogue-api-tests failed', 'api-tests')
                            throw e
                        }
                    }
                }
        }

      stage("promote-image"){
        steps{
            script{
                 withAWS(credentials: 'aws-creds', region: 'us-east-1') {
                    sh """
                        docker pull ${acc_id}.dkr.ecr.us-east-1.amazonaws.com/${project}/${component}:${appversion}

                        docker tag ${acc_id}.dkr.ecr.us-east-1.amazonaws.com/${project}/${component}:${appversion} ${acc_id}.dkr.ecr.us-east-1.amazonaws.com/${project}/${component}:${env.GIT_COMMIT}

                        docker push ${acc_id}.dkr.ecr.us-east-1.amazonaws.com/${project}/${component}:${env.GIT_COMMIT}
                    """
              }
          }
        }
        }
        stage("sit-Deploy"){
          steps {
                    script {
                        try {
                            withAWS(credentials: 'aws-creds', region: 'us-east-1') {
                                sh """
                                    aws eks update-kubeconfig --name roboshop --region us-east-1

                                    helm upgrade --install ${env_COMPONENT} ./helm \
                                        -f ./helm/values-sit.yaml \
                                        --namespace roboshop-sit \
                                        --create-namespace \
                                        --set deployment.imageVersion=${env_COMMIT_ID} \
                                        --wait --timeout 5m

                                    kubectl rollout status deployment/${env_COMPONENT} -n roboshop-sit --timeout=120s
                                """
                            }
                            utils.updateCommitStatus('success', "Deployed ${env_COMMIT_ID} to roboshop-sit", 'sit-deploy')
                        }
                        catch (Exception e) {
                            utils.updateCommitStatus('failure', 'Deploy to roboshop-sit failed', 'sit-deploy')
                            throw e
                        }
                
                    }
                    }
        }
        
    }
  post{ 
        always { 
            echo 'I will always say Hello again!'
        }
        success { 
            echo 'I will run when success'
        }
        failure { 
            echo 'I will Run when it is failed'
        }
    }
    }
}


s=5
k=5
sum=s+k
echo"sum is $sum"