package org.gebish.actionlog


import geb.Browser
import geb.content.TemplateDerivedPageContent
import geb.navigator.DefaultNavigator

class ActionLogInjector {

    private static boolean injected
    private static ReportTracingInterceptor tracingInterceptor = new ReportTracingInterceptor()

    static void configure(Map config) {
        TracingContext.currentContext.configure(config)
        if (injected) {
            return
        }

        [TemplateDerivedPageContent, DefaultNavigator, Browser].each { Class clazz ->
            Class.forName(clazz.name) // Hack the Static initializer
            def proxyMetaClass = ProxyMetaClass.getInstance(clazz)
            proxyMetaClass.interceptor = tracingInterceptor
            clazz.metaClass = proxyMetaClass
        }
        injected = true
    }
}
