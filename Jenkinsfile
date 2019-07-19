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
    stage('Maven Build Master') {
      when {
        branch 'master'
      }
      steps {
        script {
          def stepBuildTime = System.currentTimeMillis()
          def commit = gitCommit()
          def branchName = 'master'

          withCredentials([
                  string(credentialsId: 'pact_broker_username', variable: 'PACT_BROKER_USERNAME'),
                  string(credentialsId: 'pact_broker_password', variable: 'PACT_BROKER_PASSWORD')]
          ) {
              sh 'mvn -version'
              sh "mvn clean package pact:publish -DrunContractTests=true -DPACT_BROKER_URL=https://pact-broker-test.cloudapps.digital -DPACT_CONSUMER_VERSION=${commit}" +
                      " -DPACT_BROKER_USERNAME=${PACT_BROKER_USERNAME} -DPACT_BROKER_PASSWORD=${PACT_BROKER_PASSWORD} -DPACT_CONSUMER_TAG=${branchName} -Dpact.provider.version=${commit} -Dpact.verifier.publishResults=true"
          }
          postSuccessfulMetrics("ledger.maven-build", stepBuildTime)
        }
      }
      post {
        failure {
          postMetric("ledger.maven-build.failure", 1)
        }
      }
    }
    stage('Maven Build Branch') {
      when {
        not {
          branch 'master'
        }
      }
      steps {
        script {
          def stepBuildTime = System.currentTimeMillis()
          def commit = gitCommit()
          def branchName = gitBranchName()

          withCredentials([
                  string(credentialsId: 'pact_broker_username', variable: 'PACT_BROKER_USERNAME'),
                  string(credentialsId: 'pact_broker_password', variable: 'PACT_BROKER_PASSWORD')]
          ) {
              sh 'mvn -version'
              sh "mvn clean package pact:publish -DrunContractTests=true -DPACT_BROKER_URL=https://pact-broker-test.cloudapps.digital -DPACT_CONSUMER_VERSION=${commit}" +
                      " -DPACT_BROKER_USERNAME=${PACT_BROKER_USERNAME} -DPACT_BROKER_PASSWORD=${PACT_BROKER_PASSWORD} -DPACT_CONSUMER_TAG=${branchName}"
          }
          postSuccessfulMetrics("ledger.maven-build", stepBuildTime)
      }
      }
      post {
          failure {
              postMetric("ledger.maven-build.failure", 1)
          }
      }
    }
    stage('Contract Tests') {
      steps {
        script {
          env.PACT_TAG = gitBranchName()
        }
        ws('contract-tests-wp') {
          runPactProviderTests("pay-connector", "${env.PACT_TAG}")
        }
      }
      post {
        always {
          ws('contract-tests-wp') {
              deleteDir()
          }
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
    stage('Check pact compatibility') {
      steps {
        checkPactCompatibility("ledger", gitCommit(), "test")
      }
    }
    stage('Deploy') {
      when {
        branch 'master'
      }
      steps {
        deployEcs("ledger")
      }
    }
    stage('Pact Tag') {
      when {
        branch 'master'
      }
      steps {
        echo 'Tagging provider pact with "test"'
        tagPact("ledger", gitCommit(), "test")
      }
    }
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