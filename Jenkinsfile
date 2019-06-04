#!/usr/bin/env groovy

pipeline {
  agent any

  parameters {
    booleanParam(defaultValue: false, description: '', name: 'runEndToEndTestsOnPR')
  }

  options {
    ansiColor('xterm')
    timestamps()
  }

  libraries {
    lib("pay-jenkins-library@master")
  }

  environment {
    DOCKER_HOST = "unix:///var/run/docker.sock"
    RUN_END_TO_END_ON_PR = "${params.runEndToEndTestsOnPR}"
    JAVA_HOME="/usr/lib/jvm/java-1.11.0-openjdk-amd64"
  }

  stages {
    stage('Maven Build') {
      steps {
        script {
          long stepBuildTime = System.currentTimeMillis()
          sh 'mvn -version'
          sh 'mvn clean verify'
          runProviderContractTests()
          postSuccessfulMetrics("ledger.maven-build", stepBuildTime)
        }
      }
      post {
        failure {
          postMetric("ledger.maven-build.failure", 1)
        }
      }
    }

    stage('Docker Build') {
      steps {
        script {
          buildAppWithMetrics{
            app = "ledger"
          }
        }
      }
      post {
        failure {
          postMetric("ledger.docker-build.failure", 1)
        }
      }
    }
    /*stage('Tests') {
      failFast true
      parallel {
        stage('Card Payment End-to-End Tests') {
            when {
                anyOf {
                  branch 'master'
                  environment name: 'RUN_END_TO_END_ON_PR', value: 'true'
                }
            }
            steps {
                runCardPaymentsE2E("ledger")
            }
        }
      }
    }*/
    stage('Docker Tag') {
      steps {
        script {
          dockerTagWithMetrics {
            app = "ledger"
          }
        }
      }
      post {
        failure {
          postMetric("ledger.docker-tag.failure", 1)
        }
      }
    }
    stage('Deploy') {
      when {
        branch 'master'
      }
      steps {
        //checkPactCompatibility("ledger", gitCommit(), "test")
        deployEcs("ledger")
      }
    }
    /*stage('Pact Tag') {
      when {
        branch 'master'
      }
      steps {
        echo 'Tagging provider pact with "test"'
        tagPact("ledger", gitCommit(), "test")
      }
    }*/
    /*stage('Smoke Tests') {
      when {
        branch 'master'
      }
      steps {
        runDirectDebitSmokeTest()
      }
    }*/
    stage('Complete') {
      failFast true
      parallel {
        stage('Tag Build') {
          when {
            branch 'master'
          }
          steps {
            tagDeployment("ledger")
          }
        }
        stage('Trigger Deploy Notification') {
          when {
            branch 'master'
          }
          steps {
            triggerGraphiteDeployEvent("ledger")
          }
        }
      }
    }
  }
  post {
    failure {
      postMetric(appendBranchSuffix("ledger") + ".failure", 1)
    }
    success {
      postSuccessfulMetrics(appendBranchSuffix("ledger"))
    }
  }
}