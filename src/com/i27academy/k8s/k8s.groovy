package com.i27academy.k8s

class K8s {
    def jenkins
    K8s(jenkins) {
        this.jenkins = jenkins
    }

    def auth_login_eks(eks_cluster_name, eks_region) {
    jenkins.sh """#!/bin/bash
    echo "Entering Authentication method for EKS Cluster Login"
    # Update kubeconfig with EKS cluster details
    aws eks --region $eks_region update-kubeconfig --name $eks_cluster_name
    echo "************* Listing Number of Nodes in EKS *************"
    # List the nodes in the EKS cluster
    kubectl get nodes
    kubectl get ns
    """
}

}
