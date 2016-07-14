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
import org.onosproject.yangutils.datamodel.YangDerivedInfo;
import org.onosproject.yangutils.datamodel.utils.builtindatatype.YangDataTypes;
import org.onosproject.yangutils.datamodel.YangLeaf;
import org.onosproject.yangutils.datamodel.YangModule;
import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yangutils.datamodel.YangNodeType;
import org.onosproject.yangutils.datamodel.YangType;
import org.onosproject.yangutils.datamodel.YangTypeDef;
import org.onosproject.yangutils.datamodel.YangUnion;
import org.onosproject.yangutils.parser.exceptions.ParserException;
import org.onosproject.yangutils.parser.impl.YangUtilsParserManager;

import java.io.IOException;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

/**
 * Test cases for bit listener.
 */
public class BitListenerTest {

    private final YangUtilsParserManager manager = new YangUtilsParserManager();

    /**
     * Checks bit statement without position.
     */
    @Test
    public void processBitTypeStatement() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/BitTypeStatement.yang");

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
     * Checks bit statement with typedef.
     */
    @Test
    public void processBitTypedefStatement() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/BitTypedefStatement.yang");

        // Check whether the data model tree returned is of type module.
        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        YangTypeDef typedef = (YangTypeDef) yangNode.getChild();
        assertThat(typedef.getName(), is("type15"));

        YangType type = typedef.getTypeList().iterator().next();
        assertThat(type.getDataType(), is(YangDataTypes.BITS));
        assertThat(type.getDataTypeName(), is("bits"));

        // Check bit name map
        Map<String, YangBit> bitNameMap = ((YangBits) type.getDataTypeExtendedInfo()).getBitNameMap();
        assertThat(bitNameMap.size(), is(3));
        for (Map.Entry<String, YangBit> element : bitNameMap.entrySet()) {
            String bitName = element.getKey();
            YangBit yangBit = element.getValue();
            if (bitName.equals("disable-nagle")) {
                assertThat(yangBit.getPosition(), is(0));
            } else if (bitName.equals("auto-sense-speed")) {
                assertThat(yangBit.getPosition(), is(1));
            } else if (bitName.equals("Mb-only")) {
                assertThat(yangBit.getPosition(), is(2));
            } else {
                throw new IOException("Invalid bit name: " + bitName);
            }
        }

