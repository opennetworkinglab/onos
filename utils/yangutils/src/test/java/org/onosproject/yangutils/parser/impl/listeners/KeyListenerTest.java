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

import org.junit.Rule;
import org.junit.Test;

import org.junit.rules.ExpectedException;
import org.onosproject.yangutils.datamodel.YangModule;
import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yangutils.datamodel.YangNodeType;
import org.onosproject.yangutils.datamodel.YangList;
import org.onosproject.yangutils.parser.exceptions.ParserException;
import org.onosproject.yangutils.parser.impl.YangUtilsParserManager;

import java.io.IOException;
import java.util.List;
import java.util.ListIterator;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * Test cases for key listener.
 */
public class KeyListenerTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private final YangUtilsParserManager manager = new YangUtilsParserManager();

    /**
     * Checks key statement as sub-statement of list.
     */
    @Test
    public void processListSubStatementKey() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/ListSubStatementKey.yang");

        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        // Check whether the list is child of module
        YangList yangList = (YangList) yangNode.getChild();
        assertThat(yangList.getName(), is("valid"));

        ListIterator<String> keyList = yangList.getKeyList().listIterator();
        assertThat(keyList.next(), is("invalid-interval"));
    }

    /**
     * Check multiple key values.
     */
    @Test
    public void processMultipleKeyValues() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/MultipleKeyValues.yang");

        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        // Check whether the list is child of module
        YangList yangList = (YangList) yangNode.getChild();
        assertThat(yangList.getName(), is("valid"));

        List<String> keyList = yangList.getKeyList();
        assertThat(keyList.contains("ospf"), is(true));
        assertThat(keyList.contains("isis"), is(true));
    }

    /**
     * Checks key statement without statement end.
     */
    @Test
    public void processKeyWithoutStatementEnd() throws IOException, ParserException {
        thrown.expect(ParserException.class);
        thrown.expectMessage("mismatched input 'leaf' expecting {';', '+'}");
        YangNode node = manager.getDataModel("src/test/resources/KeyWithoutStatementEnd.yang");
    }

    /**
     * Checks key values are set correctly.
     */
    @Test
    public void processConfigFalseNoKey() throws IOException, ParserException {
        YangNode node = manager.getDataModel("src/test/resources/ConfigFalseNoKey.yang");

        assertThat((node instanceof YangModule), is(true));
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        // Check whether the list is child of module
        YangList yangList = (YangList) yangNode.getChild();
        assertThat(yangList.getName(), is("valid"));
    }

    /**
     * Checks key values are set correctly.
     */
    @Test
    public void processConfigFalseValidKeyValidLeaf() throws IOException, ParserException {
        YangNode node = manager.getDataModel("src/test/resources/ConfigFalseValidKeyValidLeaf.yang");

        assertThat((node instanceof YangModule), is(true));
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        // Check whether the list is child of module
        YangList yangList = (YangList) yangNode.getChild();
        assertThat(yangList.getName(), is("valid"));

        ListIterator<String> keyList = yangList.getKeyList().listIterator();
        assertThat(keyList.next(), is("invalid-interval"));
    }

    /**
     * Checks key values are set correctly.
     */
    @Test
    public void processConfigFalseValidKeyValidLeafList() throws IOException, ParserException {
        YangNode node = manager.getDataModel("src/test/resources/ConfigFalseValidKeyValidLeafList.yang");

        assertThat((node instanceof YangModule), is(true));
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        // Check whether the list is child of module
        YangList yangList = (YangList) yangNode.getChild();
        assertThat(yangList.getName(), is("valid"));

        ListIterator<String> keyList = yangList.getKeyList().listIterator();
        assertThat(keyList.next(), is("invalid-interval"));
    }

    /**
     * Checks whether exception is thrown when list's config is set to true and there is no key.
     */
    @Test
    public void processConfigTrueNoKey() throws IOException, ParserException {
        thrown.expect(ParserException.class);
        thrown.expectMessage("A list must have atleast one key leaf if config is true");
        YangNode node = manager.getDataModel("src/test/resources/ConfigTrueNoKey.yang");
    }

    /**
     * Checks whether exception is thrown when list's config is set to true and there is no leaf.
     */
    @Test
    public void processConfigTrueNoleafNoLeafList() throws IOException, ParserException {
        thrown.expect(ParserException.class);
        thrown.expectMessage("A list must have atleast one key leaf if config is true");
        YangNode node = manager.getDataModel("src/test/resources/ConfigTrueNoleafNoLeafList.yang");
    }

    /**
     * Checks key values are set correctly.
     */
    @Test
    public void processConfigTrueValidKeyValidLeaf() throws IOException, ParserException {
        YangNode node = manager.getDataModel("src/test/resources/ConfigTrueValidKeyValidLeaf.yang");

        assertThat((node instanceof YangModule), is(true));
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        // Check whether the list is child of module
        YangList yangList = (YangList) yangNode.getChild();
        assertThat(yangList.getName(), is("valid"));

        ListIterator<String> keyList = yangList.getKeyList().listIterator();
        assertThat(keyList.next(), is("invalid-interval"));
    }

    /**
     * Checks key values are set correctly.
     */
    @Test
    public void processKeyWithUsesInList() throws IOException, ParserException {
        YangNode node = manager.getDataModel("src/test/resources/KeyWithUsesInList.yang");

        assertThat((node instanceof YangModule), is(true));
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("Test"));

        // Check whether the list is child of module
        YangList yangList = (YangList) yangNode.getChild().getNextSibling();
        assertThat(yangList.getName(), is("valid"));

        ListIterator<String> keyList = yangList.getKeyList().listIterator();
        assertThat(keyList.next(), is("invalid-interval"));
    }

    /**
     * Checks whether exception is thrown when key leaf identifier is not found in list.
     */
    @Test
    public void processInvalidLeafIdentifier() throws IOException, ParserException {
        thrown.expect(ParserException.class);
        thrown.expectMessage("An identifier, in key, must refer to a child leaf of the list");
        YangNode node = manager.getDataModel("src/test/resources/InvalidLeafIdentifier.yang");
    }

    /**
     * Checks whether exception is thrown when key leaf-list identifier is not found in list.
     */
    @Test
    public void processInvalidLeafListIdentifier() throws IOException, ParserException {
        thrown.expect(ParserException.class);
        thrown.expectMessage("An identifier, in key, must refer to a child leaf of the list");
        YangNode node = manager.getDataModel("src/test/resources/InvalidLeafListIdentifier.yang");
    }

    /**
     * Checks whether exception is thrown when key leaf-list is of type empty.
     */
    @Test
    public void processKeyLeafListTypeEmpty() throws IOException, ParserException {
        thrown.expect(ParserException.class);
        thrown.expectMessage("A leaf-list that is part of the key must not be the built-in type \"empty\".");
        YangNode node = manager.getDataModel("src/test/resources/KeyLeafListTypeEmpty.yang");
    }

    /**
     * Checks whether exception is thrown when key leaf is of type empty.
     */
    @Test
    public void processKeyLeafTypeEmpty() throws IOException, ParserException {
        thrown.expect(ParserException.class);
        thrown.expectMessage("A leaf that is part of the key must not be the built-in type \"empty\".");
        YangNode node = manager.getDataModel("src/test/resources/KeyLeafTypeEmpty.yang");
    }
}