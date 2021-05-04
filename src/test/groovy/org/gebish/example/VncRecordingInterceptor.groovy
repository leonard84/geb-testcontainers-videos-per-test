package org.gebish.example

import org.openqa.selenium.InvalidArgumentException
import org.spockframework.runtime.extension.IMethodInterceptor
import org.spockframework.runtime.extension.IMethodInvocation
import org.spockframework.runtime.model.Attachment
import org.spockframework.runtime.model.MethodKind

import geb.Browser
import geb.test.GebTestManager
import geb.test.ManagedGebTest
import net.oneandone.spock.orderextension.Order
import spock.lang.Stepwise

class VncRecordingInterceptor implements IMethodInterceptor, SafeFileName {
    TestcontainersWebDriver container


    @Override
    void intercept(IMethodInvocation invocation) throws Throwable {
        switch (invocation.method.kind) {
        // this is around setup-feature-cleanup so it runs before setup methods
            case MethodKind.ITERATION_EXECUTION:
                interceptIterationExecution(invocation)
                break
                // this is the actual feature method, we only get access to an exception here
            case MethodKind.FEATURE:
                interceptFeatureMethodExecution(invocation)
                break
                // this is at the end of the spec
            case MethodKind.CLEANUP_SPEC:
                interceptCleanupSpecExecution(invocation)
                break
            default:
                throw new InvalidArgumentException("Unsupported invocation")
        }
    }

    void interceptIterationExecution(IMethodInvocation invocation) throws Throwable {
        GebTestManager testManager = (invocation.instance as ManagedGebTest).testManager
        Browser browser = testManager.browser
        container = (browser.driver as TestcontainersWebDriver)

        container.startRecording()
        invocation.proceed()
    }

    void interceptFeatureMethodExecution(IMethodInvocation invocation) throws Throwable {
        GebTestManager testManager = (invocation.instance as ManagedGebTest).testManager
        Browser browser = testManager.browser
        container = (browser.driver as TestcontainersWebDriver)

        boolean ordered = isOrdered(invocation)
        String prefix = (ordered ? invocation.getSpec().name : invocation.iteration.name).with { getFilesystemFriendlyName(it) }

        container.startRecording()
        try {
            invocation.proceed()
        } catch (Exception | AssertionError ex) {
            container.retainRecordingIfNeeded(prefix, false).ifPresent { addRecordingAttachment(invocation, container.reportDir, it) }
            throw ex
        }

        // don't restart recording in an ordered execution
        if (!ordered) {
            container.retainRecordingIfNeeded(prefix, true).ifPresent { addRecordingAttachment(invocation, container.reportDir, it) }
        }
    }

    void interceptCleanupSpecExecution(IMethodInvocation invocation) throws Throwable {
        if (container) {
            // save for spec in case the Spec was ordered and didn't fail before
            container.retainRecordingIfNeeded(getFilesystemFriendlyName(invocation.spec.name), true)
        }
        invocation.proceed()
    }

    private static void addRecordingAttachment(IMethodInvocation invocation, File reportDir, File video) {
        String url = reportDir.parentFile.toPath().relativize(video.toPath())
        invocation.getFeature().addAttachment(new Attachment(video.name, url))
    }

    boolean isOrdered(IMethodInvocation invocation) {
        def specClass = invocation.spec.reflection
        return specClass.isAnnotationPresent(Stepwise) || specClass.isAnnotationPresent(Order)
    }
}
