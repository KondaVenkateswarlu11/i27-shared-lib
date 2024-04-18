import com.i27academy.builds.Docker
import com.i27academy.kubernetes.K8s

def call(Map pipelineParams) {
    Docker docker = new Docker(this)
    K8s k8s = new K8s(this)

    pipeline {
        agent{
            label 'k8s-slave'
        }
        parameters {
            choice(name: 'buildOnly',
                choices: 'no\nyes',
                description: 'This will only build the application'
            )
            choice(name: 'scanOnly',
                choices: 'no\nyes',
                description: 'This will Scan the application'
            )
            choice(name: 'dockerPush',
                choices: 'no\nyes',
                description: 'This will build the app, docker build, docker push'
            )
            choice(name: 'deployToDev',
                choices: 'no\nyes',
                description: 'This will Deploy the app to Dev env'
            )
            choice(name: 'deployToTest',
                choices: 'no\nyes',
                description: 'This will Deploy the app to Test env'
            )
            choice(name: 'deployToStage',
                choices: 'no\nyes',
                description: 'This will Deploy the app to Stage env'
            )
            choice(name: 'deployToProd',
                choices: 'no\nyes',
                description: 'This will Deploy the app to Prod env'
            )
        }
        environment{
            APPLICATION_NAME = "${pipelineParams.appName}"
            POM_VERSION = readMavenPom().getVersion()
            POM_PACKAGING = readMavenPom().getPackaging()

            DOCKER_HUB = "docker.io/venkat3cs"
            DOCKER_CREDS = credentials('Hub.Docker_Creds')

            SONAR_URL = "http://54.236.253.51:9000/" 
            SONAR_TOKEN = credentials('Sonar_Token')
        }
        tools{
            maven 'Maven-3.8.8'
            jdk 'JDK-17'
        }
        stages {
            stage ('Authenticate to AWS Cloud EKS') {
                steps {
                    echo "Executing in AWS Cloud auth stage"
                    script {
                        //eks_cluster_name, eks_zone
                        k8s.auth_login_eks()
                    }  
                }
            }
            stage ('Build'){
                when {
                    anyOf {
                        expression {
                            params.buildOnly == 'yes'
                        // params.dockerPush == 'yes'
                        }
                    }
                }
                // Application Build happens here
                steps { // jenkins env variable no need of env 
                    script {
                        //buildApp().call()
                        echo "********** Executing Addition Method **********"
                        println docker.add(4,5)
                        docker.buildApp("${env.APPLICATION_NAME}")
                    }

                    //-DskipTests=true 
                }
            }
            stage("Unit Test"){
                when{
                    anyOf{
                        expression{
                            params.buildOnly == 'yes'
                            params.dockerPush == 'yes'
                        }
                    }
                }
                steps{
                    echo "This Stage will perform the unittests for ${env.APPLICATION_NAME} application"
                    sh "mvn test"
                }
                // this will show the test results trend using the jacoco plugin
                post{
                    always{
                        junit 'target/surefire-reports/*.xml'
                    }
                }
            }
            stage("Sonar"){
                when{
                    anyOf{
                        expression{
                            params.scanOnly == 'yes'
                        }
                    }
                }
                steps{
                    echo "*******************Sonar scans will happen at this stage***********************"
                    withSonarQubeEnv('Sonar-Server'){
                        sh"""
                            mvn clean verify sonar:sonar \
                                -Dsonar.projectKey=i27-eureka \
                                -Dsonar.host.url=${env.SONAR_URL} \
                                -Dsonar.login=${SONAR_TOKEN}              
                        """
                    }
                    timeout (time: 5, unit: 'MINUTES'){
                        script{
                            waitForQualityGate abortPipeline: true
                        }
                        
                    }
                }
            }
            //We have written this stage just for our understanding nothing to do with it, but very imp
            /*
            stage("Docker Format"){
                steps{
                    echo "This stage will change the format of the .jar file as per our requirement"
                    //This is how we will read the actual format of the .jar file
                    echo "Actual Format: ${env.APPLICATION_NAME}-${env.POM_VERSION}-${env.POM_PACKAGING}"

                    //Now We need to fromat the as our custom wish 
                    //eureka-buildnumber-branchname-packaging
                    echo "Custom Fromat: ${env.APPLICATION_NAME}-${currentBuild.number}-${BRANCH_NAME}.${env.POM_PACKAGING}"
                }
            }*/
            stage("Docker Build and Push"){
                when{
                    anyOf{
                        expression{
                            params.dockerPush == 'yes'
                        }
                    }
                }
                steps{
                    script{
                        dockerBuildandPush().call()
                    }
                    
                }
            }
            stage("Deploy to Docker-Dev"){
                when{
                    anyOf{
                        expression{
                            params.deployToDev == 'yes'
                        }
                    }
                }
                steps{
                    script{
                        imageValidation().call()
                        dockerDeploy('dev', '5761', '8761').call()
                        echo "Deployed to Dev Successfully!!!!!!!!!"
                    }
                }
            }
            stage("Deploy to Docker-test"){
                when{
                    anyOf{
                        expression{
                            params.deployToTest == 'yes'
                        }
                    }
                }
                steps{
                    script{
                        imageValidation().call()
                        echo "***** Entering Test Environment *****"
                        dockerDeploy('tst', '6761', '8761').call()
                    }
                }
            }

            stage("Deploy to Docker-stage"){
                when{
                    anyOf{
                        expression{
                            params.deployToStage == 'yes'
                        }
                    }
                }
                steps{
                    script{
                        imageValidation().call()
                        dockerDeploy('stage', '7761', '8761').call()
                    }
                }
            }
            stage("Deploy to Docker-prod"){
                when {
                    // deployToProd === yes "and" branch "release/*****" 
                    allOf {
                        anyOf {
                            expression {
                                params.deployToProd == 'yes'
                            }
                        }
                        anyOf {
                            branch 'master'
                            //branch 'release/*'
                            // only tags with vx.x.x should deploy to prod
                        }
                    }
                }
                steps{
                    timeout(time: 300, unit: 'SECONDS') {
                        input message: "Deploying ${env.APPLICATION_NAME} to prod ????", ok: 'yes', submitter: 'venkat'
                    }
                    script{
                        imageValidation().call()
                        dockerDeploy('prod', '8761', '8761').call()
                    }
                }
            }
            stage ('clean'){
                steps {
                    cleanWs()
                }
            }
        }
    }
}

