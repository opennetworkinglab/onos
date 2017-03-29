#!groovy

pipeline {

    agent any

    stages {
        stage('pull') {
            steps {
                sh 'which warden-client && sum `which warden-client`'
                sh 'warden-client list'
                git url: 'https://gerrit.onosproject.org/onos'
                sh 'warden-client --reqId CI-${BUILD_NUMBER} --timeout 5 --duration 10 --nodes 1 reserve'
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
                    "stc": {
                        timeout(10) {
                            sh '''#!/bin/bash -l
                                export stcColor=false
                                ONOS_ROOT=`pwd`
                                source tools/build/envDefaults
                                onos-package-test
                                echo "Waiting for cell..."
                                warden-client --reqId CI-${BUILD_NUMBER} status > cell.txt
                                source cell.txt
                                rm -f cell.txt
                                proxy-stc
                            '''
                        }
                    }
                )
            }
        }
    }

    post {
        always {
            sh '''#!/bin/bash -l
            warden-client --reqId CI-${BUILD_NUMBER} return
            '''
        }
    }
}

