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
import org.onosproject.event.ListenerService;
import org.onosproject.yms.app.yab.YangApplicationBroker;
import org.onosproject.yms.app.ych.DefaultYangCodecHandler;
import org.onosproject.yms.app.ych.defaultcodecs.YangCodecRegistry;
import org.onosproject.yms.app.ydt.DefaultYdtWalker;
import org.onosproject.yms.app.ydt.YangRequestWorkBench;
import org.onosproject.yms.app.ynh.YangNotificationExtendedService;
import org.onosproject.yms.app.ynh.YangNotificationManager;
import org.onosproject.yms.app.ysr.DefaultYangModuleLibrary;
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

import static java.lang.String.valueOf;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.yms.app.ych.defaultcodecs.YangCodecRegistry.initializeDefaultCodec;

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
    private ExecutorService executor;
    private YangNotificationExtendedService ynhExtendedService;
    private YangModuleLibrary library;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Activate
    public void activate() {
        appId = coreService.registerApplication(APP_ID);
        moduleIdGenerator = coreService.getIdGenerator(MODULE_ID);
        schemaRegistry = new DefaultYangSchemaRegistry();
        library = new DefaultYangModuleLibrary(getNewModuleId());
        executor = newSingleThreadExecutor(groupedThreads(
                "onos/apps/yang-management-system/schema-registry",
                "schema-registry-handler", log));
        ynhExtendedService = new YangNotificationManager(schemaRegistry);
        //Initialize the default codec
        initializeDefaultCodec();

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        schemaRegistry.flushYsrData();
        executor.shutdown();

        // TODO implementation for other components.
        log.info("Stopped");
    }

    @Override
    public YdtBuilder getYdtBuilder(String logicalRootName,
                                    String rootNamespace,
                                    YmsOperationType opType) {
        return new YangRequestWorkBench(logicalRootName, rootNamespace,
                                        opType, schemaRegistry, true);
    }

    @Override
    public YdtBuilder getYdtBuilder(String logicalRootName,
                                    String rootNamespace,
                                    YmsOperationType opType,
                                    Object schemaRegistryForYdt) {
        if (schemaRegistryForYdt != null) {
            return new YangRequestWorkBench(
                    logicalRootName, rootNamespace, opType,
                    (YangSchemaRegistry) schemaRegistryForYdt, false);
        }
        return new YangRequestWorkBench(logicalRootName, rootNamespace,
                                        opType, schemaRegistry, true);
    }

    @Override
    public YdtWalker getYdtWalker() {
        return new DefaultYdtWalker();
    }

    @Override
    public YdtResponse executeOperation(YdtBuilder operationRequest) {
        YangApplicationBroker requestBroker =
                new YangApplicationBroker(schemaRegistry);
        switch (operationRequest.getYmsOperationType()) {
            case EDIT_CONFIG_REQUEST:
                try {
                    return requestBroker.processEdit(operationRequest);
                } catch (CloneNotSupportedException e) {
                    log.error("YAB: failed to process edit request.");
                }
            case QUERY_CONFIG_REQUEST:
                // TODO : to be implemented
            case QUERY_REQUEST:
                return requestBroker.processQuery(operationRequest);
            case RPC_REQUEST:
                return requestBroker.processOperation(operationRequest);
            default:
                // TODO : throw exception
        }
        return null;
    }

    @Override
    public YangNotificationService getYangNotificationService() {
        return ynhExtendedService;
    }

    @Override
    public YangModuleLibrary getYangModuleLibrary() {
        //TODO: get for YCH should be handled.
        return library;
    }

    @Override
    public String getYangFile(YangModuleIdentifier moduleIdentifier) {
        return schemaRegistry.getYangFile(moduleIdentifier);
    }

    @Override
    public void registerDefaultCodec(YangDataTreeCodec defaultCodec,
                                     YangProtocolEncodingFormat dataFormat) {
        YangCodecRegistry.registerDefaultCodec(defaultCodec, dataFormat);
    }

    @Override
    public void registerService(Object manager, Class<?> service,
                                List<String> features) {
        //perform registration of service
        executor.execute(() -> {

            schemaRegistry.registerApplication(manager, service);
            //process notification registration.
            processNotificationRegistration(manager, service);

            schemaRegistry.processModuleLibrary(service.getName(), library);
        });
        // TODO implementation based on supported features.
    }

    /**
     * Process notification registration for manager class object.
     *
     * @param manager yang manager
     * @param service service class
     */
    private void processNotificationRegistration(Object manager, Class<?> service) {
        synchronized (service) {
            if (manager != null && manager instanceof ListenerService) {
                if (schemaRegistry.verifyNotificationObject(manager, service)) {
                    ynhExtendedService.registerAsListener((ListenerService) manager);
                }
            }
        }
    }

    @Override
    public void unRegisterService(Object appManager, Class<?> yangService) {
        schemaRegistry.unRegisterApplication(appManager, yangService);
    }

    @Override
    public YangCodecHandler getYangCodecHandler() {
        YangSchemaRegistry registry = new DefaultYangSchemaRegistry();
        DefaultYangCodecHandler handler = new DefaultYangCodecHandler(registry);
        handler.setLibrary(new DefaultYangModuleLibrary(getNewModuleId()));
        return handler;
    }

    /**
     * Returns schema registry.
     *
     * @return schema registry
     */
    public YangSchemaRegistry getSchemaRegistry() {
        return schemaRegistry;
    }

    /**
     * Returns new generated module id.
     *
     * @return new module id
     */
    private String getNewModuleId() {
        return valueOf(moduleIdGenerator.getNewId());
    }
}
