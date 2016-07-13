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

import org.onosproject.yangutils.datamodel.YangContainer;
import org.onosproject.yangutils.datamodel.utils.builtindatatype.YangDataTypes;
import org.onosproject.yangutils.datamodel.YangLeaf;
import org.onosproject.yangutils.datamodel.YangLeafRef;
import org.onosproject.yangutils.datamodel.YangModule;
import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yangutils.datamodel.YangNodeType;
import org.onosproject.yangutils.datamodel.YangType;
import org.onosproject.yangutils.parser.exceptions.ParserException;
import org.onosproject.yangutils.parser.impl.YangUtilsParserManager;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
/**
 * Test cases for require-instance listener.
 */
public class RequireInstanceListenerTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private final YangUtilsParserManager manager = new YangUtilsParserManager();

    /**
     * Checks require-statement with true as status.
     */
    @Test
    public void processRequireInstanceTrue() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/RequireInstanceTrue.yang");

        // Check whether the data model tree returned is of type module.
        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("PathListener"));

        YangContainer container = (YangContainer) yangNode.getChild().getNextSibling();
        ListIterator<YangLeaf> leafIterator = container.getListOfLeaf().listIterator();
        YangLeaf leafInfo = leafIterator.next();

        // Check whether the require-instance value is set correctly in leafref.
        assertThat(leafInfo.getName(), is("ifname"));
        YangLeafRef yangLeafRef = (YangLeafRef) leafInfo.getDataType().getDataTypeExtendedInfo();
        assertThat(yangLeafRef.getRequireInstance(), is(true));
    }

    /**
     * Checks require-statement with false as status.
     */
    @Test
    public void processRequireInstanceFalse() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/RequireInstanceFalse.yang");

        // Check whether the data model tree returned is of type module.
        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("PathListener"));

        ListIterator<YangLeaf> leafIterator = yangNode.getListOfLeaf().listIterator();
        YangLeaf leafInfo = leafIterator.next();

        // Check whether the require-instance value is set correctly in instance-identifier.
        assertThat(leafInfo.getName(), is("admin-status"));

        YangType type = leafInfo.getDataType();

        assertThat(type.getDataType(), is(YangDataTypes.INSTANCE_IDENTIFIER));
        boolean status = ((YangType<Boolean>) type).getDataTypeExtendedInfo();

        assertThat(status, is(false));
    }

    /**
     * Checks require-statement default value when its not there in YANG under instance-identifier.
     */
    @Test
    public void processRequireInstanceDefaultValueInInstanceIdentifier() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/RequireInstanceDefaultValueInInstanceIdentifier.yang");

        // Check whether the data model tree returned is of type module.
        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("PathListener"));

        ListIterator<YangLeaf> leafIterator = yangNode.getListOfLeaf().listIterator();
        YangLeaf leafInfo = leafIterator.next();

        // Check whether the require-instance value is set correctly in instance-identifier.
        assertThat(leafInfo.getName(), is("admin-status"));

        YangType type = leafInfo.getDataType();

        assertThat(type.getDataType(), is(YangDataTypes.INSTANCE_IDENTIFIER));

        boolean status = ((YangType<Boolean>) type).getDataTypeExtendedInfo();
        assertThat(status, is(true));
    }

    /**
     * Checks require-statement default value when its not there in YANG under leafref.
     */
    @Test
    public void processRequireInstanceDefaultValueForLeafref() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/RequireInstanceDefaultValueForLeafref.yang");

        // Check whether the data model tree returned is of type module.
        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("PathListener"));

        YangContainer container = (YangContainer) yangNode.getChild().getNextSibling();
        ListIterator<YangLeaf> leafIterator = container.getListOfLeaf().listIterator();
        YangLeaf leafInfo = leafIterator.next();

        // Check whether the require-instance value is set correctly in leafref.
        assertThat(leafInfo.getName(), is("ifname"));
        YangLeafRef yangLeafRef = (YangLeafRef) leafInfo.getDataType().getDataTypeExtendedInfo();
        assertThat(yangLeafRef.getRequireInstance(), is(true));
    }
}
