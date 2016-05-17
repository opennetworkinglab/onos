/*
 * Copyright 2016-present Open Networking Laboratory
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

package org.onosproject.yangutils.translator.tojava;

import java.io.File;
import java.io.IOException;

import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yangutils.translator.tojava.utils.JavaExtendsListHolder;

import static org.onosproject.yangutils.translator.tojava.utils.JavaFileGenerator.generateEventFile;
import static org.onosproject.yangutils.translator.tojava.utils.JavaIdentifierSyntax.getCapitalCase;
import static org.onosproject.yangutils.translator.tojava.utils.TempJavaCodeFragmentFilesUtils.closeFile;
import static org.onosproject.yangutils.utils.io.impl.FileSystemUtil.createPackage;
import static org.onosproject.yangutils.utils.io.impl.YangIoUtils.getAbsolutePackagePath;

/**
 * Represents implementation of java bean code fragments temporary implementations.
 * Maintains the temp files required specific for event java snippet generation.
 */
public class TempJavaEventFragmentFiles
        extends TempJavaFragmentFiles {

    /**
     * File name for generated class file for special type like union, typedef
     * suffix.
     */
    private static final String EVENT_FILE_NAME_SUFFIX = "Event";

    /**
     * Java file handle for event file.
     */
    private File eventJavaFileHandle;

    /**
     * Creates an instance of temporary java code fragment.
     *
     * @param javaFileInfo generated java file info
     * @throws IOException when fails to create new file handle
     */
    public TempJavaEventFragmentFiles(JavaFileInfo javaFileInfo)
            throws IOException {
        setJavaExtendsListHolder(new JavaExtendsListHolder());
        setJavaImportData(new JavaImportData());
        setJavaFileInfo(javaFileInfo);

        setAbsoluteDirPath(getAbsolutePackagePath(getJavaFileInfo().getBaseCodeGenPath(),
                getJavaFileInfo().getPackageFilePath()));

    }

    /**
     * Returns event's java file handle.
     *
     * @return java file handle
     */
    private File getEventJavaFileHandle() {
        return eventJavaFileHandle;
    }

    /**
     * Sets event's java file handle.
     *
     * @param eventJavaFileHandle file handle for event
     */
    private void setEventJavaFileHandle(File eventJavaFileHandle) {
        this.eventJavaFileHandle = eventJavaFileHandle;
    }

    /**
     * Constructs java code exit.
     *
     * @param fileType generated file type
     * @param curNode current YANG node
     * @throws IOException when fails to generate java files
     */
    @Override
    public void generateJavaFile(int fileType, YangNode curNode)
            throws IOException {

        createPackage(curNode);
        String parentInfo = getCapitalCase(((JavaFileInfoContainer) curNode.getParent())
                .getJavaFileInfo().getJavaName());

        /**
         * Creates event interface file.
         */
        setEventJavaFileHandle(getJavaFileHandle(parentInfo + EVENT_FILE_NAME_SUFFIX));
        generateEventFile(getEventJavaFileHandle(), curNode, null);

        /**
         * Close all the file handles.
         */
        freeTemporaryResources(false);
    }

    /**
     * Removes all temporary file handles.
     *
     * @param isErrorOccurred when translator fails to generate java files we
     * need to close all open file handles include temporary files
     * and java files.
     * @throws IOException when failed to delete the temporary files
     */
    @Override
    public void freeTemporaryResources(boolean isErrorOccurred)
            throws IOException {
        boolean isError = isErrorOccurred;
        /**
         * Close all java file handles and when error occurs delete the files.
         */
        closeFile(getEventJavaFileHandle(), isError);

        super.freeTemporaryResources(isErrorOccurred);

    }
}
