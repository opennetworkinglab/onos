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

import org.junit.Test;
import org.onosproject.yangutils.datamodel.YangModule;
import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yangutils.datamodel.YangNodeType;
import org.onosproject.yangutils.datamodel.YangRpc;
import org.onosproject.yangutils.datamodel.YangInput;
import org.onosproject.yangutils.datamodel.YangLeaf;
import org.onosproject.yangutils.datamodel.YangList;
import org.onosproject.yangutils.datamodel.YangContainer;
import org.onosproject.yangutils.datamodel.YangTypeDef;
import org.onosproject.yangutils.datamodel.YangStatusType;
import org.onosproject.yangutils.datamodel.YangDataTypes;
import org.onosproject.yangutils.parser.exceptions.ParserException;
import org.onosproject.yangutils.parser.impl.YangUtilsParserManager;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * Test cases for testing Input listener functionality.
 */
public class InputListenerTest {

    private final YangUtilsParserManager manager = new YangUtilsParserManager();

    /**
     * Checks input statements with data definition statements as sub-statements.
     */
    @Test
    public void processInputStatementWithDataDefinition() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/InputStatementWithDataDefinition.yang");

        assertThat((node instanceof YangModule), is(true));
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("rock"));

        YangRpc yangRpc = (YangRpc) yangNode.getChild();
        assertThat(yangRpc.getName(), is("activate-software-image"));

        YangInput yangInput = (YangInput) yangRpc.getChild();
        assertThat(yangInput.getName(), is("activate-software-image_input"));
        ListIterator<YangLeaf> leafIterator = yangInput.getListOfLeaf().listIterator();
        YangLeaf leafInfo = leafIterator.next();

        assertThat(leafInfo.getName(), is("image-name"));
        assertThat(leafInfo.getDataType().getDataTypeName(), is("string"));

        YangList yangList = (YangList) yangInput.getChild();
        assertThat(yangList.getName(), is("ospf"));
        assertThat(yangList.getKeyList().contains("invalid-interval"), is(true));
        assertThat(yangList.isConfig(), is(true));
        assertThat(yangList.getMaxElements(), is(10));
        assertThat(yangList.getMinElements(), is(3));
        leafIterator = yangList.getListOfLeaf().listIterator();
        leafInfo = leafIterator.next();
        assertThat(leafInfo.getName(), is("invalid-interval"));
        assertThat(leafInfo.getDataType().getDataTypeName(), is("uint16"));

        YangContainer yangContainer = (YangContainer) yangList.getNextSibling();
        assertThat(yangContainer.getName(), is("isis"));

        leafIterator = yangContainer.getListOfLeaf().listIterator();
        leafInfo = leafIterator.next();
        assertThat(leafInfo.getName(), is("invalid-interval"));
        assertThat(leafInfo.getDataType().getDataTypeName(), is("uint16"));
    }

    /**
     * Checks input statements with type-def statement as sub-statements.
     */
    @Test
    public void processInputStatementWithTypedef() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/InputStatementWithTypedef.yang");
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("rock"));

        YangRpc yangRpc = (YangRpc) yangNode.getChild();
        assertThat(yangRpc.getName(), is("activate-software-image"));

        YangInput yangInput = (YangInput) yangRpc.getChild();
        assertThat(yangInput.getName(), is("activate-software-image_input"));
        YangTypeDef typeDef = (YangTypeDef) yangInput.getChild();
        assertThat(typeDef.getName(), is("my-type"));
        assertThat(typeDef.getStatus(), is(YangStatusType.DEPRECATED));
        assertThat(typeDef.getTypeDefBaseType().getDataType(), is(YangDataTypes.INT32));
    }
}
