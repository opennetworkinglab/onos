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

package org.onosproject.yms.app.yob;

import org.onosproject.yms.app.ydt.DefaultYdtWalker;
import org.onosproject.yms.app.ydt.YdtExtendedContext;
import org.onosproject.yms.app.ydt.YdtExtendedListener;
import org.onosproject.yms.app.ydt.YdtExtendedWalker;
import org.onosproject.yms.app.ysr.YangSchemaRegistry;

import static org.onosproject.yms.app.ydt.AppType.YOB;

/**
 * Represents implementation of interfaces to build and obtain YANG objects
 * from YDT.
 */
public class DefaultYobBuilder implements YobBuilder {

    /**
     * Creates an instance of DefaultYobBuilder.
     */
    public DefaultYobBuilder() {
    }

    @Override
    public Object getYangObject(YdtExtendedContext ydtRootNode,
                                YangSchemaRegistry schemaRegistry) {
        YdtExtendedWalker ydtExtendedWalker = new DefaultYdtWalker();
        YdtExtendedListener yobListener =
                new YobListener(ydtRootNode, schemaRegistry);
        if (ydtRootNode != null) {
            ydtExtendedWalker.walk(yobListener, ydtRootNode);
            YobWorkBench yobWorkBench =
                    (YobWorkBench) ydtRootNode.getAppInfo(YOB);
            return yobWorkBench.getBuilderOrBuiltObject().getBuiltObject();
        }
        return null;
    }
}
