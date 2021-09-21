#!/usr/bin/env groovy

def runConsumerContractTests(consumerTag) {
  runTypedContractTests("consumer", consumerTag)
}

def runProviderContractTests() {
  runTypedContractTests("provider")
}

def runTypedContractTests(contractTestType, consumerTag = "master") {
  def commit = gitCommit()
  withCredentials([
          string(credentialsId: 'pact_broker_username', variable: 'PACT_BROKER_USERNAME'),
          string(credentialsId: 'pact_broker_password', variable: 'PACT_BROKER_PASSWORD')]
  ) {
      def contractTestTypeFilter = ""
      if (contractTestType == "consumer") {
        contractTestTypeFilter = "-DrunConsumerContractTests=true"
      } else if (contractTestType == "provider") {
        contractTestTypeFilter = "-DrunContractTests=true"
      }

      sh "mvn clean package pact:publish " +
          "-DPACT_BROKER_URL=https://pact-broker-test.cloudapps.digital " +
          "-DPACT_BROKER_USERNAME=${PACT_BROKER_USERNAME} " +
          "-DPACT_BROKER_PASSWORD=${PACT_BROKER_PASSWORD} " +
          "-DPACT_CONSUMER_VERSION=${commit} " +
          "-DPACT_CONSUMER_TAG=${consumerTag} " +
          "-Dpact.provider.version=${commit} " +
          "-Dpact.verifier.publishResults=true " +
          "${contractTestTypeFilter}"
  }
}

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
    stage('Unit and Integration Tests') {
      steps {
        script {
          def stepBuildTime = System.currentTimeMillis()
          sh 'mvn clean verify'
          postSuccessfulMetrics("ledger.unit-it-test", stepBuildTime)
        }
      }
      post {
        failure {
          postMetric("ledger.unit-test.failure", 1)
        }
      }
    }
    stage('Contract Tests: Ledger as Provider') {
      steps {
        script {
          def stepBuildTime = System.currentTimeMillis()
          runProviderContractTests()
          postSuccessfulMetrics("ledger.provider-contract-tests", stepBuildTime)
        }
      }
      post {
        failure {
          postMetric("ledger.provider-contract-tests.failure", 1)
        }
      }
    }
    stage('Contract Tests: Ledger as Consumer (branch build)') {
      when {
        not {
          branch 'master'
        }
      }
      steps {
        script {
          def stepBuildTime = System.currentTimeMillis()
          def branchName = gitBranchName()
          runConsumerContractTests(branchName)
          postSuccessfulMetrics("ledger.consumer-contract-tests", stepBuildTime)
        }
      }
      post {
        failure {
          postMetric("ledger.maven-build.failure", 1)
        }
      }
    }
    stage('Contract Tests: Ledger as Consumer (master)') {
      when {
        branch 'master'
      }
      steps {
        script {
          def stepBuildTime = System.currentTimeMillis()
          runConsumerContractTests("master")
          postSuccessfulMetrics("ledger.consumer-contract-tests", stepBuildTime)
        }
      }
      post {
        failure {
          postMetric("ledger.consumer-contract-tests.failure", 1)
        }
      }
    }
    stage('Contract Tests: Providers to Ledger') {
      steps {
        script {
          env.PACT_TAG = gitBranchName()
        }
        ws('contract-tests-wp') {
          runPactProviderTests("pay-connector", "${env.PACT_TAG}", "ledger")
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
    stage('End-to-End Tests') {
      when {
          anyOf {
            branch 'master'
            environment name: 'RUN_END_TO_END_ON_PR', value: 'true'
          }
      }
      steps {
          runAppE2E("ledger", "card")
      }
    }
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
      when {
        branch 'master'
      }
      steps {
        checkPactCompatibility("ledger", gitCommit(), "test")
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
    stage('Smoke Tests') {
      when {
        branch 'master'
      }
      steps {
        runCardSmokeTest()
      }
    }
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
