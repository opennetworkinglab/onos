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

package org.onosproject.yangutils.parser.impl;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yangutils.parser.exceptions.ParserException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Test case for testing YANG utils parser manager.
 */
public class YangUtilsParserManagerTest {

    YangUtilsParserManager manager = new YangUtilsParserManager();
    File file;
    BufferedWriter out;

    @Before
    public void setUp() throws Exception {
        file = new File("demo.yang");
        out = new BufferedWriter(new FileWriter(file));
    }
    @After
    public void tearDown() throws Exception {
        file.delete();
    }

    /**
     * This test case checks whether the null pointer exception is generated
     * when the input YANG file is null.
     */
    @Test(expected = NullPointerException.class)
    public void getDataModelNullFileTest() throws IOException, ParserException {
        YangUtilsParserManager manager = new YangUtilsParserManager();
        YangNode node = manager.getDataModel(null);
    }

    /**
     * This test case checks whether the io exception is generated
     * when the input YANG file is non existent.
     */
    @Test(expected = ParserException.class)
    public void getDataModelNonExistentFileTest() throws IOException, ParserException {

        YangUtilsParserManager manager = new YangUtilsParserManager();
        YangNode node = manager.getDataModel("nonexistent.yang");
    }

    /**
     * This test case checks if the input YANG file is correct no exception
     * should be generated.
     */
    @Test
    public void getDataModelCorrectFileTest() throws IOException, ParserException {

        out.write("module ONOS {\n");
        out.write("yang-version 1;\n");
        out.write("namespace urn:ietf:params:xml:ns:yang:ietf-ospf;\n");
        out.write("prefix On;\n");
        out.write("}\n");
        out.close();

        YangNode node = manager.getDataModel("demo.yang");
    }

    /**
     * This test case checks if the input YANG file with wrong YANG constructs
     * than parser exception should be generated.
     */
    @Test(expected = ParserException.class)
    public void getDataModelIncorrectFileTest() throws IOException, ParserException {

        out.write("module ONOS {\n");
        out.write("yang-version 1\n");
        out.write("namespace urn:ietf:params:xml:ns:yang:ietf-ospf;\n");
        out.write("prefix On;\n");
        out.write("}\n");
        out.close();

        YangNode node = manager.getDataModel("demo.yang");
    }
}