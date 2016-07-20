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

package org.onosproject.yangutils.plugin.manager;

import java.io.IOException;

import org.junit.Test;
import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yangutils.parser.exceptions.ParserException;
import org.onosproject.yangutils.parser.impl.YangUtilsParserManager;
import org.onosproject.yangutils.utils.io.impl.YangPluginConfig;

import static org.onosproject.yangutils.translator.tojava.JavaCodeGeneratorUtil.generateJavaCode;
import static org.onosproject.yangutils.utils.io.impl.YangIoUtils.deleteDirectory;

/**
 * Unit tests for choice-case translator.
 */
public final class ChoiceCaseTranslatorTest {

    private final YangUtilsParserManager manager = new YangUtilsParserManager();

    /**
     * Checks choice-case translation should not result in any exception.
     */
    @Test
    public void processChoiceCaseTranslator() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/ChoiceCaseTranslator.yang");

        YangPluginConfig yangPluginConfig = new YangPluginConfig();
        yangPluginConfig.setCodeGenDir("target/ChoiceCaseTestGenFile/");

        generateJavaCode(node, yangPluginConfig);

        deleteDirectory("target/ChoiceCaseTestGenFile/");
    }
    // TODO enhance the test cases, after having a framework of translator test.
}
