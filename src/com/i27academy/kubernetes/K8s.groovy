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

    def k8sdeploy(fileName, docker_image, namespace ){
        jenkins.sh """#!/bin/bash
        echo "Executing K8S Deploy Method"
        echo "Final Image Tag is $docker_image"
        sed -i "s|DIT|$docker_image|g" ./.cicd/$fileName
        kubectl apply -f ./.cicd/$fileName -n $namespace
        """
    }

     def k8sHelmChartDeploy(appName, env, helmChartPath, imageTag){
       jenkins.sh """#!/bin/bash
       echo "*************** Helm Groovy method Starts here ***************"
       echo "Checking if helm chart exists"
       if helm list | grep -q "${appName}-${env}-chart"; then
        echo "Chart Exists !!!!!!!!!"
        echo "Upgrading the Chart !!!!!!"
        helm upgrade ${appName}-${env}-chart -f ./.cicd/k8s/values_${env}.yaml --set image.tag=${imageTag} ${helmChartPath}
       else 
        echo "Installing the Chart"
        helm install ${appName}-${env}-chart -f ./.cicd/k8s/values_${env}.yaml --set image.tag=${imageTag} ${helmChartPath}
       fi
       # helm install chartname -f valuesfilepath chartpath
       # helm upgrade chartname -f valuefilepath chartpath

       """

     }

    //This Methode will clone the sharedLib repo to the Eureka folder to maintaina all the files
    // in the same location for the further uses.
     def gitClone() {
       jenkins.sh """#!/bin/bash
       echo "*************** Entering Git Clone Method ***************"
       git clone -b master https://github.com/KondaVenkateswarlu11/i27-shared-lib.git
       echo "Listing the files"
       ls -la 
       echo "Showing the files under i27-shared-lib repo"
       ls -la i27-shared-lib
       echo "Showing the files under chart folder"
       ls -la i27-shared-lib/chart/
       """ 
    }
}
