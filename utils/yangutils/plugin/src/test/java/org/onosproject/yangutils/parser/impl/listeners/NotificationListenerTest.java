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
import org.onosproject.yangutils.datamodel.YangLeaf;
import org.onosproject.yangutils.datamodel.YangModule;
import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yangutils.datamodel.YangNodeType;
import org.onosproject.yangutils.datamodel.YangNotification;
import org.onosproject.yangutils.datamodel.YangStatusType;
import org.onosproject.yangutils.datamodel.YangTypeDef;
import org.onosproject.yangutils.parser.exceptions.ParserException;
import org.onosproject.yangutils.parser.impl.YangUtilsParserManager;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * Test cases for testing notification listener functionality.
 */
public class NotificationListenerTest {

    private final YangUtilsParserManager manager = new YangUtilsParserManager();

    /**
     * Checks valid notification statement.
     */
    @Test
    public void processValidNotificationStatement() throws IOException, ParserException {

        YangNode node = manager.getDataModel("src/test/resources/ValidNotificationStatement.yang");

        assertThat((node instanceof YangModule), is(true));
        assertThat(node.getNodeType(), is(YangNodeType.MODULE_NODE));
        YangModule yangNode = (YangModule) node;
        assertThat(yangNode.getName(), is("rock"));

        YangNotification yangNotification = (YangNotification) yangNode.getChild();
        assertThat(yangNotification.getName(), is("link-failure"));
        assertThat(yangNotification.getDescription(), is("\"A link failure has been detected\""));
        assertThat(yangNotification.getStatus(), is(YangStatusType.DEPRECATED));
        assertThat(yangNotification.getReference(), is("\"reference\""));

        YangTypeDef typeDef = (YangTypeDef) yangNotification.getChild();
        assertThat(typeDef.getName(), is("my-type"));
        assertThat(typeDef.getStatus(), is(YangStatusType.DEPRECATED));

        ListIterator<YangLeaf> leafIterator = yangNotification.getListOfLeaf().listIterator();
        YangLeaf leafInfo = leafIterator.next();

        assertThat(leafInfo.getName(), is("if-name"));
        assertThat(leafInfo.getDataType().getDataTypeName(), is("string"));
    }
}
