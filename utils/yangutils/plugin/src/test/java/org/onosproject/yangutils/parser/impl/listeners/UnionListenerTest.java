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
import java.util.List;
import java.util.ListIterator;
import org.junit.Test;
import org.onosproject.yangutils.datamodel.YangDataTypes;
import org.onosproject.yangutils.datamodel.YangLeaf;
import org.onosproject.yangutils.datamodel.YangLeafList;
import org.onosproject.yangutils.datamodel.YangList;
import org.onosproject.yangutils.datamodel.YangModule;
import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yangutils.datamodel.YangNodeType;
import org.onosproject.yangutils.datamodel.YangType;
import org.onosproject.yangutils.datamodel.YangUnion;
import org.onosproject.yangutils.parser.exceptions.ParserException;
import org.onosproject.yangutils.parser.impl.YangUtilsParserManager;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * Test cases for testing union listener.
 */
public class UnionListenerTest {

    private final YangUtilsParserManager manager = new YangUtilsParserManager();

    /**
     * Checks union when type is in leaf.
     */
    @Test
    public void processUnionWhenTypeInLeaf() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/UnionWhenTypeInLeaf.yang");

        // Check whether the data model tree returned is of type module.
        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        YangList yangList = (YangList) yangNode.getChild();
        assertThat(yangList.getName(), is("valid"));

        ListIterator<YangLeaf> leafIterator = yangList.getListOfLeaf().listIterator();
        YangLeaf leafInfo = leafIterator.next();

        assertThat(leafInfo.getName(), is("invalid-interval"));

        assertThat(leafInfo.getDataType().getDataType(), is(YangDataTypes.UNION));
        assertThat(leafInfo.getDataType().getDataTypeName(), is("union"));

        YangUnion yangUnion = (YangUnion) leafInfo.getDataType().getDataTypeExtendedInfo();

        List<YangType<?>> typeList = yangUnion.getTypeList();
        ListIterator<YangType<?>> typeListIterator = typeList.listIterator();
        YangType<?> yangType = typeListIterator.next();

        assertThat(yangType.getDataTypeName(), is("int32"));
        assertThat(yangType.getDataType(), is(YangDataTypes.INT32));

        YangType<?> yangTypeEnum = typeListIterator.next();

        assertThat(yangTypeEnum.getDataTypeName(), is("enumeration"));
        assertThat(yangTypeEnum.getDataType(), is(YangDataTypes.ENUMERATION));
    }

    /**
     * Checks union when type is in leaflist.
     */
    @Test
    public void processUnionWhenTypeInLeafList() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/UnionWhenTypeInLeafList.yang");

        // Check whether the data model tree returned is of type module.
        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        YangList yangList = (YangList) yangNode.getChild();
        assertThat(yangList.getName(), is("valid"));

        ListIterator<YangLeafList> leafListIterator = yangList.getListOfLeafList().listIterator();
        YangLeafList leafListInfo = leafListIterator.next();

        assertThat(leafListInfo.getName(), is("invalid-interval"));

        assertThat(leafListInfo.getDataType().getDataType(), is(YangDataTypes.UNION));
        assertThat(leafListInfo.getDataType().getDataTypeName(), is("union"));

        YangUnion yangUnion = (YangUnion) leafListInfo.getDataType().getDataTypeExtendedInfo();

        List<YangType<?>> typeList = yangUnion.getTypeList();
        ListIterator<YangType<?>> typeListIterator = typeList.listIterator();
        YangType<?> yangType = typeListIterator.next();

        assertThat(yangType.getDataTypeName(), is("int32"));
        assertThat(yangType.getDataType(), is(YangDataTypes.INT32));

        YangType<?> yangTypeEnum = typeListIterator.next();

        assertThat(yangTypeEnum.getDataTypeName(), is("enumeration"));
        assertThat(yangTypeEnum.getDataType(), is(YangDataTypes.ENUMERATION));
    }

    /**
     * Checks union with empty type.
     */
    @Test (expected = ParserException.class)
    public void processUnionWithEmptyType() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/UnionWithEmptyType.yang");
    }
}
