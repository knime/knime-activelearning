#!groovy
def BN = (BRANCH_NAME == 'master' || BRANCH_NAME.startsWith('releases/')) ? BRANCH_NAME : 'releases/2024-12'

library "knime-pipeline@todo/DEVOPS-2151-workflow-tests-default-mac-os-arm"

properties([
    pipelineTriggers([
        upstream('knime-javasnippet/' + env.BRANCH_NAME.replaceAll('/', '%2F'))
    ]),
    buildDiscarder(logRotator(numToKeepStr: '5')),
	parameters(workflowTests.getConfigurationsAsParameters([
        ignoreConfiguration: ['macosx-aarch']
    ])),
    disableConcurrentBuilds()
])

try {
    // Needs more RAM because of the dependency on tensorflow
    // it needs to download and install a bunch of large binaries
    // Therefore we use the 'large' image and allow mvn to use 4G of memory
    withEnv(["MAVEN_OPTS=-Xmx4G"]){
        knimetools.defaultTychoBuild('org.knime.update.activelearning', 'maven && large && java17')
    }

    workflowTests.runTests(
        dependencies: [
            repositories: [
                'knime-activelearning', 
                'knime-datageneration', 
                'knime-deeplearning', 
                'knime-distance', 
                'knime-filehandling',
                'knime-kerberos',
                'knime-jep', 
                'knime-jfreechart', 
                'knime-js-base', 
                'knime-streaming', 
                'knime-tensorflow' 
            ],
        ],
        // configurations: ['MacOS_12_M1_knime420', 'MacOS_13_M1_knime421', 'MacOS_14_M1_knime494'],
        withAssertions: true,
    )

    // stage('Sonarqube analysis') {
    //     env.lastStage = env.STAGE_NAME
    //     workflowTests.runSonar()
    // }

} catch (ex) {
    currentBuild.result = 'FAILURE'
    throw ex
    notifications.notifyBuild(currentBuild.result)
} finally {
}
/* vim: set shiftwidth=4 expandtab smarttab: */
