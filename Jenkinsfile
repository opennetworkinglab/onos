pipeline {
    agent any

    stages {
        stage('build') {
            steps {
                sh '''#!/bin/bash -l
                source tools/build/envDefaults
                onos-buck build onos
                '''
            }
        }
    }
}
