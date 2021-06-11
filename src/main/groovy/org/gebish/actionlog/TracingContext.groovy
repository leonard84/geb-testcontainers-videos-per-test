package org.gebish.actionlog

import groovy.transform.Canonical

@Canonical
class TracingContext {

    private static final ThreadLocal<TracingContext> CURRENT = ThreadLocal.<TracingContext>withInitial {
        new TracingContext()
    }

    Set<String> screenshotOn = []
    Set<String> protect = []

    int indent = 0
    int counter = 0
    boolean tracingActive
    boolean includeTimestamps

    int increaseIndent() {
        indent++
    }

    int decreaseIndent() {
        indent--
    }

    int increaseCounter() {
        ++counter
    }

    void configure(Map config) {
        screenshotOn = (config.screenshotOn as Set) ?: []
        includeTimestamps = config.includeTimestamps
        counter = 0
        indent = 0
    }

    void protectPassword(String password) {
        protect.add(password)
    }

    boolean shouldTakeScreenshot(String beforeOrAfter, String method) {
        screenshotOn.contains("$beforeOrAfter:$method".toString())
    }

    String maskProtected(String value) {
        protect.contains(value) ? '*****' : value
    }

    static TracingContext getCurrentContext() {
        CURRENT.get()
    }
}
