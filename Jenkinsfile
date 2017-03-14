pipeline {
    agent any

    stages {
        stage('build') {
            steps {
                bash '. tools/build/envDefaults && onos-buck build onos'
            }
        }
    }
}
