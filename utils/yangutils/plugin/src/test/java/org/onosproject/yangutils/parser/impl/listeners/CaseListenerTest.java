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
import org.onosproject.yangutils.datamodel.YangLeaf;
import org.onosproject.yangutils.datamodel.YangModule;
import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yangutils.datamodel.YangNodeType;
import org.onosproject.yangutils.parser.exceptions.ParserException;
import org.onosproject.yangutils.parser.impl.YangUtilsParserManager;

import java.io.IOException;
import java.util.ListIterator;

/**
 * Test cases for case listener.
 */
public class CaseListenerTest {

    private final YangUtilsParserManager manager = new YangUtilsParserManager();

    /**
     * Checks multiple case statement.
     */
    @Test
    public void processCaseStatement() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/CaseStatement.yang");

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

        YangCase yangCase1 = (YangCase) yangChoice.getChild();
        assertThat(yangCase1.getName(), is("sports-arena"));

        // Check whether leaf properties as set correctly.
        ListIterator<YangLeaf> leafIterator1 = yangCase1.getListOfLeaf().listIterator();
        YangLeaf leafInfo1 = leafIterator1.next();

        assertThat(leafInfo1.getName(), is("pretzel"));

        YangCase yangCase2 = (YangCase) yangCase1.getNextSibling();
        assertThat(yangCase2.getName(), is("late-night"));

        // Check whether leaf properties as set correctly.
        ListIterator<YangLeaf> leafIterator2 = yangCase2.getListOfLeaf().listIterator();
        YangLeaf leafInfo2 = leafIterator2.next();

        assertThat(leafInfo2.getName(), is("chocolate"));
    }

    /**
     * Checks duplicate case in choice.
     */
    @Test(expected = ParserException.class)
    public void processDuplicateCaseInChoice() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/DuplicateCaseInChoice.yang");
    }

    /**
     * Checks duplicate leaf at different hierarchy.
     */
    @Test(expected = ParserException.class)
    public void processDuplicateLeafInHierarchy() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/DuplicateLeafInHierarchy.yang");
    }

    /**
     * Checks duplicate leaf in different case within choice.
     */
    @Test(expected = ParserException.class)
    public void processDuplicateLeafInChoice() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/DuplicateLeafInChoice.yang");
    }

    /**
     * Checks same case within different choice.
     */
    @Test
    public void processCaseStatementSameEntryDifferentChoice() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/CaseStatementSameEntryDifferentChoice.yang");

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

        YangCase yangCase1 = (YangCase) yangChoice.getChild();
        assertThat(yangCase1.getName(), is("sports-arena"));

        // Check whether leaf properties as set correctly.
        ListIterator<YangLeaf> leafIterator1 = yangCase1.getListOfLeaf().listIterator();
        YangLeaf leafInfo1 = leafIterator1.next();

        assertThat(leafInfo1.getName(), is("pretzel"));

        YangChoice yangChoice2 = (YangChoice) yangChoice.getNextSibling();
        assertThat(yangChoice2.getName(), is("lunch"));

        YangCase yangCase2 = (YangCase) yangChoice2.getChild();
        assertThat(yangCase2.getName(), is("sports-arena"));

        // Check whether leaf properties as set correctly.
        ListIterator<YangLeaf> leafIterator2 = yangCase2.getListOfLeaf().listIterator();
        YangLeaf leafInfo2 = leafIterator2.next();

        assertThat(leafInfo2.getName(), is("chocolate"));
    }

    /**
     * Checks case choice hierarchy.
     */
    @Test
    public void processCaseChoiceHierarchy() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/CaseChoiceHierarchy.yang");

        // Check whether the data model tree returned is of type module.
        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        YangContainer yangContainer = (YangContainer) yangNode.getChild();
        assertThat(yangContainer.getName(), is("food"));

        YangChoice yangChoice1 = (YangChoice) yangContainer.getChild();
        assertThat(yangChoice1.getName(), is("snack"));

        YangCase yangCase1 = (YangCase) yangChoice1.getChild();
        assertThat(yangCase1.getName(), is("sports-arena"));

        // Check whether leaf properties as set correctly.
        ListIterator<YangLeaf> leafIterator1 = yangCase1.getListOfLeaf().listIterator();
        YangLeaf leafInfo1 = leafIterator1.next();

        assertThat(leafInfo1.getName(), is("pretzel"));

        YangCase yangCase2 = (YangCase) yangCase1.getNextSibling();
        assertThat(yangCase2.getName(), is("late-night"));

        YangChoice yangChoice2 = (YangChoice) yangCase2.getChild();
        assertThat(yangChoice2.getName(), is("dinner"));

        YangCase yangCase3 = (YangCase) yangChoice2.getChild();
        assertThat(yangCase3.getName(), is("late-night"));

        // Check whether leaf properties as set correctly.
        ListIterator<YangLeaf> leafIterator2 = yangCase3.getListOfLeaf().listIterator();
        YangLeaf leafInfo2 = leafIterator2.next();
        assertThat(leafInfo2.getName(), is("beer"));
    }
}
