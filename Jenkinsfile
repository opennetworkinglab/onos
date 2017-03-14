pipeline {
    agent any

    stages {
        stage('build') {
            steps {
                sh '. tools/build/envDefaults && onos-buck build onos'
            }
        }
    }
}
