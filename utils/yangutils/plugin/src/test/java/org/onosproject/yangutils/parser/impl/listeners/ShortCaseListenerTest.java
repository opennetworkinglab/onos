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
import org.onosproject.yangutils.datamodel.YangCase;
import org.onosproject.yangutils.datamodel.YangChoice;
import org.onosproject.yangutils.datamodel.YangContainer;
import org.onosproject.yangutils.datamodel.YangList;
import org.onosproject.yangutils.datamodel.YangModule;
import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yangutils.datamodel.YangNodeType;
import org.onosproject.yangutils.parser.exceptions.ParserException;
import org.onosproject.yangutils.parser.impl.YangUtilsParserManager;

import java.io.IOException;

/**
 * Test cases for short case listener.
 */
public class ShortCaseListenerTest {

    private final YangUtilsParserManager manager = new YangUtilsParserManager();

    /**
     * Checks short case listener with container.
     */
    @Test
    public void processShortCaseListenerWithContainer() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/ShortCaseListenerWithContainer.yang");

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

        YangCase yangCase = (YangCase) yangChoice.getChild();
        assertThat(yangCase.getName(), is("sports-arena"));

        YangContainer yangContainer1 = (YangContainer) yangCase.getChild();
        assertThat(yangContainer1.getName(), is("sports-arena"));
    }

    /**
     * Checks short case listener with list.
     */
    @Test
    public void processShortCaseListenerWithList() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/ShortCaseListenerWithList.yang");

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

        YangCase yangCase = (YangCase) yangChoice.getChild();
        assertThat(yangCase.getName(), is("sports-arena"));

        YangList yangList = (YangList) yangCase.getChild();
        assertThat(yangList.getName(), is("sports-arena"));
    }
}
