package com.i27academy.kubernetes

class K8s {
    def jenkins
    K8s(jenkins) {
        this.jenkins = jenkins
    }

    def auth_login_eks() {
    jenkins.sh """#!/bin/bash
    echo "Entering Authentication method for EKS Cluster Login"
    # Update kubeconfig with EKS cluster details
    aws eks --region us-east-1 update-kubeconfig --name clothingCluster
    echo "************* Listing Number of Nodes in EKS *************"
    # List the nodes in the EKS cluster
    kubectl get nodes
    kubectl get ns
    """
    }

}
