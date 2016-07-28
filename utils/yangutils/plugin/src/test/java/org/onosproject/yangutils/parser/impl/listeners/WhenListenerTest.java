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
import org.onosproject.yangutils.datamodel.YangContainer;
import org.onosproject.yangutils.datamodel.YangLeaf;
import org.onosproject.yangutils.datamodel.YangList;
import org.onosproject.yangutils.datamodel.YangModule;
import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yangutils.parser.exceptions.ParserException;
import org.onosproject.yangutils.parser.impl.YangUtilsParserManager;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * Test cases for testing when listener functionality.
 */
public class WhenListenerTest {

    private final YangUtilsParserManager manager = new YangUtilsParserManager();

    /**
     * Checks if when listener updates the data model.
     */
    @Test
    public void processContainerSubStatementWhen() throws IOException, ParserException {
        YangNode node = manager.getDataModel("src/test/resources/ContainerSubStatementWhen.yang");

        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        YangList yangList = (YangList) yangNode.getChild();
        String expectedConstraint = "../switching-capability = 'TDM'";
        assertThat(yangList.getName(), is("interface-switching-capability"));
        assertThat(yangList.getWhen().getCondition(), is(expectedConstraint));

        YangContainer container = (YangContainer) yangList.getNextSibling();
        assertThat(container.getName(), is("time-division-multiplex-capable"));
        assertThat(container.getWhen().getCondition(), is(expectedConstraint));
    }

    /**
     * Checks if when listener updates the data model.
     */
    @Test
    public void processLeafSubStatementWhen() throws IOException, ParserException {
        YangNode node = manager.getDataModel("src/test/resources/LeafSubStatementWhen.yang");

        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        ListIterator<YangLeaf> leafIterator = yangNode.getListOfLeaf().listIterator();
        YangLeaf leafInfo = leafIterator.next();

        assertThat(leafInfo.getWhen().getCondition(), is("ifType != 'ethernet'"));
    }
}