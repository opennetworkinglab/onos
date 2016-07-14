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

import org.onosproject.yangutils.utils.io.impl.YangPluginConfig;

import static org.onosproject.yangutils.translator.tojava.GeneratedTempFileType.CONSTRUCTOR_IMPL_MASK;
import static org.onosproject.yangutils.translator.tojava.utils.MethodsGenerator.getConstructor;
import static org.onosproject.yangutils.translator.tojava.utils.TempJavaCodeFragmentFilesUtils.closeFile;

/**
 * Represents implementation of java bean code fragments temporary implementations.
 * Maintains the temp files required specific for bean java snippet generation.
 */
public class TempJavaBeanFragmentFiles
        extends TempJavaFragmentFiles {

    /**
     * File name for constructor.
     */
    private static final String CONSTRUCTOR_FILE_NAME = "Constructor";

    /**
     * Temporary file handle for constructor of class.
     */
    private File constructorImplTempFileHandle;

    /**
     * Creates an instance of temporary java code fragment.
     *
     * @param javaFileInfo generated java file info
     * @throws IOException when fails to create new file handle
     */
    public TempJavaBeanFragmentFiles(JavaFileInfo javaFileInfo)
            throws IOException {

        super(javaFileInfo);

        /*
         * Initialize getterImpl, attributes, constructor, hash code, equals and
         * to strings when generation file type matches to impl class mask.
         */
        addGeneratedTempFile(CONSTRUCTOR_IMPL_MASK);

        setConstructorImplTempFileHandle(getTemporaryFileHandle(CONSTRUCTOR_FILE_NAME));
    }

    /**
     * Returns constructor's temporary file handle.
     *
     * @return temporary file handle
     */
    public File getConstructorImplTempFileHandle() {
        return constructorImplTempFileHandle;
    }

    /**
     * Sets to constructor's temporary file handle.
     *
     * @param constructor file handle for to constructor
     */
    private void setConstructorImplTempFileHandle(File constructor) {
        constructorImplTempFileHandle = constructor;
    }

    /**
     * Adds constructor for class.
     *
     * @param attr attribute info
     * @throws IOException when fails to append to temporary file
     */
    private void addConstructor(JavaAttributeInfo attr, YangPluginConfig pluginConfig)
            throws IOException {
        appendToFile(getConstructorImplTempFileHandle(), getConstructor(getGeneratedJavaClassName(), attr,
                getGeneratedJavaFiles(), pluginConfig));
    }

    /**
     * Adds the new attribute info to the target generated temporary files.
     *
     * @param newAttrInfo the attribute info that needs to be added to temporary
     * files
     * @throws IOException IO operation fail
     */
    @Override
    void addJavaSnippetInfoToApplicableTempFiles(JavaAttributeInfo newAttrInfo, YangPluginConfig pluginConfig)
            throws IOException {
        super.addJavaSnippetInfoToApplicableTempFiles(newAttrInfo, pluginConfig);
        addConstructor(newAttrInfo, pluginConfig);
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

        /*
         * Close constructor temporary file handle and delete the file.
         */
        closeFile(getConstructorImplTempFileHandle(), true);

        super.freeTemporaryResources(isErrorOccurred);
    }

}
