import static org.openqa.selenium.remote.DesiredCapabilities.chrome
import static org.testcontainers.containers.BrowserWebDriverContainer.VncRecordingMode.RECORD_ALL
import static org.testcontainers.containers.BrowserWebDriverContainer.VncRecordingMode.RECORD_FAILING
import static org.testcontainers.shaded.org.apache.commons.io.FileUtils.ONE_GB

import org.gebish.actionlog.ActionLogInjector
import org.gebish.example.TestcontainersWebDriver
import org.testcontainers.containers.BrowserWebDriverContainer
import org.testcontainers.containers.BrowserWebDriverContainer.VncRecordingMode
import org.testcontainers.containers.Network

reportsDir = "build/reports/geb"


driver = {
    def file = new File(reportsDir, "video")
    file.mkdirs()
    def container = new BrowserWebDriverContainer()
            .withCapabilities(chrome())
            .withRecordingMode(VncRecordingMode.SKIP, null)
            .withNetwork(Network.SHARED)
            .withSharedMemorySize(2 * ONE_GB) as BrowserWebDriverContainer

    container.start()

    new TestcontainersWebDriver(container as BrowserWebDriverContainer, // groovy generics looses some type information so cast is necessary
            file,
            Boolean.getBoolean("videoRecordFailuresOnly") ?
                    RECORD_FAILING :
                    RECORD_ALL)
}

ActionLogInjector.configure(
        screenshotOn: ['before:click()', 'after:click()', 'after:value(String)'],
        includeTimestamps: true
)
