pipeline {
  agent {
    docker {
      image 'maven:3.6-jdk-8'
    }
  }

  options {
    timeout(time: 10, unit: 'MINUTES')
      ansiColor('xterm')
  }

  stages {
    stage('Build') {
      steps {
        sh 'mvn compile'
      }
    }

    stage('Test') {
      steps {
        sh 'mvn test'
      }
    }

    stage('Package') {
      steps {
        sh 'mvn package'
      }
    }
  }
}