//This method will build the image and push it to registry
def dockerBuildandPush(){
    return{

        sh "ls -al"
        sh "cp ${workspace}/target/i27-${env.APPLICATION_NAME}-${env.POM_VERSION}.${env.POM_PACKAGING} ./.cicd"
        sh "ls -al .cicd"               
        echo "*******************  Build Docker Image  ********************************"
        sh "docker build --force-rm --no-cache --pull --rm=true --build-arg JAR_SOURCE=i27-${env.APPLICATION_NAME}-${env.POM_VERSION}.${env.POM_PACKAGING} -t ${env.DOCKER_HUB}/${env.APPLICATION_NAME}:${GIT_COMMIT} ./.cicd"
        
        echo "*******************  Login to DockerHub   ********************************"
        sh "docker login -u ${DOCKER_CREDS_USR} -p ${DOCKER_CREDS_PSW}"

        echo "*******************  push the docker image to dockerhub  ********************************"
        sh "docker push ${env.DOCKER_HUB}/${env.APPLICATION_NAME}:${GIT_COMMIT}"
        echo "Pushed the image succesfully!!!"
    
    }
}


// This method is developed for Deploying our App in different environments
def dockerDeploy(envDeploy, hostPort, contPort) {
    return{
        echo "*******************Deploying To $envDeploy**********************"
        withCredentials([usernamePassword(credentialsId: 'dockerenv_maha_creds', passwordVariable: 'PASSWORD', usernameVariable: 'USERNAME')]) {
            // some block
            // With the help of this block, ,the slave will be connecting to docker-vm and execute the commands to create the containers.
            //sshpass -p ssh -o StrictHostKeyChecking=no user@host command_to_run
            //sh "sshpass -p ${PASSWORD} -v ssh -o StrictHostKeyChecking=no ${USERNAME}@${docker_server_ip} hostname -i"
                        
            script{
                echo "Pulling the image from dockerhub"
                sh "sshpass -p ${PASSWORD} -v ssh -o StrictHostKeyChecking=no ${USERNAME}@${docker_server_ip} docker pull ${env.DOCKER_HUB}/${env.APPLICATION_NAME}:${GIT_COMMIT}"
                try{
                    echo "Stopping the container"
                    sh "sshpass -p ${PASSWORD} -v ssh -o StrictHostKeyChecking=no ${USERNAME}@${docker_server_ip} docker stop ${env.APPLICATION_NAME}-$envDeploy"
                    echo "removing the container"
                    sh "sshpass -p ${PASSWORD} -v ssh -o StrictHostKeyChecking=no ${USERNAME}@${docker_server_ip} docker rm ${env.APPLICATION_NAME}-$envDeploy"
                }
                catch(error){
                    echo "caught the error: $error"
                }                           
                echo "creating the container"
                sh "sshpass -p ${PASSWORD} -v ssh -o StrictHostKeyChecking=no ${USERNAME}@${docker_server_ip} docker run -d -p $hostPort:$contPort --name ${env.APPLICATION_NAME}-$envDeploy ${env.DOCKER_HUB}/${env.APPLICATION_NAME}:${GIT_COMMIT}"               
                            
            }
        }
            
    }
}

//This methode will validate the image 
def imageValidation(){
    return{
        println ("Pulling the docker image")
        try{
            sh "docker pull ${env.DOCKER_HUB}/${env.APPLICATION_NAME}:${GIT_COMMIT}"
        }
        catch (Exception e) {
            println("OOPS!, docker images with this tag is not available")
            buildApp().call()
            dockerBuildandPush().call()
        }

    }
}

