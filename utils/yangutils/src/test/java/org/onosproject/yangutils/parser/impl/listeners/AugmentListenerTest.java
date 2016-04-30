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
import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yangutils.datamodel.YangModule;
import org.onosproject.yangutils.datamodel.YangAugment;
import org.onosproject.yangutils.datamodel.YangLeaf;
import org.onosproject.yangutils.datamodel.YangNodeIdentifier;
import org.onosproject.yangutils.datamodel.YangDataTypes;
import org.onosproject.yangutils.datamodel.YangNodeType;
import org.onosproject.yangutils.parser.exceptions.ParserException;
import org.onosproject.yangutils.parser.impl.YangUtilsParserManager;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * Test cases for testing augment listener functionality.
 */
public class AugmentListenerTest {

    private final YangUtilsParserManager manager = new YangUtilsParserManager();

    /**
     * Checks valid augment statement.
     */
    @Test
    public void processValidAugmentStatement() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/ValidAugmentStatement.yang");

        assertThat((node instanceof YangModule), is(true));
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        YangAugment yangAugment = (YangAugment) yangNode.getChild();
        ListIterator<YangNodeIdentifier> nodeIdentifierIterator = yangAugment.getTargetNode().listIterator();
        YangNodeIdentifier yangNodeIdentifier = nodeIdentifierIterator.next();
        assertThat(yangNodeIdentifier.getPrefix(), is("if"));
        assertThat(yangNodeIdentifier.getName(), is("interfaces"));

        ListIterator<YangLeaf> leafIterator = yangAugment.getListOfLeaf().listIterator();
        YangLeaf leafInfo = leafIterator.next();

        assertThat(leafInfo.getName(), is("ds0ChannelNumber"));
        assertThat(leafInfo.getDataType().getDataTypeName(), is("ChannelNumber"));
        assertThat(leafInfo.getDataType().getDataType(), is(YangDataTypes.DERIVED));
    }
}
