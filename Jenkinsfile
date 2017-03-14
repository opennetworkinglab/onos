pipeline {
    agent any
    environment {

    }
    stages {
        stage('build') {
            steps {
                sh 'tools/build/envDefaults && buck build onos'
            }
        }
    }
}
