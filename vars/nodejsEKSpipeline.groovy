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
        stage('installing dependencies'){  //stage 2 
            steps{
                script{
                    sh """
                       npm install
                    """
                }
            }
        }
         // this stage gives us coverage report and test cases report, sonarqube access this to check quality gate
        stage('Unit tests'){  //stage 5 its for testing purpose 
            steps {
                script {
                    try{

                    sh """
                        npm test
                    """
                     utils.updateCommitStatus('SUCCESS', 'Unit tests passed', 'unit-tests')
                    }
                  catch (Exception e) {
                            utils.updateCommitStatus('FAILURE', 'Unit tests failed', 'unit-tests')
                            throw e
                        }
                } 
            }

        /*  stage('SonarQube Analysis') {  stage-4
             steps {
                  as we give credentials of our sonarqube server token and saved in (sonar-server) using here get acces jenkins to sonarqube using
                 withSonarQubeEnv('sonar-server') {
                     sh "${tool 'sonar-8'}/bin/sonar-scanner"
                 }
             }
         } 
         stage('SonarQube Quality Gate') {  stage-5 by this we add our quality gate whole code it will give revert back to here jenkins if success pipeline builds otherswise it aborts
             steps {
                 timeout(time: 10, unit: 'MINUTES') {
                     script {
                         def qg = waitForQualityGate()  Pauses pipeline
                         if (qg.status != 'OK') {
                             error "Pipeline aborted: ${qg.status}"
                         }
                     }
                 }
             }

         } */
          stage('library-scan')  //stage 6  Dependabot alerts checking
             steps {
                 script{
                    try{
                 withCredentials([string(credentialsId: 'github-token', variable: 'GH_TOKEN')]) {
                     sh '''
                         set -e

                         REPO="srikanth-865/catalogue"

                         curl -s -L \
                         -H "Accept: application/vnd.github+json" \
                         -H "Authorization: Bearer ${GH_TOKEN}" \
                         -H "X-GitHub-Api-Version: 2026-03-10" \
                         "https:api.github.com/repos/${REPO}/dependabot/alerts?state=open" \
                         -o alerts.json

                         echo "---- Open Dependabot Alerts ----"
                         jq -r '.[] | "\\(.number)\\t\\(.security_vulnerability.severity)\\t\\(.dependency.package.name)\\t\\(.security_advisory.ghsa_id)"' alerts.json

                         HIGH_CRITICAL_COUNT=$(jq '[.[] | select(.security_vulnerability.severity == "high" or .security_vulnerability.severity == "critical")] | length' alerts.json)

                         echo "High/Critical alert count: ${HIGH_CRITICAL_COUNT}"

                         if [ "$HIGH_CRITICAL_COUNT" -gt 0 ]; then
                             echo "❌ Found ${HIGH_CRITICAL_COUNT} High/Critical severity dependency alert(s). Failing build."
                             exit 1
                         else
                             echo "✅ No High/Critical dependency alerts found."
                        fi
                     '''
               }
                   utils.updateCommitStatus('SUCCESS', 'Library scan passed', 'library-scan')
             }
                 catch (Exception e){
                            utils.updateCommitStatus('FAILURE', 'Library scan failed', 'library-scan')
                            throw e
                }
                 }
            }
        
       stage("docker build"){   //stage 3
        steps{
            script{ 
                try{
                 // in this as we provide aws-creds in jenkins credential so we giving here so by that it will get secretkey&acceskey to build and push image into aws
                withAWS(credentials: 'aws-creds', region: 'us-east-1') {
                sh """
                     aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin ${acc_id}.dkr.ecr.us-east-1.amazonaws.com
                    docker build -t ${acc_id}.dkr.ecr.us-east-1.amazonaws.com/${project}/${component}:${appversion} .  
                """
              }
                utils.updateCommitStatus('success', 'Docker image build', 'build-image')
               }
              catch (Exception e) {
              utils.updateCommitStatus('failure', 'Docker image faied', 'build-image')
               throw e
                                 }
        } 
        }
       }
       stage('Trivy Scan') {  stage 7
            steps {
                script {
                    try{
                    def dockerfileScan = sh(
                        script: """
                            trivy config --exit-code 1 --severity HIGH,CRITICAL --format table ./Dockerfile
                        """,
                        returnStatus: true
                    )

                    def imageScan = sh(
                        script: """
                            trivy image --scanners vuln --pkg-types os --exit-code 1 --severity HIGH,CRITICAL --format table ${acc_id}.dkr.ecr.us-east-1.amazonaws.com/${project}/${component}:${appversion}
                        """,
                        returnStatus: true
                    )

                    if (dockerfileScan != 0 || imageScan != 0) {
                        error "Trivy found HIGH/CRITICAL issues in Dockerfile and/or OS packages. Failing pipeline."
                    }
                    utils.updateCommitStatus('success', 'trivy scan success', 'trivy-scan')
                }
                 catch (Exception e) {
              utils.updateCommitStatus('failure', 'Docker image faied', 'trivy-scan')
               throw e
                                 }

            }
        }
       }

        stage("ECR image push"){  stage 8
        steps{
            script{ 
                try{
                 //in this as we provide aws-creds in jenkins credential so we giving here so by that it will get secretkey&acceskey to build and push image into aws
                withAWS(credentials: 'aws-creds', region: 'us-east-1') {
                sh """
                     aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin ${acc_id}.dkr.ecr.us-east-1.amazonaws.com
                    docker push ${acc_id}.dkr.ecr.us-east-1.amazonaws.com/${project}/${component}:${appversion}   
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