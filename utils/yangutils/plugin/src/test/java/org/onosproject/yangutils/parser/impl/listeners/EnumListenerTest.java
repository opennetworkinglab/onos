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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
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
import java.util.Iterator;
import java.util.ListIterator;
import java.util.SortedSet;

/**
 * Test cases for enum listener.
 */
public class EnumListenerTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private final YangUtilsParserManager manager = new YangUtilsParserManager();

    /**
     * Checks enum statement without value.
     */
    @Test
    public void processEnumTypeStatement() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/EnumTypeStatement.yang");

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

        SortedSet<YangEnum> enumSet = ((YangEnumeration) leafInfo.getDataType().getDataTypeExtendedInfo()).getEnumSet();
        for (YangEnum tmp : enumSet) {
            if (tmp.getNamedValue().equals("10m")) {
                assertThat(tmp.getValue(), is(0));
            } else if (tmp.getNamedValue().equals("100m")) {
                assertThat(tmp.getValue(), is(1));
            } else if (tmp.getNamedValue().equals("auto")) {
                assertThat(tmp.getValue(), is(2));
            }
        }
    }

    /**
     * Checks if enum with same name is not allowed.
     */
    @Test(expected = ParserException.class)
    public void processEnumWithDuplicateName() throws IOException, ParserException {
        YangNode node = manager.getDataModel("src/test/resources/EnumWithDuplicateName.yang");
    }

    /**
     * Checks enum boundary value.
     */
    @Test
    public void processEnumBoundaryValue() throws IOException, ParserException {
        thrown.expect(ParserException.class);
        thrown.expectMessage("YANG file error : value value 21474836472147483647 is not valid.");
        YangNode node = manager.getDataModel("src/test/resources/EnumBoundaryValue.yang");
    }

    /**
     * Checks whether exception is thrown if value is not specified following max enum value.
     */
    @Test
    public void processEnumMaxNextValue() throws IOException, ParserException {
        thrown.expect(ParserException.class);
        thrown.expectMessage("YANG file error : "
                + "An enum value MUST be specified for enum substatements following the one"
                + "with the current highest value");
        YangNode node = manager.getDataModel("src/test/resources/EnumMaxNextValue.yang");
    }

    /**
     * Checks enum values stored are sorted.
     */
    @Test
    public void processEnumSorted() throws IOException, ParserException {
        YangNode node = manager.getDataModel("src/test/resources/EnumSorted.yang");
        // Check whether the data model tree returned is of type module.
        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        ListIterator<YangLeaf> leafIterator = yangNode.getListOfLeaf().listIterator();
        YangLeaf leafInfo = leafIterator.next();

        assertThat(leafInfo.getName(), is("ifType"));
        assertThat(leafInfo.getDataType().getDataTypeName(), is("enumeration"));
        assertThat(leafInfo.getDataType().getDataType(), is(YangDataTypes.ENUMERATION));
        assertThat(((YangEnumeration) leafInfo.getDataType().getDataTypeExtendedInfo()).getName(),
                is("ifType_enum"));

        SortedSet<YangEnum> enumSet = ((YangEnumeration) leafInfo.getDataType().getDataTypeExtendedInfo()).getEnumSet();
        Iterator<YangEnum> enumIterator = enumSet.iterator();
        assertThat(enumIterator.next().getNamedValue(), is("five"));
    }
}
