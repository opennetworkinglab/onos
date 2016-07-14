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

package org.onosproject.yangutils.parser.impl.listeners;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import org.junit.Test;
import org.onosproject.yangutils.datamodel.YangBit;
import org.onosproject.yangutils.datamodel.YangBits;
import org.onosproject.yangutils.datamodel.utils.builtindatatype.YangDataTypes;
import org.onosproject.yangutils.datamodel.YangLeaf;
import org.onosproject.yangutils.datamodel.YangModule;
import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yangutils.datamodel.YangNodeType;
import org.onosproject.yangutils.parser.exceptions.ParserException;
import org.onosproject.yangutils.parser.impl.YangUtilsParserManager;

import java.io.IOException;
import java.util.ListIterator;
import java.util.Map;

/**
 * Test cases for position listener.
 */
public class PositionListenerTest {

    private final YangUtilsParserManager manager = new YangUtilsParserManager();

    /**
     * Checks explicitly configured value.
     */
    @Test
    public void processPositionStatement() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/PositionStatement.yang");

        // Check whether the data model tree returned is of type module.
        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        ListIterator<YangLeaf> leafIterator = yangNode.getListOfLeaf().listIterator();
        YangLeaf leafInfo = leafIterator.next();

        assertThat(leafInfo.getName(), is("mybits"));
        assertThat(leafInfo.getDataType().getDataTypeName(), is("bits"));
        assertThat(leafInfo.getDataType().getDataType(), is(YangDataTypes.BITS));
        assertThat(((YangBits) leafInfo.getDataType().getDataTypeExtendedInfo()).getBitsName(),
                is("mybits"));

        // Check bit name map
        Map<String, YangBit> bitNameMap = ((YangBits) leafInfo.getDataType().getDataTypeExtendedInfo()).getBitNameMap();
        assertThat(bitNameMap.size(), is(3));
        for (Map.Entry<String, YangBit> element : bitNameMap.entrySet()) {
            String bitName = element.getKey();
            YangBit yangBit = element.getValue();
            if (bitName.equals("disable-nagle")) {
                assertThat(yangBit.getPosition(), is(0));
            } else if (bitName.equals("auto-sense-speed")) {
                assertThat(yangBit.getPosition(), is(1));
            } else if (bitName.equals("Ten-Mb-only")) {
                assertThat(yangBit.getPosition(), is(2));
            } else {
                throw new IOException("Invalid bit name: " + bitName);
            }
        }

