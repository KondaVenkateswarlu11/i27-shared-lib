package com.i27academy.builds

class Docker{
    def jenkins
    Docker(jenkins){
        this.jenkins = jenkins
    }

    //Addition method
    def add(firstNumber, lastNumber){
        return firstNumber+lastNumber
    }

    //THis method will be used to build the Application
    def buildApp(appName){
        jenkins.sh """#!/bin/bash
        echo "This stage will build the $appName application"
        mvn clean package -DskipTests=true
        """ 
    }
 
}

