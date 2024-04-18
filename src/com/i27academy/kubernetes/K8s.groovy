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

    def k8sdeploy(){
        jenkins.sh """#!/bin/bash
        echo "Executing K8S Deploy Method"
        kubectl apply -f ./.cicd/k8s_dev.yaml
        """
    }

}
