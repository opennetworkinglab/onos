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

package org.onosproject.yms.app.ydt;

import org.onosproject.yangutils.datamodel.YangSchemaNode;
import org.onosproject.yms.app.ysr.DefaultYangSchemaRegistry;
import org.onosproject.yms.app.ysr.TestYangSchemaNodeProvider;
import org.onosproject.yms.app.ysr.YangSchemaRegistry;
import org.onosproject.yms.ysr.YangModuleIdentifier;
import org.onosproject.yms.ysr.YangModuleLibrary;

import static org.junit.Assert.assertEquals;


/**
 * Represent Yang schema registry. Yang schema registry provides
 * interface to an application to register its YANG
 * schema with YMS. It provides YANG schema nodes to YDT, YNB and YSB.
 */
public class MockYangSchemaRegistry implements YangSchemaRegistry {

    @Override
    public void registerApplication(Object managerObject, Class<?> serviceClass) {

    }

    @Override
    public void unRegisterApplication(Object managerObject,
                                      Class<?> serviceClass) {
    }

    @Override
    public Object getRegisteredApplication(YangSchemaNode yangSchemaNode) {
        return null;
    }

    @Override
    public YangSchemaNode getYangSchemaNodeUsingAppName(String schemaName) {
        return null;
    }

    @Override
    public YangSchemaNode getYangSchemaNodeUsingSchemaName(String appName) {

        final String target = "target/TestYangSchemaNodeProvider";
        TestYangSchemaNodeProvider testYangSchemaNodeProvider =
                new TestYangSchemaNodeProvider();

        String searchDir = "src/test/resources/ydtTestYangFiles/";

        testYangSchemaNodeProvider.processSchemaRegistry(null);

        DefaultYangSchemaRegistry registry = testYangSchemaNodeProvider
                .getDefaultYangSchemaRegistry();
        YangSchemaNode yangNode = registry
                .getYangSchemaNodeUsingSchemaName(appName);
        assertEquals(appName, yangNode.getName());
        return yangNode;
    }

    @Override
    public YangSchemaNode
    getYangSchemaNodeUsingGeneratedRootNodeInterfaceFileName(
            String rootInterfaceFileName) {
        return null;
    }

    @Override
    public YangSchemaNode getYangSchemaNodeUsingGeneratedRootNodeOpPramFileName(
            String rootOpParamFileName) {
        return null;
    }

    @Override
    public YangSchemaNode getRootYangSchemaNodeForNotification(
            String eventSubject) {
        return null;
    }

    @Override
    public Class<?> getRegisteredClass(YangSchemaNode schemaNode) {
        return null;
    }

    @Override
    public boolean verifyNotificationObject(Object appObj, Class<?> service) {
        return false;
    }

    @Override
    public void flushYsrData() {

    }

    @Override
    public String getYangFile(YangModuleIdentifier moduleIdentifier) {
        return null;
    }

    @Override
    public void processModuleLibrary(String serviceName, YangModuleLibrary library) {

    }

}
