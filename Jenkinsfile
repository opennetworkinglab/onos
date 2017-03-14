pipeline {
    agent any

    stages {
        stage('build') {
            steps {
                sh '''#!/bin/bash -l
                . tools/build/envDefaults && onos-buck build onos
                '''
            }
        }
    }
}
