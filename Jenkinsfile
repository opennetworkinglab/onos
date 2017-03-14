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
    }
}