        // Check bit position map
        Map<Integer, YangBit> bitPositionMap = ((YangBits) leafInfo.getDataType().getDataTypeExtendedInfo())
                                                                                 .getBitPositionMap();
        assertThat(bitPositionMap.size(), is(3));
        for (Map.Entry<Integer, YangBit> element : bitPositionMap.entrySet()) {
            int position = element.getKey();
            YangBit yangBit = element.getValue();
            if (position == 0) {
                assertThat(yangBit.getBitName(), is("disable-nagle"));
            } else if (position == 1) {
                assertThat(yangBit.getBitName(), is("auto-sense-speed"));
            } else if (position == 2) {
                assertThat(yangBit.getBitName(), is("Ten-Mb-only"));
            } else {
                throw new IOException("Invalid bit position: " + position);
            }
        }
    }

    /**
     * Checks position value with double quotes.
     */
    @Test
    public void processPositionWithDoubleQuotes() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/PositionWithDoubleQuotes.yang");

        // Check whether the data model tree returned is of type module.
        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        ListIterator<YangLeaf> leafIterator = yangNode.getListOfLeaf().listIterator();
        YangLeaf leafInfo = leafIterator.next();

        assertThat(leafInfo.getName(), is("mybits"));
        assertThat(leafInfo.getDataType().getDataTypeName(), is("bits"));
        assertThat(leafInfo.getDataType().getDataType(), is(YangDataTypes.BITS));
        assertThat(((YangBits) leafInfo.getDataType().getDataTypeExtendedInfo()).getBitsName(),
                is("mybits"));

        // Check bit name map
        Map<String, YangBit> bitNameMap = ((YangBits) leafInfo.getDataType().getDataTypeExtendedInfo()).getBitNameMap();
        assertThat(bitNameMap.size(), is(3));
        for (Map.Entry<String, YangBit> element : bitNameMap.entrySet()) {
            String bitName = element.getKey();
            YangBit yangBit = element.getValue();
            if (bitName.equals("disable-nagle")) {
                assertThat(yangBit.getPosition(), is(0));
            } else if (bitName.equals("auto-sense-speed")) {
                assertThat(yangBit.getPosition(), is(1));
            } else if (bitName.equals("Ten-Mb-only")) {
                assertThat(yangBit.getPosition(), is(2));
            } else {
                throw new IOException("Invalid bit name: " + bitName);
            }
        }

        // Check bit position map
        Map<Integer, YangBit> bitPositionMap = ((YangBits) leafInfo.getDataType().getDataTypeExtendedInfo())
                                                                                 .getBitPositionMap();
        assertThat(bitPositionMap.size(), is(3));
        for (Map.Entry<Integer, YangBit> element : bitPositionMap.entrySet()) {
            int position = element.getKey();
            YangBit yangBit = element.getValue();
            if (position == 0) {
                assertThat(yangBit.getBitName(), is("disable-nagle"));
            } else if (position == 1) {
                assertThat(yangBit.getBitName(), is("auto-sense-speed"));
            } else if (position == 2) {
                assertThat(yangBit.getBitName(), is("Ten-Mb-only"));
            } else {
                throw new IOException("Invalid bit position: " + position);
            }
        }
    }

    /**
     * Checks explicit value and auto generated value.
     */
    @Test
    public void processPositionImplicitAndExplicit() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/PositionImplicitAndExplicit.yang");

        // Check whether the data model tree returned is of type module.
        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        ListIterator<YangLeaf> leafIterator = yangNode.getListOfLeaf().listIterator();
        YangLeaf leafInfo = leafIterator.next();

        assertThat(leafInfo.getName(), is("mybits"));
        assertThat(leafInfo.getDataType().getDataTypeName(), is("bits"));
        assertThat(leafInfo.getDataType().getDataType(), is(YangDataTypes.BITS));
        assertThat(((YangBits) leafInfo.getDataType().getDataTypeExtendedInfo()).getBitsName(),
                is("mybits"));

        // Check bit name map
        Map<String, YangBit> bitNameMap = ((YangBits) leafInfo.getDataType().getDataTypeExtendedInfo()).getBitNameMap();
        assertThat(bitNameMap.size(), is(3));
        for (Map.Entry<String, YangBit> element : bitNameMap.entrySet()) {
            String bitName = element.getKey();
            YangBit yangBit = element.getValue();
            if (bitName.equals("disable-nagle")) {
                assertThat(yangBit.getPosition(), is(0));
            } else if (bitName.equals("auto-sense-speed")) {
                assertThat(yangBit.getPosition(), is(1));
            } else if (bitName.equals("Ten-Mb-only")) {
                assertThat(yangBit.getPosition(), is(2));
            } else {
                throw new IOException("Invalid bit name: " + bitName);
            }
        }

        // Check bit position map
        Map<Integer, YangBit> bitPositionMap = ((YangBits) leafInfo.getDataType().getDataTypeExtendedInfo())
                                                                                 .getBitPositionMap();
        assertThat(bitPositionMap.size(), is(3));
        for (Map.Entry<Integer, YangBit> element : bitPositionMap.entrySet()) {
            int position = element.getKey();
            YangBit yangBit = element.getValue();
            if (position == 0) {
                assertThat(yangBit.getBitName(), is("disable-nagle"));
            } else if (position == 1) {
                assertThat(yangBit.getBitName(), is("auto-sense-speed"));
            } else if (position == 2) {
                assertThat(yangBit.getBitName(), is("Ten-Mb-only"));
            } else {
                throw new IOException("Invalid bit position: " + position);
            }
        }
    }

    /**
     * Checks explicit value should not be repeated.
     */
    @Test(expected = ParserException.class)
    public void processPositionDuplication() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/PositionDuplication.yang");
    }

    /**
     * Checks explicit or auto generated value should not be repeated.
     */
    @Test(expected = ParserException.class)
    public void processPositionImplicitAndExplicitDuplication() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/PositionImplicitAndExplicitDuplication.yang");
    }

    /**
     * Checks if negative value of position is not allowed.
     */
    @Test(expected = ParserException.class)
    public void processPositionNegativeValue() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/PositionNegativeValue.yang");
    }
}
