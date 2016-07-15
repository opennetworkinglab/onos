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
import org.junit.Test;
import org.onosproject.yangutils.datamodel.YangExtension;
import org.onosproject.yangutils.datamodel.YangModule;
import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yangutils.datamodel.YangNodeType;
import org.onosproject.yangutils.parser.exceptions.ParserException;
import org.onosproject.yangutils.parser.impl.YangUtilsParserManager;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * Test cases for testing extension listener.
 */
public class ExtensionListenerTest {

    private final YangUtilsParserManager manager = new YangUtilsParserManager();

    /**
     * Checks extension statement as sub-statement of module.
     */
    @Test
    public void processValidExtensionStatement() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/ValidExtensionStatement.yang");

        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("ietf-yang-compiler-annotation"));

        YangExtension extension = yangNode.getExtensionList().iterator().next();
        assertThat(extension.getName(), is("compiler-annotation"));
        assertThat(extension.getArgumentName(), is("target"));
        assertThat(extension.getDescription(), is("\"This extension allows for defining compiler annotations\""));
    }
}

