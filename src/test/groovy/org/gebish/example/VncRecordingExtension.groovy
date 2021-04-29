package org.gebish.example

import geb.test.ManagedGebTest
import org.spockframework.runtime.extension.IGlobalExtension
import org.spockframework.runtime.model.SpecInfo

class VncRecordingExtension implements IGlobalExtension {

    @Override
    void start() {
    }

    @Override
    void visitSpec(SpecInfo spec) {
        if (ManagedGebTest.isAssignableFrom(spec.reflection) && testContainers) {
            VncRecordingInterceptor interceptor = new VncRecordingInterceptor()
            spec.bottomSpec.allFeatures*.featureMethod*.addInterceptor(interceptor)
            spec.bottomSpec.addCleanupSpecInterceptor(interceptor)
        }
    }

    @Override
    void stop() {
    }

    boolean isTestContainers() {
        // maybe there is a better way to check if testcontainers should be used,
        // otherwise the interceptor will have to be registered for all drivers and then check the type of the driver
        System.getProperty("geb.env")?.contains('testcontainer')
    }
}
