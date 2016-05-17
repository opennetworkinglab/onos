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
import org.onosproject.yangutils.datamodel.YangDataTypes;
import org.onosproject.yangutils.datamodel.YangEnum;
import org.onosproject.yangutils.datamodel.YangEnumeration;
import org.onosproject.yangutils.datamodel.YangLeaf;
import org.onosproject.yangutils.datamodel.YangModule;
import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yangutils.datamodel.YangNodeType;
import org.onosproject.yangutils.parser.exceptions.ParserException;
import org.onosproject.yangutils.parser.impl.YangUtilsParserManager;

import java.io.IOException;
import java.util.ListIterator;
import java.util.Set;

/**
 * Test cases for value listener.
 */
public class ValueListenerTest {

    private final YangUtilsParserManager manager = new YangUtilsParserManager();

    /**
     * Checks explicitly configured value.
     */
    @Test
    public void processValueStatement() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/ValueStatement.yang");

        // Check whether the data model tree returned is of type module.
        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        ListIterator<YangLeaf> leafIterator = yangNode.getListOfLeaf().listIterator();
        YangLeaf leafInfo = leafIterator.next();

        assertThat(leafInfo.getName(), is("speed"));
        assertThat(leafInfo.getDataType().getDataTypeName(), is("enumeration"));
        assertThat(leafInfo.getDataType().getDataType(), is(YangDataTypes.ENUMERATION));
        assertThat(((YangEnumeration) leafInfo.getDataType().getDataTypeExtendedInfo()).getName(),
                is("speed_enum"));

        Set<YangEnum> enumSet = ((YangEnumeration) leafInfo.getDataType().getDataTypeExtendedInfo()).getEnumSet();
        for (YangEnum tmp : enumSet) {
            if (tmp.getNamedValue().equals("10m")) {
                assertThat(tmp.getValue(), is(10));
            } else if (tmp.getNamedValue().equals("100m")) {
                assertThat(tmp.getValue(), is(100));
            } else if (tmp.getNamedValue().equals("auto")) {
                assertThat(tmp.getValue(), is(1000));
            }
        }
    }

    /**
     * Checks explicitly configured negative value.
     */
    @Test
    public void processValueStatementWithNegativeValue() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/ValueStatementWithNegativeValue.yang");

        // Check whether the data model tree returned is of type module.
        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        ListIterator<YangLeaf> leafIterator = yangNode.getListOfLeaf().listIterator();
        YangLeaf leafInfo = leafIterator.next();

        assertThat(leafInfo.getName(), is("speed"));
        assertThat(leafInfo.getDataType().getDataTypeName(), is("enumeration"));
        assertThat(leafInfo.getDataType().getDataType(), is(YangDataTypes.ENUMERATION));
        assertThat(((YangEnumeration) leafInfo.getDataType().getDataTypeExtendedInfo()).getName(),
                is("speed_enum"));

        Set<YangEnum> enumSet = ((YangEnumeration) leafInfo.getDataType().getDataTypeExtendedInfo()).getEnumSet();
        for (YangEnum tmp : enumSet) {
            if (tmp.getNamedValue().equals("10m")) {
                assertThat(tmp.getValue(), is(-2));
            } else if (tmp.getNamedValue().equals("100m")) {
                assertThat(tmp.getValue(), is(-1));
            } else if (tmp.getNamedValue().equals("auto")) {
                assertThat(tmp.getValue(), is(0));
            }
        }
    }

    /**
     * Checks explicitly configured value with double quotes.
     */
    @Test
    public void processValueStatementWithQuotes() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/ValueStatementWithQuotes.yang");

        // Check whether the data model tree returned is of type module.
        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        ListIterator<YangLeaf> leafIterator = yangNode.getListOfLeaf().listIterator();
        YangLeaf leafInfo = leafIterator.next();

        assertThat(leafInfo.getName(), is("speed"));
        assertThat(leafInfo.getDataType().getDataTypeName(), is("enumeration"));
        assertThat(leafInfo.getDataType().getDataType(), is(YangDataTypes.ENUMERATION));
        assertThat(((YangEnumeration) leafInfo.getDataType().getDataTypeExtendedInfo()).getName(),
                is("speed_enum"));

        Set<YangEnum> enumSet = ((YangEnumeration) leafInfo.getDataType().getDataTypeExtendedInfo()).getEnumSet();
        for (YangEnum tmp : enumSet) {
            if (tmp.getNamedValue().equals("10m")) {
                assertThat(tmp.getValue(), is(10));
            } else if (tmp.getNamedValue().equals("100m")) {
                assertThat(tmp.getValue(), is(100));
            } else if (tmp.getNamedValue().equals("auto")) {
                assertThat(tmp.getValue(), is(1000));
            }
        }
    }

    /**
     * Checks explicit value and auto generated value.
     */
    @Test
    public void processValueAndAutoStatement() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/ValueAndAutoStatement.yang");

        // Check whether the data model tree returned is of type module.
        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        ListIterator<YangLeaf> leafIterator = yangNode.getListOfLeaf().listIterator();
        YangLeaf leafInfo = leafIterator.next();

        assertThat(leafInfo.getName(), is("speed"));
        assertThat(leafInfo.getDataType().getDataTypeName(), is("enumeration"));
        assertThat(leafInfo.getDataType().getDataType(), is(YangDataTypes.ENUMERATION));
        assertThat(((YangEnumeration) leafInfo.getDataType().getDataTypeExtendedInfo()).getName(),
                is("speed_enum"));

        Set<YangEnum> enumSet = ((YangEnumeration) leafInfo.getDataType().getDataTypeExtendedInfo()).getEnumSet();
        for (YangEnum tmp : enumSet) {
            if (tmp.getNamedValue().equals("10m")) {
                assertThat(tmp.getValue(), is(10));
            } else if (tmp.getNamedValue().equals("100m")) {
                assertThat(tmp.getValue(), is(11));
            } else if (tmp.getNamedValue().equals("auto")) {
                assertThat(tmp.getValue(), is(1000));
            }
        }
    }

    /**
     * Checks explicit value should not be repeated.
     */
    @Test(expected = ParserException.class)
    public void processValueDuplication() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/ValueDuplication.yang");
    }

    /**
     * Checks explicit or auto generated value should not be repeated.
     */
    @Test(expected = ParserException.class)
    public void processValueExplicitAndAutoDuplication() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/ValueExplicitAndAutoDuplication.yang");
    }
}
