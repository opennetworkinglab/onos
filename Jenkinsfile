#!groovy

pipeline {
    agent any

    stages {
        stage('build') {
            steps {
                sh '''#!/bin/bash -l
                ONOS_ROOT=`pwd`
                source tools/build/envDefaults
                onos-buck build onos
                '''
            }
        }

        stage('unit-test') {
            steps {
                sh '''#!/bin/bash -l
                ONOS_ROOT=`pwd`
                source tools/build/envDefaults
                onos-buck test
                '''
            }
        }

        stage('docker-image') {
            steps {
                sh '''#!/bin/bash -l
                ONOS_ROOT=`pwd`
                source tools/build/envDefaults
                docker build -t onosproject/onos-test-docker .
                '''
            }
        }
    }

}
