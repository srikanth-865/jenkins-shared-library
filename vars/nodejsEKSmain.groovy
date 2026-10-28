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
                    docker tag  ${acc_id}.dkr.ecr.us-east-1.amazonaws.com/${project}/${component}  ${acc_id}.dkr.ecr.us-east-1.amazonaws.com/${project}/${env.GIT_COMMIT}
                    docker push ${acc_id}.dkr.ecr.us-east-1.amazonaws.com/${project}/${env.GIT_COMMIT}
                """
              }
              utils.updateCommitStatus('success', 'push image to ECR', 'push-image')
          }
          catch(Exception e){
                            utils.updateCommitStatus('failure', 'push image to ECR', 'push-image')
                            throw e
        }
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