package org.gebish.actionlog


import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.regex.Pattern
import java.util.stream.Collectors
import java.util.stream.Stream

import org.codehaus.groovy.runtime.wrappers.Wrapper
import org.openqa.selenium.remote.RemoteWebElement

import geb.Browser
import geb.Page
import geb.content.PageContentContainer
import geb.content.PageContentTemplate
import geb.content.TemplateDerivedContentStringRepresentationProvider
import geb.content.TemplateDerivedPageContent
import geb.navigator.DefaultNavigator
import geb.test.ManagedGebTest

class ReportTracingInterceptor implements Interceptor {

    private static final Set<String> traceMethods = new HashSet<>(Arrays.asList('value', 'click', 'sendKeys', 'at', 'to'))

    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern('yyyy-MM-dd\'T\'HH:mm:ss[.SSS]')

    private Writer writer = new PrintWriter(System.out)

    private static final Pattern WEBDRIVER_CLEANUP = Pattern.compile('\\[\\w+Driver:[\\w )(-]+] ->')

    private TracingContext tc() {
        return TracingContext.currentContext
    }

    /**
     * Returns the writer associated with this interceptor.
     */
    Writer getWriter() {
        return writer
    }

    /**
     * Changes the writer associated with this interceptor.
     */
    void setWriter(Writer writer) {
        this.writer = writer
    }

    Object beforeInvoke(Object object, String methodName, Object[] arguments) {
        final TracingContext tc = tc()
        if (traceMethods.contains(methodName) && !tc.tracingActive) {
            tc.tracingActive = true
            if (tc.indent == 0) {
                write(object, methodName, arguments)
                screenshot(methodName, arguments, 'before')
            }
            tc.increaseIndent()
            tc.tracingActive = false
        }
        return null
    }

    Object afterInvoke(Object object, String methodName, Object[] arguments, Object result) {
        final TracingContext tc = tc()
        if (traceMethods.contains(methodName) && !tc.tracingActive) {
            tc.tracingActive = true
            tc.decreaseIndent()
            if (tc.indent == 0) {
                screenshot(methodName, arguments, 'after')
            }
            tc.tracingActive = false
        }
        return result
    }

    boolean doInvoke() {
        return true
    }

    private void screenshot(String methodName, Object[] arguments, String beforeAfter) {
        final String methodSignature = methodSignature(methodName, arguments)
        final TracingContext tc = tc()
        if (tc.shouldTakeScreenshot(beforeAfter, methodSignature)) {
            try {
                ManagedGebTest currentSpec = ReportingContext.currentSpec
                final Page page = currentSpec.testManager.browser.page
                currentSpec.testManager.report(String.format('%03d %s %s %s',
                        tc.counter,
                        beforeAfter,
                        methodName,
                        page != null ? page.class.simpleName : ''))
            } catch (Exception e) {
                e.printStackTrace(System.err)
            }
        }
    }

    protected String methodSignature(String methodName, Object[] arguments) {
        StringBuilder stringBuilder = new StringBuilder(30)
        stringBuilder.append(methodName)
        stringBuilder.append('(')
        for (int i = 0; i < arguments.length; i++) {
            if (i > 0) {
                stringBuilder.append(', ')
            }
            Object argument = arguments[i]
            if (argument != null) {
                stringBuilder.append(argument.class.simpleName)
            }
        }
        stringBuilder.append(')')
        return stringBuilder.toString()
    }

    private String indent() {
        StringBuilder result = new StringBuilder()
        result.append(String.format('%03d ', tc().increaseCounter()))
        for (int i = 0; i < tc().indent; i++) {
            result.append('  ')
        }
        return result.toString()
    }

    protected void write(Object object, String methodName, Object[] arguments) {
        try {
            if (tc().includeTimestamps) {
                writer.write(' [')
                writer.write(TIMESTAMP_FORMAT.format(LocalDateTime.now()))
                writer.write('] ')
            }
            writer.write(indent())
            writeInfo(object, methodName, arguments)
            writer.write('\n')
            writer.flush()
        } catch (IOException e) {
            e.printStackTrace(System.err)
        }
    }

    protected void writeInfo(Object instance, String methodName, Object[] arguments) {
        try {
            writer.write('[')
            writer.write(optimizeName(instance))
            writer.write(']')
            writer.write('.')
            writer.write(methodName)
            writer.write('(')
            for (int i = 0; i < arguments.length; i++) {
                if (i > 0) {
                    writer.write(', ')
                }
                Object argument = arguments[i]
                if (argument != null) {
                    writer.write(optimizeArgument(argument))
                }
            }
            writer.write(')')
        } catch (IOException e) {
            e.printStackTrace(System.err)
        }
    }

    private String optimizeArgument(Object argument) {
        String argValue
        if (argument instanceof Wrapper) {
            argValue = ((Wrapper) argument).unwrap().toString()
        } else if (argument instanceof Class) {
            argValue = ((Class) argument).simpleName
        } else {
            argValue = argument.toString()
        }
        return tc().maskProtected(argValue)
    }

    private String optimizeName(Object object) {
        if (object instanceof PageContentContainer) {
            PageContentContainer container = (PageContentContainer) object
            return Stream.concat(
                    Stream.of(container.rootContainer.class.simpleName),
                    container.contentPath.stream())
                    .collect(Collectors.joining('.'))
        } else if (object instanceof TemplateDerivedContentStringRepresentationProvider) {
            PageContentTemplate template = (PageContentTemplate) object.template

            if (template.owner != null) {
                return String.format('%s.%s.%s',
                        optimizeName(template.owner),
                        template.name,
                        optimizeName(object.innerProvider))
            }
        } else if (object instanceof TemplateDerivedPageContent) {
            return optimizeName(object._stringRepresentationProvider)
        } else if (object instanceof DefaultNavigator) {
            DefaultNavigator nonEmptyNavigator = (DefaultNavigator) object
            if (nonEmptyNavigator.size() > 1) {
                return nonEmptyNavigator.allElements().stream().map{optimizeName(it)}.collect(Collectors.joining(';'))
            } else if (nonEmptyNavigator.size() == 1) {
                return optimizeName(nonEmptyNavigator.getElement(0))
            }
        } else if (object instanceof RemoteWebElement) {
            // The String includes the Browser which is noise for us
            // e.g. [[FirefoxDriver: firefox on WINDOWS (eb5a4ceb-041a-461a-a407-407a3f58da20)] -> id: prompt]
            return WEBDRIVER_CLEANUP.matcher(object.toString()).replaceFirst('[Driver] ->')
        } else if (object instanceof Class) {
            return ((Class) object).simpleName
        } else if (object instanceof Browser) {
            return 'Browser'
        }

        return object.toString()
    }
}
