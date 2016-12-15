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

package org.onosproject.yms.app.yab;

import static java.lang.String.valueOf;

import java.io.IOException;
import java.util.List;

import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onosproject.core.CoreService;
import org.onosproject.core.IdGenerator;
import org.onosproject.drivers.netconf.MockCoreService;
import org.onosproject.yms.app.ych.DefaultYangCodecHandler;
import org.onosproject.yms.app.ych.defaultcodecs.YangCodecRegistry;
import org.onosproject.yms.app.ydt.YangRequestWorkBench;
import org.onosproject.yms.app.ysr.DefaultYangModuleLibrary;
import org.onosproject.yms.app.ysr.MockYangSchemaRegistry;
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

/**
 * Represents implementation of YANG application management system manager.
 */
public class MockYmsManager
        implements YmsService {
    private static final String MODULE_ID = "module-id";

    YangSchemaRegistry schemaRegistry;
    private YangModuleLibrary library;
    private IdGenerator moduleIdGenerator;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    public MockYmsManager()
            throws InstantiationException, IllegalAccessException, IOException {
        coreService = new MockCoreService();
        moduleIdGenerator = coreService.getIdGenerator(MODULE_ID);
        library = new DefaultYangModuleLibrary(getNewModuleId());
        YangCodecRegistry.initializeDefaultCodec();
    }

    @Override
    public YdtBuilder getYdtBuilder(String logicalRootName,
                                    String rootNamespace,
                                    YmsOperationType operationType) {
        return new YangRequestWorkBench(logicalRootName, rootNamespace,
                                        operationType, schemaRegistry, false);
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
        YangApplicationBroker requestBroker =
                new YangApplicationBroker(schemaRegistry);
        switch (operationRequest.getYmsOperationType()) {
            case EDIT_CONFIG_REQUEST:
                try {
                    return requestBroker.processEdit(operationRequest);
                } catch (CloneNotSupportedException e) {
                }
                break;
            case QUERY_CONFIG_REQUEST:
            case QUERY_REQUEST:
                return requestBroker.processQuery(operationRequest);
            case RPC_REQUEST:
                return requestBroker.processOperation(operationRequest);
            default:
        }
        return null;
    }

    @Override
    public YangNotificationService getYangNotificationService() {
        return null;
    }

    @Override
    public void registerService(Object appManager, Class<?> yangService,
                                List<String> supportedFeatureList) {
    }

    @Override
    public void unRegisterService(Object appManager, Class<?> yangService) {

    }

    @Override
    public YangModuleLibrary getYangModuleLibrary() {
        return library;
    }

    @Override
    public String getYangFile(YangModuleIdentifier moduleIdentifier) {
        return null;
    }

    @Override
    public void registerDefaultCodec(YangDataTreeCodec defaultCodec,
                                     YangProtocolEncodingFormat dataFormat) {
    }

    @Override
    public YangCodecHandler getYangCodecHandler() {
        YangSchemaRegistry registry = new MockYangSchemaRegistry();
        DefaultYangCodecHandler handler = new DefaultYangCodecHandler(registry);
        handler.setLibrary(new DefaultYangModuleLibrary(getNewModuleId()));
        return handler;
    }

    private String getNewModuleId() {
        return valueOf(moduleIdGenerator.getNewId());
    }
}
