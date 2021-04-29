package org.gebish.example

import geb.spock.GebReportingSpec
import spock.lang.Stepwise

@Stepwise
class StepwiseVideoRecordingSpec extends GebReportingSpec {

    def 'go to gebish.org first'() {
        expect:
        browser.go('https://gebish.org')
    }

    def 'then go to spockframework.org'() {
        expect:
        browser.go('https://spockframework.org/')
        sleep 1000
    }
}
