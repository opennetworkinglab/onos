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

package org.onosproject.yms.app.ysr;

import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network1.rev20151208.IetfNetwork1Service;
import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yangutils.datamodel.YangSchemaNode;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.onosproject.yangutils.datamodel.utils.DataModelUtils.deSerializeDataModel;
import static org.onosproject.yangutils.utils.UtilConstants.TEMP;
import static org.onosproject.yangutils.utils.io.impl.YangIoUtils.deleteDirectory;

/**
 * Represents mock bundle context. provides bundle context for YSR to do unit
 * testing.
 */
public class TestYangSchemaNodeProvider {

    private static final String FS = File.separator;
    private static final String PATH = System.getProperty("user.dir") +
            FS + "target" + FS + "classes" + FS;
    private static final String SER_FILE_PATH = "yang" + FS + "resources" +
            FS + "YangMetaData.ser";
    private static final String TEMP_FOLDER_PATH = PATH + TEMP;
    private final DefaultYangSchemaRegistry registry =
            new DefaultYangSchemaRegistry();
    private static final String RESOURCE = "src/test/resources";
    private List<YangNode> nodes = new ArrayList<>();

    /**
     * Creates an instance of mock bundle context.
     */
    public TestYangSchemaNodeProvider() {
    }

    /**
     * Process YANG schema node for a application.
     *
     * @param appObject application object
     */
    public void processSchemaRegistry(Object appObject) {
        try {
            Set<YangNode> appNode = deSerializeDataModel(PATH + SER_FILE_PATH);
            nodes.addAll(appNode);
            String appName;
            ClassLoader classLoader = TestYangSchemaNodeProvider.class.getClassLoader();
            for (YangSchemaNode node : nodes) {
                appName = registry.getServiceName(node);
                Class<?> cls;
                try {
                    cls = classLoader.loadClass(appName);
                } catch (ClassNotFoundException e) {
                    continue;
                }
                registry.processRegistration(cls, RESOURCE, nodes, appObject, true);
                registry.updateServiceClass(cls);
                //interface generation.
                appName = registry.getInterfaceClassName(node);
                try {
                    cls = classLoader.loadClass(appName);
                } catch (ClassNotFoundException e) {
                    continue;
                }
                registry.processRegistration(cls, RESOURCE,
                                             nodes, appObject, true);
                registry.updateServiceClass(cls);
            }
            deleteDirectory(TEMP_FOLDER_PATH);
        } catch (IOException e) {
        }
    }

    /**
     * Unregisters services.
     *
     * @param appName application name
     */
    void unregisterService(String appName) {
        ClassLoader classLoader = TestYangSchemaNodeProvider.class.getClassLoader();
        try {
            Class<?> cls = classLoader.loadClass(appName);
            registry.unRegisterApplication(null, cls);
        } catch (ClassNotFoundException e) {
        }

    }

    /**
     * Returns schema registry.
     *
     * @return schema registry
     */
    public DefaultYangSchemaRegistry getDefaultYangSchemaRegistry() {
        return registry;
    }

    /**
     * Process registration of a service.
     */
    void processRegistrationOfApp() {
        getDefaultYangSchemaRegistry().doPreProcessing(IetfNetwork1Service.class,
                                                       new MockIetfManager());
    }

}