        // Check bit position map
        Map<Integer, YangBit> bitPositionMap = ((YangBits) type.getDataTypeExtendedInfo()).getBitPositionMap();
        assertThat(bitPositionMap.size(), is(3));
        for (Map.Entry<Integer, YangBit> element : bitPositionMap.entrySet()) {
            int position = element.getKey();
            YangBit yangBit = element.getValue();
            if (position == 0) {
                assertThat(yangBit.getBitName(), is("disable-nagle"));
            } else if (position == 1) {
                assertThat(yangBit.getBitName(), is("auto-sense-speed"));
            } else if (position == 2) {
                assertThat(yangBit.getBitName(), is("Mb-only"));
            } else {
                throw new IOException("Invalid bit position: " + position);
            }
        }
    }

    /**
     * Checks bit statement with typedef with referred leaf.
     */
    @Test
    public void processBitTypedefReferredLeafStatement() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/BitTypedefReferredLeafStatement.yang");

        // Check whether the data model tree returned is of type module.
        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        YangTypeDef typedef = (YangTypeDef) yangNode.getChild();
        assertThat(typedef.getName(), is("topBits"));

        YangType type = typedef.getTypeList().iterator().next();
        assertThat(type.getDataType(), is(YangDataTypes.BITS));
        assertThat(type.getDataTypeName(), is("bits"));

        // Check bit name map
        Map<String, YangBit> bitNameMap = ((YangBits) type.getDataTypeExtendedInfo()).getBitNameMap();
        assertThat(bitNameMap.size(), is(3));
        for (Map.Entry<String, YangBit> element : bitNameMap.entrySet()) {
            String bitName = element.getKey();
            YangBit yangBit = element.getValue();
            if (bitName.equals("disable-nagle")) {
                assertThat(yangBit.getPosition(), is(0));
            } else if (bitName.equals("auto-sense-speed")) {
                assertThat(yangBit.getPosition(), is(1));
            } else if (bitName.equals("Mb-only")) {
                assertThat(yangBit.getPosition(), is(2));
            } else {
                throw new IOException("Invalid bit name: " + bitName);
            }
        }

        // Check bit position map
        Map<Integer, YangBit> bitPositionMap = ((YangBits) type.getDataTypeExtendedInfo()).getBitPositionMap();
        assertThat(bitPositionMap.size(), is(3));
        for (Map.Entry<Integer, YangBit> element : bitPositionMap.entrySet()) {
            int position = element.getKey();
            YangBit yangBit = element.getValue();
            if (position == 0) {
                assertThat(yangBit.getBitName(), is("disable-nagle"));
            } else if (position == 1) {
                assertThat(yangBit.getBitName(), is("auto-sense-speed"));
            } else if (position == 2) {
                assertThat(yangBit.getBitName(), is("Mb-only"));
            } else {
                throw new IOException("Invalid bit position: " + position);
            }
        }

        // Check leaf reffered typedef
        ListIterator<YangLeaf> leafIterator = yangNode.getListOfLeaf().listIterator();
        YangLeaf leafInfo = leafIterator.next();

        assertThat(leafInfo.getName(), is("myBits"));
        assertThat(leafInfo.getDataType().getDataTypeName(), is("topBits"));
        assertThat(leafInfo.getDataType().getDataType(), is(YangDataTypes.DERIVED));
        YangType<YangDerivedInfo> typeDerived = (YangType<YangDerivedInfo>) leafInfo.getDataType();
        YangDerivedInfo derivedInfo = (YangDerivedInfo) typeDerived.getDataTypeExtendedInfo();
        YangTypeDef prevTypedef = (YangTypeDef) derivedInfo.getReferredTypeDef();
        assertThat(prevTypedef.getName(), is("topBits"));
        YangType topType = prevTypedef.getTypeList().iterator().next();
        assertThat(topType.getDataType(), is(YangDataTypes.BITS));
        assertThat(topType.getDataTypeName(), is("bits"));
        YangType<YangBits> typeBits = (YangType<YangBits>) topType;
        YangBits bits = typeBits.getDataTypeExtendedInfo();

        // Check bit name map
        bitNameMap = bits.getBitNameMap();
        assertThat(bitNameMap.size(), is(3));
        for (Map.Entry<String, YangBit> element : bitNameMap.entrySet()) {
            String bitName = element.getKey();
            YangBit yangBit = element.getValue();
            if (bitName.equals("disable-nagle")) {
                assertThat(yangBit.getPosition(), is(0));
            } else if (bitName.equals("auto-sense-speed")) {
                assertThat(yangBit.getPosition(), is(1));
            } else if (bitName.equals("Mb-only")) {
                assertThat(yangBit.getPosition(), is(2));
            } else {
                throw new IOException("Invalid bit name: " + bitName);
            }
        }

        // Check bit position map
        bitPositionMap = bits.getBitPositionMap();
        assertThat(bitPositionMap.size(), is(3));
        for (Map.Entry<Integer, YangBit> element : bitPositionMap.entrySet()) {
            int position = element.getKey();
            YangBit yangBit = element.getValue();
            if (position == 0) {
                assertThat(yangBit.getBitName(), is("disable-nagle"));
            } else if (position == 1) {
                assertThat(yangBit.getBitName(), is("auto-sense-speed"));
            } else if (position == 2) {
                assertThat(yangBit.getBitName(), is("Mb-only"));
            } else {
                throw new IOException("Invalid bit position: " + position);
            }
        }
    }

    /**
     * Checks bit statement with union.
     */
    @Test
    public void processBitUnionStatement() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/BitUnionStatement.yang");

        // Check whether the data model tree returned is of type module.
        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        ListIterator<YangLeaf> leafIterator = yangNode.getListOfLeaf().listIterator();
        YangLeaf leafInfo = leafIterator.next();

        assertThat(leafInfo.getName(), is("type15"));

        assertThat(leafInfo.getDataType().getDataType(), is(YangDataTypes.UNION));
        assertThat(leafInfo.getDataType().getDataTypeName(), is("union"));

        YangUnion yangUnion = (YangUnion) leafInfo.getDataType().getDataTypeExtendedInfo();

        List<YangType<?>> typeList = yangUnion.getTypeList();
        ListIterator<YangType<?>> typeListIterator = typeList.listIterator();
        YangType<?> yangType = typeListIterator.next();

        assertThat(yangType.getDataType(), is(YangDataTypes.BITS));
        assertThat(yangType.getDataTypeName(), is("bits"));

        // Check bit name map
        Map<String, YangBit> bitNameMap = ((YangBits) yangType.getDataTypeExtendedInfo()).getBitNameMap();
        assertThat(bitNameMap.size(), is(3));
        for (Map.Entry<String, YangBit> element : bitNameMap.entrySet()) {
            String bitName = element.getKey();
            YangBit yangBit = element.getValue();
            if (bitName.equals("disable-nagle")) {
                assertThat(yangBit.getPosition(), is(0));
            } else if (bitName.equals("auto-sense-speed")) {
                assertThat(yangBit.getPosition(), is(1));
            } else if (bitName.equals("Mb-only")) {
                assertThat(yangBit.getPosition(), is(2));
            } else {
                throw new IOException("Invalid bit name: " + bitName);
            }
        }

        // Check bit position map
        Map<Integer, YangBit> bitPositionMap = ((YangBits) yangType.getDataTypeExtendedInfo()).getBitPositionMap();
        assertThat(bitPositionMap.size(), is(3));
        for (Map.Entry<Integer, YangBit> element : bitPositionMap.entrySet()) {
            int position = element.getKey();
            YangBit yangBit = element.getValue();
            if (position == 0) {
                assertThat(yangBit.getBitName(), is("disable-nagle"));
            } else if (position == 1) {
                assertThat(yangBit.getBitName(), is("auto-sense-speed"));
            } else if (position == 2) {
                assertThat(yangBit.getBitName(), is("Mb-only"));
            } else {
                throw new IOException("Invalid bit position: " + position);
            }
        }
    }

    /**
     * Checks if enum with same name is not allowed.
     */
    @Test(expected = ParserException.class)
    public void processBitWithDuplicateName() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/BitWithDuplicateName.yang");
    }
}
