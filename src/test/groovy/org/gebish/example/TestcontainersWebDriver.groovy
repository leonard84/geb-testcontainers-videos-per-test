package org.gebish.example

import static org.testcontainers.containers.BrowserWebDriverContainer.VncRecordingMode.RECORD_ALL
import static org.testcontainers.containers.BrowserWebDriverContainer.VncRecordingMode.RECORD_FAILING

import org.openqa.selenium.remote.RemoteWebDriver
import org.testcontainers.containers.BrowserWebDriverContainer
import org.testcontainers.containers.DefaultRecordingFileFactory
import org.testcontainers.containers.RecordingFileFactory
import org.testcontainers.containers.VncRecordingContainer

class TestcontainersWebDriver implements SafeFileName {

    private static final RecordingFileFactory recordingFileFactory = new DefaultRecordingFileFactory()
    static final VncRecordingContainer.VncRecordingFormat recordingFormat = VncRecordingContainer.VncRecordingFormat.MP4
    final File reportDir
    final BrowserWebDriverContainer container

    private final BrowserWebDriverContainer.VncRecordingMode recordingMode
    private VncRecordingContainer vncContainer

    TestcontainersWebDriver(BrowserWebDriverContainer container,
                            File reportDir,
                            BrowserWebDriverContainer.VncRecordingMode recordingMode) {
        this.reportDir = reportDir
        this.recordingMode = recordingMode
        this.container = container
        container.start()
    }

    @Delegate
    RemoteWebDriver getDriver() {
        container.webDriver
    }

    @Override
    void quit() {
        stopRecording()
        driver.quit()
        container.stop()
    }

    private VncRecordingContainer createVncContainer() {
        new VncRecordingContainer(container)
                .withVncPassword(container.password)
                .withVncPort(container.port)
                .withVideoFormat(recordingFormat)
    }

    void startRecording() {
        if (!vncContainer) {
            vncContainer = createVncContainer()
            container.webDriver.get("about:blank") //start browser with a blank page, clears website from previous test
            vncContainer.start()
        }
    }

    void stopRecording() {
        vncContainer?.stop()
        vncContainer = null
    }

    /**
     * Will save the recording based on the recordingMode and the succeeded flag.
     *
     * In any case it will stop the current recording session.
     *
     * @param prefix Name of the video
     * @param succeeded indicates whether the test was successfull
     * @return empty if nothing was saved; or the file of the video
     */
    Optional<File> retainRecordingIfNeeded(String prefix, boolean succeeded) {
        final boolean shouldRecord;
        switch (recordingMode) {
            case RECORD_ALL:
                shouldRecord = true;
                break;
            case RECORD_FAILING:
                shouldRecord = !succeeded;
                break;
            default:
                shouldRecord = false;
                break;
        }

        def result = Optional.empty()
        if (shouldRecord && vncContainer) {
            File recordingFile = recordingFileFactory.recordingFileForTest(reportDir, prefix, succeeded, recordingFormat)
            vncContainer.saveRecordingToFile(recordingFile)
            result = Optional.of(recordingFile)
        }

        stopRecording()
        return result
    }
}
