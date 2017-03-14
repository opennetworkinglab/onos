pipeline {
    agent any
    stages {
        stage('build') {
            steps {
                sh 'buck build onos'
            }
        }
    }
}
