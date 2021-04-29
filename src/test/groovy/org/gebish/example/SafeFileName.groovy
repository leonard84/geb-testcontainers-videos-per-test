package org.gebish.example

trait SafeFileName {
    String getFilesystemFriendlyName(String testName) {
        def name = testName.replaceAll(/[^\w-]/, "_")
        if (name.size() > 220) { // filename limit of 255 allow for prefix and suffixes
            name = name.substring(0, 220 - 3) + "___"
        }
        return name
    }
}
