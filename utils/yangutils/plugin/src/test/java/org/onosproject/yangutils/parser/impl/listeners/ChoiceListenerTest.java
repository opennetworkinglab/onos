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
import org.onosproject.yangutils.datamodel.YangChoice;
import org.onosproject.yangutils.datamodel.YangContainer;
import org.onosproject.yangutils.datamodel.YangModule;
import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yangutils.datamodel.YangNodeType;
import org.onosproject.yangutils.parser.exceptions.ParserException;
import org.onosproject.yangutils.parser.impl.YangUtilsParserManager;

import java.io.IOException;

/**
 * Test cases for choice listener.
 */
public class ChoiceListenerTest {

    private final YangUtilsParserManager manager = new YangUtilsParserManager();

    /**
     * Checks choice statement without body.
     */
    @Test
    public void processChoiceStatementWithoutBody() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/ChoiceStatementWithoutBody.yang");

        // Check whether the data model tree returned is of type module.
        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        YangContainer yangContainer = (YangContainer) yangNode.getChild();
        assertThat(yangContainer.getName(), is("food"));

        YangChoice yangChoice = (YangChoice) yangContainer.getChild();
        assertThat(yangChoice.getName(), is("snack"));
    }

    /**
     * Checks choice statement with stmt end.
     */
    @Test
    public void processChoiceStatementWithStmtend() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/ChoiceStatementWithStmtend.yang");

        // Check whether the data model tree returned is of type module.
        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        YangContainer yangContainer = (YangContainer) yangNode.getChild();
        assertThat(yangContainer.getName(), is("food"));

        YangChoice yangChoice = (YangChoice) yangContainer.getChild();
        assertThat(yangChoice.getName(), is("snack"));
    }

    /**
     * Checks choice statement duplicate entry.
     */
    @Test(expected = ParserException.class)
    public void processChoiceStatementDuplicateEntry() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/ChoiceStatementDuplicateEntry.yang");
    }

    /**
     * Checks choice statement with same entry in two different container.
     */
    @Test
    public void processChoiceStatementSameEntryDifferentContainer() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/ChoiceStatementSameEntryDifferentContainer.yang");

        // Check whether the data model tree returned is of type module.
        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        YangContainer yangContainer1 = (YangContainer) yangNode.getChild();
        assertThat(yangContainer1.getName(), is("food1"));

        YangChoice yangChoice1 = (YangChoice) yangContainer1.getChild();
        assertThat(yangChoice1.getName(), is("snack"));

        YangContainer yangContainer2 = (YangContainer) yangNode.getChild().getNextSibling();
        assertThat(yangContainer2.getName(), is("food2"));

        YangChoice yangChoice2 = (YangChoice) yangContainer2.getChild();
        assertThat(yangChoice2.getName(), is("snack"));
    }
}
