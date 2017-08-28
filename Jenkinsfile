#!groovy

pipeline {

    agent any

    stages {
        stage('pull') {
            steps {
                git url: 'https://gerrit.onosproject.org/onos'
            }
        }

        stage('build') {
            steps {
                sh '''#!/bin/bash -l
                    ONOS_ROOT=`pwd`
                    source tools/build/envDefaults
                    onos-buck build onos
                '''
            }
        }

        stage('test') {
            steps {
                parallel (
                    "unit-tests": {
                        sh '''#!/bin/bash -l
                            ONOS_ROOT=`pwd`
                            source tools/build/envDefaults
                            onos-buck test
                        '''
                    },
                    "javadocs": {
                        sh '''#!/bin/bash -l
                            ONOS_ROOT=`pwd`
                            source tools/build/envDefaults
                            onos-buck build //docs:external //docs:internal --show-output
                        '''
                    },
                    "docker-image": {
                        sh '''#!/bin/bash -l
                            ONOS_ROOT=`pwd`
                            source tools/build/envDefaults
                            docker build -t onosproject/onos-test-docker .
                        '''
                    },
                )
            }
        }
    }

}

