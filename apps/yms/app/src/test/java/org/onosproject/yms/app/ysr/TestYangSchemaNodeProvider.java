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
import org.onosproject.yangutils.datamodel.YangSchemaNode;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import static org.onosproject.yangutils.utils.UtilConstants.PERIOD;
import static org.onosproject.yangutils.utils.UtilConstants.TEMP;
import static org.onosproject.yangutils.utils.io.impl.YangIoUtils.deleteDirectory;
import static org.onosproject.yangutils.utils.io.impl.YangIoUtils.getCapitalCase;

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
    private final DefaultYangSchemaRegistry defaultYangSchemaRegistry =
            new DefaultYangSchemaRegistry("module-id");

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

        Set<YangSchemaNode> appNode = defaultYangSchemaRegistry
                .deSerializeDataModel(PATH + SER_FILE_PATH);
        YsrAppContext appContext = new YsrAppContext();
        defaultYangSchemaRegistry.ysrContextForSchemaStore(appContext);
        defaultYangSchemaRegistry
                .setClassLoader(this.getClass().getClassLoader());
        String appName;
        for (YangSchemaNode node : appNode) {
            defaultYangSchemaRegistry.processApplicationContext(node);
            defaultYangSchemaRegistry.ysrAppContext().appObject(appObject);
            defaultYangSchemaRegistry.ysrContextForAppStore()
                    .appObject(appObject);
            defaultYangSchemaRegistry.ysrContextForSchemaStore()
                    .appObject(appObject);
            appName = node.getJavaPackage() + PERIOD +
                    getCapitalCase(node.getJavaClassNameOrBuiltInType());
            storeClasses(appName);
        }

        try {
            deleteDirectory(TEMP_FOLDER_PATH);
        } catch (IOException e) {
        }
    }

    /**
     * Stores test generated class to YSR store.
     *
     * @param name name of class
     */
    private void storeClasses(String name) {
        ClassLoader classLoader = this.getClass().getClassLoader();
        if (getDefaultYangSchemaRegistry().verifyClassExistence(name)) {
            try {
                Class<?> nodeClass = classLoader.loadClass(name);
                getDefaultYangSchemaRegistry().updateServiceClass(nodeClass);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Unregisters services.
     *
     * @param appName application name
     */
    public void unregisterService(String appName) {

        if (getDefaultYangSchemaRegistry().verifyClassExistence(appName)) {
            try {
                Class<?> curClass = Class.forName(appName);
                getDefaultYangSchemaRegistry()
                        .unRegisterApplication(null, curClass);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Returns schema registry.
     *
     * @return schema registry
     */
    public DefaultYangSchemaRegistry getDefaultYangSchemaRegistry() {
        return defaultYangSchemaRegistry;
    }

    /**
     * Process registration of a service.
     */
    public void processRegistrationOfApp() {
        getDefaultYangSchemaRegistry()
                .processRegistration(IetfNetwork1Service.class,
                                     new MockIetfManager(), "target");

    }

}
