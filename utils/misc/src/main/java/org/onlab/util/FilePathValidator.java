/*
 * Copyright 2018-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onlab.util;

import java.io.File;
import java.io.IOException;
import java.util.zip.ZipEntry;

/**
 * Utilities for validation of Zip files.
 */
public final class FilePathValidator {

    /**
     * Do not allow construction.
     */
    private FilePathValidator() {
    }

    /**
     * Validates a File. Checks that the file being created does not
     * lie outside the target directory.
     *
     * @param destinationFile file to check
     * @param destinationDir target directory
     * @return true if the Entry resolves to a file inside the target directory; false otherwise
     */
    public static boolean validateFile(File destinationFile, File destinationDir) {
        try {
            String canonicalDestinationDirPath = destinationDir.getCanonicalPath();
            String canonicalDestinationFile = destinationFile.getCanonicalPath();
            return canonicalDestinationFile.startsWith(canonicalDestinationDirPath + File.separator);
        } catch (IOException ioe) {
            return false;
        }
    }

    /**
     * Validates a zip entry. Checks that the file being created does not
     * lie outside the target directory.
     *
     * See https://snyk.io/research/zip-slip-vulnerability for more information.
     *
     * @param entry ZipEntry to check
     * @param destinationDir target directory
     * @return true if the Entry resolves to a file inside the target directory; false otherwise
     */
    public static boolean validateZipEntry(ZipEntry entry, File destinationDir) {
        try {
            String canonicalDestinationDirPath = destinationDir.getCanonicalPath();
            File destinationFile = new File(destinationDir, entry.getName());
            String canonicalDestinationFile = destinationFile.getCanonicalPath();
            return canonicalDestinationFile.startsWith(canonicalDestinationDirPath + File.separator);
        } catch (IOException ioe) {
            return false;
        }
    }

}
