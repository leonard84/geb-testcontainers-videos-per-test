package org.gebish.actionlog

import geb.test.ManagedGebTest

class ReportingContext {
    private final static ThreadLocal<ManagedGebTest> CURRENT_SPEC = new ThreadLocal<ManagedGebTest>()

    private final static ThreadLocal<RunInfo> CURRENT_EXECUTION = new ThreadLocal<RunInfo>() {
        @Override
        protected RunInfo initialValue() {
            return new RunInfo()
        }
    }

    static ManagedGebTest getCurrentSpec() {
        CURRENT_SPEC.get()
    }

    static void setCurrentSpec(ManagedGebTest spec) {
        if (spec == null) {
            CURRENT_SPEC.remove()
        } else {
            CURRENT_SPEC.set(spec)
        }
    }

    static RunInfo getRunInfo() {
        CURRENT_EXECUTION.get()
    }
}
