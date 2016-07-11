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

import org.junit.Test;
import org.onosproject.yangutils.datamodel.YangMust;
import org.onosproject.yangutils.datamodel.YangModule;
import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yangutils.datamodel.YangContainer;
import org.onosproject.yangutils.datamodel.YangAppErrorInfo;
import org.onosproject.yangutils.parser.exceptions.ParserException;
import org.onosproject.yangutils.parser.impl.YangUtilsParserManager;

import java.io.IOException;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * Test-cases for testing error app tag message listener functionality.
 */
public class ErrorAppTagListenerTest {
    private final YangUtilsParserManager manager = new YangUtilsParserManager();

    /**
     * Checks if error app tag message is default updated in the data model.
     */
    @Test
    public void processContainerSubStatementErrorDefaultAppTag() throws IOException, ParserException {
        YangNode node = manager.getDataModel("src/test/resources/ContainerSubStatementErrorDefaultAppTag.yang");

        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("ErrorAppTag"));

        YangContainer yangContainer = (YangContainer) yangNode.getChild();
        assertThat(yangContainer.getName(), is("interface"));

        String expectedConstraint = "ifType != 'ethernet' or (ifType = 'ethernet' and ifMTU = 1500)";
        List<YangMust> mustConstraintList = yangContainer.getListOfMust();
        assertThat(mustConstraintList.iterator().next().getConstraint(), is(expectedConstraint));

        YangAppErrorInfo yangAppErrorInfo = mustConstraintList.iterator().next().getAppErrorInfo();
        assertThat(yangAppErrorInfo.getGetErrorAppTag(), is("must-violation"));
        assertThat(yangAppErrorInfo.getGetErrorTag(), is("operation-failed"));
    }

    /**
     * Checks if error app tag message listener updates the data model.
     */
    @Test
    public void processContainerSubStatementErrorAppTag() throws IOException, ParserException {
        YangNode node = manager.getDataModel("src/test/resources/ContainerSubStatementErrorAppTag.yang");

        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("ErrorAppTag"));

        YangContainer yangContainer = (YangContainer) yangNode.getChild();
        assertThat(yangContainer.getName(), is("interface"));

        String expectedConstraint = "ifType != 'ethernet' or (ifType = 'ethernet' and ifMTU = 1500)";
        List<YangMust> mustConstraintList = yangContainer.getListOfMust();
        assertThat(mustConstraintList.iterator().next().getConstraint(), is(expectedConstraint));

        YangAppErrorInfo yangAppErrorInfo = mustConstraintList.iterator().next().getAppErrorInfo();
        assertThat(yangAppErrorInfo.getGetErrorAppTag(), is("An ethernet MTU must be 1500"));
        assertThat(yangAppErrorInfo.getGetErrorTag(), is("operation-failed"));
    }
}