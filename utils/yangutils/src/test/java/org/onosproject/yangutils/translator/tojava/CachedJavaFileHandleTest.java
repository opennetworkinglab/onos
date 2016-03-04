/*
 * Copyright 2016 Open Networking Laboratory
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

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import org.onosproject.yangutils.datamodel.YangDataTypes;
import org.onosproject.yangutils.datamodel.YangType;
import org.onosproject.yangutils.translator.CachedFileHandle;
import org.onosproject.yangutils.translator.GeneratedFileType;
import org.onosproject.yangutils.utils.UtilConstants;
import org.onosproject.yangutils.utils.io.impl.CopyrightHeader;
import org.onosproject.yangutils.utils.io.impl.FileSystemUtil;

/**
 * Unit test case for cached java file handle.
 */
public class CachedJavaFileHandleTest {

    private static final String DIR_PKG = "target/unit/cachedfile/yangmodel/";
    private static final String PKG = "org.onosproject.unittest";
    private static final String CHILD_PKG = "target/unit/cachedfile/child";
    private static final String YANG_NAME = "Test1";
    private static final int GEN_TYPE = GeneratedFileType.GENERATE_INTERFACE_WITH_BUILDER;

    /**
     * Unit test case for add attribute info.
     *
     * @throws IOException when fails to add an attribute
     */
    @Test
    public void testForAddAttributeInfo() throws IOException {

        AttributeInfo attr = getAttr();
        attr.setListAttr(false);
        getFileHandle().addAttributeInfo(attr.getAttributeType(), attr.getAttributeName(), attr.isListAttr());
    }

    /**
     * Unit test case for close of cached files.
     *
     * @throws IOException when fails to generate files
     */
    @Test
    public void testForClose() throws IOException {

        CopyrightHeader.parseCopyrightHeader();

        AttributeInfo attr = getAttr();
        attr.setListAttr(false);
        CachedFileHandle handle = getFileHandle();
        handle.addAttributeInfo(attr.getAttributeType(), attr.getAttributeName(), attr.isListAttr());
        handle.close();

        assertThat(true, is(getStubDir().exists()));
        assertThat(true, is(getStubPkgInfo().exists()));
        assertThat(true, is(getStubInterfaceFile().exists()));
        assertThat(true, is(getStubBuilderFile().exists()));
    }

    /**
     * Returns attribute info.
     *
     * @return attribute info
     */
    @SuppressWarnings("rawtypes")
    private AttributeInfo getAttr() {
        YangType<?> type = new YangType();
        YangDataTypes dataType = YangDataTypes.STRING;

        type.setDataTypeName("string");
        type.setDataType(dataType);

        AttributeInfo attr = new AttributeInfo();

        attr.setAttributeName("testAttr");
        attr.setAttributeType(type);
        return attr;
    }

    /**
     * Returns cached java file handle.
     *
     * @return java file handle
     */
    private CachedFileHandle getFileHandle() throws IOException {
        CopyrightHeader.parseCopyrightHeader();
        FileSystemUtil.createPackage(DIR_PKG + File.separator + PKG, YANG_NAME);
        CachedFileHandle fileHandle = FileSystemUtil.createSourceFiles(PKG, YANG_NAME, GEN_TYPE);
        fileHandle.setRelativeFilePath(PKG.replace(".", "/"));
        fileHandle.setCodeGenFilePath(DIR_PKG);
        return fileHandle;
    }

    /**
     * Returns stub directory file object.
     *
     * @return stub directory file
     */
    private File getStubDir() {
        return new File(DIR_PKG);
    }

    /**
     * Returns stub package-info file object.
     *
     * @return stub package-info file
     */
    private File getStubPkgInfo() {
        return new File(DIR_PKG + PKG.replace(UtilConstants.PERIOD, UtilConstants.SLASH) + File.separator
                + "package-info.java");
    }

    /**
     * Returns stub interface file object.
     *
     * @return stub interface file
     */
    private File getStubInterfaceFile() {
        return new File(DIR_PKG + PKG.replace(UtilConstants.PERIOD, UtilConstants.SLASH) + File.separator + YANG_NAME
                + ".java");
    }

    /**
     * Returns stub builder file.
     *
     * @return stub builder file
     */
    private File getStubBuilderFile() {
        return new File(DIR_PKG + PKG.replace(UtilConstants.PERIOD, UtilConstants.SLASH) + File.separator + YANG_NAME
                + "Builder.java");
    }

}
