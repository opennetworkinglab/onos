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

import java.io.IOException;
import java.util.ListIterator;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.onosproject.yangutils.datamodel.YangDataTypes;
import org.onosproject.yangutils.datamodel.YangLeaf;
import org.onosproject.yangutils.datamodel.YangLeafList;
import org.onosproject.yangutils.datamodel.YangModule;
import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yangutils.datamodel.YangNodeType;
import org.onosproject.yangutils.parser.exceptions.ParserException;
import org.onosproject.yangutils.parser.impl.YangUtilsParserManager;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * Test case for type listener.
 */
public class TypeListenerTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private final YangUtilsParserManager manager = new YangUtilsParserManager();

    /**
     * Checks derived statement without contraints.
     */
    @Test
    public void processDerivedTypeStatement() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/DerivedTypeStatement.yang");

        // Check whether the data model tree returned is of type module.
        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        ListIterator<YangLeaf> leafIterator = yangNode.getListOfLeaf().listIterator();
        YangLeaf leafInfo = leafIterator.next();

        assertThat(leafInfo.getName(), is("invalid-interval"));
        assertThat(leafInfo.getDataType().getDataTypeName(), is("hello"));
        assertThat(leafInfo.getDataType().getDataType(), is(YangDataTypes.DERIVED));
    }

    /**
     * Checks valid yang data type.
     */
    @Test
    public void processIntegerTypeStatement() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/IntegerTypeStatement.yang");

        // Check whether the data model tree returned is of type module.
        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        ListIterator<YangLeaf> leafIterator = yangNode.getListOfLeaf().listIterator();
        YangLeaf leafInfo = leafIterator.next();

        assertThat(leafInfo.getName(), is("invalid-interval"));
        assertThat(leafInfo.getDataType().getDataTypeName(), is("uint16"));
        assertThat(leafInfo.getDataType().getDataType(), is(YangDataTypes.UINT16));
    }

    /**
     * Checks type for leaf-list.
     */
    @Test
    public void processLeafListSubStatementType() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/LeafListSubStatementType.yang");

        // Check whether the data model tree returned is of type module.
        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        ListIterator<YangLeafList> leafListIterator = yangNode.getListOfLeafList().listIterator();
        YangLeafList leafListInfo = leafListIterator.next();

        assertThat(leafListInfo.getName(), is("invalid-interval"));
        assertThat(leafListInfo.getDataType().getDataTypeName(), is("uint16"));
        assertThat(leafListInfo.getDataType().getDataType(), is(YangDataTypes.UINT16));
    }

    /**
     * Checks for unsupported type leafref.
     */
    @Test
    public void processLeafrefType() throws IOException, ParserException {

        thrown.expect(ParserException.class);
        thrown.expectMessage("YANG file error : \"leafref\" is not supported in current version,"
                + " please check wiki for YANG utils road map.");

        YangNode node = manager
                .getDataModel("src/test/resources/LeafrefInvalidIdentifier.yang");
    }

    /**
     * Checks for unsupported type identityref.
     */
    @Test
    public void processIdentityrefType() throws IOException, ParserException {

        thrown.expect(ParserException.class);
        thrown.expectMessage("YANG file error : \"identityref\" is not supported in current version,"
                + " please check wiki for YANG utils road map.");

        YangNode node = manager
                .getDataModel("src/test/resources/IdentityrefInvalidIdentifier.yang");
    }

    /**
     * Checks for unsupported type instance identifier.
     */
    @Test
    public void processInstanceIdentifierType() throws IOException, ParserException {

        thrown.expect(ParserException.class);
        thrown.expectMessage("YANG file error : \"instance-identifier\" is not supported in current version,"
                + " please check wiki for YANG utils road map.");

        YangNode node = manager
                .getDataModel("src/test/resources/InstanceIdentifierInvalidIdentifier.yang");
    }
}
