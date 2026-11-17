pipeline {
    agent { label 'windows' }
    tools { maven 'Maven3'; jdk 'JDK' }
    options {
        skipDefaultCheckout(true)
        disableConcurrentBuilds()
        skipStagesAfterUnstable()
        timeout(time: 15, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '15'))
    }
    triggers { pollSCM('* * * * *') }
    parameters {
        string(name: 'PRODUCCION_DIR', defaultValue: 'C:\\Jenkins\\produccion\\catalogo', description: 'Carpeta persistente fuera del workspace; escribible por el agente')
    }
    stages {
        stage('Checkout') {
            steps {
                checkout scm
                script {
                    env.APP_REVISION = bat(script: '@git rev-parse HEAD', returnStdout: true).trim()
                }
            }
        }
        stage('Build y pruebas unitarias') {
            steps { bat 'mvn -B -ntp clean test' }
            post { always { junit testResults: '**/target/surefire-reports/TEST-*.xml', allowEmptyResults: false } }
        }
        stage('Empaquetado e integracion') {
            steps { bat 'mvn -B -ntp verify -DskipUTs=true' }
            post { always { junit testResults: '**/target/failsafe-reports/TEST-*.xml', allowEmptyResults: false } }
        }
        stage('Archivar artefacto probado') {
            steps { archiveArtifacts artifacts: 'catalogo-web/target/catalogo.jar', fingerprint: true }
        }
        stage('Despliegue automatico y smoke test') {
            steps {
                script {
                    withEnv(["PRODUCCION_DIR=${params.PRODUCCION_DIR}"]) {
                        bat 'powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts\\deploy.ps1 -Jar catalogo-web\\target\\catalogo.jar -Destino "%PRODUCCION_DIR%" -Revision "%APP_REVISION%" -Puerto 8081'
                    }
                }
            }
        }
    }
    post {
        success { echo 'Version probada y desplegada. Revisar http://localhost:8081 y /api/estado.' }
        failure { echo 'Pipeline detenido. Revisar el primer error y los reportes JUnit. No promover codigo con pruebas fallidas.' }
    }
}
