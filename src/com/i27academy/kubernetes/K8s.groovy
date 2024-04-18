package com.i27academy.kubernetes

class K8s{

    def jenkins
    K8s(jenkins){
        this.jenkins = jenkins
    }

    def auth_login_eks(eks_cluster_name, eks_region){
        jenkins.sh """#!/bin/bash
        echo "Entering Authentication method for EKS Cluster Login"
        # Update kubeconfig with EKS cluster details
        aws eks --region $eks_region update-kubeconfig --name $eks_cluster_name
        echo "************* Listing Number of Nodes in EKS *************"
        # List the nodes in the EKS cluster
        kubectl get nodes
        """
    }

    def k8sdeploy(docker_image){
        jenkins.sh """#!/bin/bash
        echo "Executing K8S Deploy Method"
        echo "Final Image Tag is $docker_image"
        kubectl apply -f ./.cicd/k8s_dev.yaml
        """
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
                docker.buildApp("${env.APPLICATION_NAME}")
                dockerBuildandPush().call()
            }

        }
    }

}
