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

package org.onosproject.yms.app.ymsm;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.core.IdGenerator;
import org.onosproject.yms.app.ynh.YangNotificationExtendedService;
import org.onosproject.yms.app.ysr.DefaultYangSchemaRegistry;
import org.onosproject.yms.app.ysr.YangSchemaRegistry;
import org.onosproject.yms.ych.YangCodecHandler;
import org.onosproject.yms.ych.YangDataTreeCodec;
import org.onosproject.yms.ych.YangProtocolEncodingFormat;
import org.onosproject.yms.ydt.YdtBuilder;
import org.onosproject.yms.ydt.YdtResponse;
import org.onosproject.yms.ydt.YdtWalker;
import org.onosproject.yms.ydt.YmsOperationType;
import org.onosproject.yms.ymsm.YmsService;
import org.onosproject.yms.ynh.YangNotificationService;
import org.onosproject.yms.ysr.YangModuleIdentifier;
import org.onosproject.yms.ysr.YangModuleLibrary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.onlab.util.Tools.groupedThreads;

/**
 * Represents implementation of YANG management system manager.
 */
@Service
@Component(immediate = true)
public class YmsManager
        implements YmsService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String APP_ID = "org.onosproject.app.yms";
    private static final String MODULE_ID = "module-id";
    private ApplicationId appId;
    private YangSchemaRegistry schemaRegistry;
    //module id generator should be used to generate a new module id for
    //each YSR instance. So YCH also should generate it.
    private IdGenerator moduleIdGenerator;
    private ExecutorService schemaRegistryExecutor;
    private YangNotificationExtendedService ynhExtendedService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Activate
    public void activate() {
        appId = coreService.registerApplication(APP_ID);
        moduleIdGenerator = coreService.getIdGenerator(MODULE_ID);
        schemaRegistry = new DefaultYangSchemaRegistry(String.valueOf(
                moduleIdGenerator.getNewId()));
        schemaRegistryExecutor =
                Executors.newSingleThreadExecutor(groupedThreads(
                        "onos/apps/yang-management-system/schema-registry",
                        "schema-registry-handler", log));
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        ((DefaultYangSchemaRegistry) schemaRegistry).flushYsrData();
        schemaRegistryExecutor.shutdown();

        // TODO implementation for other components.
        log.info("Stopped");
    }

    @Override
    public YdtBuilder getYdtBuilder(String logicalRootName,
                                    String rootNamespace,
                                    YmsOperationType operationType) {
        return null;
    }

    @Override
    public YdtBuilder getYdtBuilder(String logicalRootName,
                                    String rootNamespace,
                                    YmsOperationType operationType,
                                    Object schemaRegistryForYdt) {
        return null;
    }

    @Override
    public YdtWalker getYdtWalker() {
        return null;
    }

    @Override
    public YdtResponse executeOperation(YdtBuilder operationRequest) {
        return null;
    }

    @Override
    public YangNotificationService getYangNotificationService() {
        return ynhExtendedService;
    }

    /**
     * Returns YANG notification extended service.
     *
     * @return YANG notification extended service
     */
    private YangNotificationExtendedService getYnhExtendedService() {
        return ynhExtendedService;
    }

    @Override
    public YangModuleLibrary getYangModuleLibrary() {
        return ((DefaultYangSchemaRegistry) schemaRegistry).getLibrary();
    }

    @Override
    public String getYangFile(YangModuleIdentifier moduleIdentifier) {
        return ((DefaultYangSchemaRegistry) schemaRegistry)
                .getYangFile(moduleIdentifier);
    }

    @Override
    public void registerDefaultCodec(YangDataTreeCodec defaultCodec,
                                     YangProtocolEncodingFormat dataFormat) {

    }

    @Override
    public void registerService(Object yangManager, Class<?> yangService,
                                List<String> supportedFeatureList) {

        //perform registration of service
        schemaRegistryExecutor.execute(() -> schemaRegistry
                .registerApplication(yangManager, yangService,
                                     getYnhExtendedService()));
    }

    @Override
    public void unRegisterService(Object appManager, Class<?> yangService) {
        schemaRegistry.unRegisterApplication(appManager, yangService);
    }

    @Override
    public YangCodecHandler getYangCodecHandler() {
        return null;
    }

    /**
     * Returns schema registry.
     *
     * @return schema registry
     */
    public YangSchemaRegistry getSchemaRegistry() {
        return schemaRegistry;
    }

}
