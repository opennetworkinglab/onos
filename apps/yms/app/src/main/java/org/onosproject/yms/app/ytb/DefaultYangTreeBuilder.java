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

package org.onosproject.yms.app.ytb;

import org.onosproject.yms.app.ydt.YangRequestWorkBench;
import org.onosproject.yms.app.ydt.YdtExtendedBuilder;
import org.onosproject.yms.app.ydt.YdtExtendedContext;
import org.onosproject.yms.app.ysr.YangSchemaRegistry;
import org.onosproject.yms.ydt.YmsOperationType;

import java.util.List;

import static org.onosproject.yms.app.ytb.YtbUtil.emptyObjErrMsg;
import static org.onosproject.yms.ydt.YmsOperationType.NOTIFICATION;
import static org.onosproject.yms.ydt.YmsOperationType.RPC_REPLY;

/**
 * Representation of YANG tree builder which generates YANG data tree from the
 * class objects which are provided from the applications and return it to the
 * protocol(s).
 */
public class DefaultYangTreeBuilder implements YangTreeBuilder {

    private static final String OBJ_LIST = "object list";
    private static final String EVENT_OBJ = "event object";
    private static final String OUTPUT_OBJ = "output object";

    /**
     * Creates the YANG tree builder.
     */
    public DefaultYangTreeBuilder() {
    }

    @Override
    public YdtExtendedBuilder getYdtBuilderForYo(List<Object> moduleObj, String rootName,
                                                 String rootNameSpace, YmsOperationType opType,
                                                 YangSchemaRegistry registry) {

        if (moduleObj == null) {
            throw new YtbException(emptyObjErrMsg(OBJ_LIST));
        }

        YdtExtendedBuilder ydtBuilder = new YangRequestWorkBench(
                rootName, rootNameSpace, opType, registry, false);

        for (Object yangObj : moduleObj) {
            YdtBuilderFromYo moduleBuilder = new YdtBuilderFromYo(
                    ydtBuilder, yangObj, registry);

            moduleBuilder.getModuleNodeFromYsr(yangObj);
            moduleBuilder.createYdtFromRootObject();
        }
        return ydtBuilder;
    }

    @Override
    public YdtExtendedContext getYdtForNotification(Object object, String rootName,
                                                    YangSchemaRegistry registry) {

        if (object == null) {
            throw new YtbException(emptyObjErrMsg(EVENT_OBJ));
        }

        YdtExtendedBuilder extBuilder = new YangRequestWorkBench(
                rootName, null, NOTIFICATION, registry, false);
        YdtBuilderFromYo moduleBuilder = new YdtBuilderFromYo(
                extBuilder, object, registry);

        moduleBuilder.getRootNodeWithNotificationFromYsr(object);
        /*
         * Adds module to YDT, so that notification can further enhance the
         * tree.
         */
        moduleBuilder.createModuleInYdt();
        moduleBuilder.createYdtFromRootObject();
        return extBuilder.getRootNode();
    }

    @Override
    public YdtExtendedBuilder getYdtForRpcResponse(Object outputObj,
                                                   YdtExtendedBuilder reqBuilder) {

        if (outputObj == null) {
            throw new YtbException(emptyObjErrMsg(OUTPUT_OBJ));
        }

        YangRequestWorkBench workBench = (YangRequestWorkBench) reqBuilder;

        // Gets the logical root node from RPC request work bench.
        YdtExtendedContext rootNode = workBench.getRootNode();

        /*
         * Creates a new work bench for RPC reply from the contents of the
         * request work bench
         */
        YdtExtendedBuilder ydtBuilder =
                new YangRequestWorkBench(null, null, RPC_REPLY,
                                         workBench.getYangSchemaRegistry(),
                                         false);
        YdtBuilderFromYo moduleBuilder =
                new YdtBuilderFromYo(ydtBuilder, outputObj,
                                     workBench.getYangSchemaRegistry());

        // Forms YDT till RPC, so that output can further enhance the tree.
        moduleBuilder.createModuleAndRpcInYdt(rootNode);
        moduleBuilder.createYdtFromRootObject();
        return ydtBuilder;
    }
}
