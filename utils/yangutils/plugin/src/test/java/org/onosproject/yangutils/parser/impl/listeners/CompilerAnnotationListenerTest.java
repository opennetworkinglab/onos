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
import org.onosproject.yangutils.datamodel.YangAppDataStructure;
import org.onosproject.yangutils.datamodel.YangCompilerAnnotation;
import org.onosproject.yangutils.datamodel.YangDataStructure;
import org.onosproject.yangutils.datamodel.YangModule;
import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yangutils.datamodel.YangNodeType;
import org.onosproject.yangutils.parser.exceptions.ParserException;
import org.onosproject.yangutils.parser.impl.YangUtilsParserManager;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * Test cases for compiler annotation listener.
 */
public class CompilerAnnotationListenerTest {

    private final YangUtilsParserManager manager = new YangUtilsParserManager();

    /**
     * Checks valid compiler annotation statements.
     */
    @Test
    public void processValidCompilerAnnotation() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/ValidCompilerAnnotation.yang");

        // Check whether the data model tree returned is of type module.
        assertThat((node instanceof YangModule), is(true));

        // Check whether the node type is set properly to module.
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));

        // Check whether the module name is set correctly.
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("event"));

        YangCompilerAnnotation compilerAnnotation = yangNode.getCompilerAnnotationList()
                .iterator().next();
        assertThat(compilerAnnotation.getPrefix(), is("ca"));
        assertThat(compilerAnnotation.getPath(), is("/candidate-servers/server"));

        YangAppDataStructure appDataStructure = compilerAnnotation.getYangAppDataStructure();
        assertThat(appDataStructure.getPrefix(), is("abc"));
        assertThat(appDataStructure.getDataStructure(), is(YangDataStructure.MAP));

        assertThat(appDataStructure.getKeyList().iterator().next(), is("name"));
    }
}
