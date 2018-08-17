/*
 * Copyright 2017-present Open Networking Foundation
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
package org.onosproject.models.common;

import com.google.common.collect.ImmutableMap;
import org.onosproject.yang.AbstractYangModelRegistrator;
import org.onosproject.yang.gen.v1.ietfinettypes.rev20130715.IetfInetTypes;
import org.onosproject.yang.gen.v1.ietfsystem.rev20140806.IetfSystem;
import org.onosproject.yang.gen.v1.ietfyangtypes.rev20130715.IetfYangTypes;
import org.osgi.service.component.annotations.Component;
import org.onosproject.yang.model.DefaultYangModuleId;
import org.onosproject.yang.model.YangModuleId;
import org.onosproject.yang.runtime.AppModuleInfo;
import org.onosproject.yang.runtime.DefaultAppModuleInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component(immediate = true)
public class YangModelRegistrator extends AbstractYangModelRegistrator {
    public YangModelRegistrator() {
        super(YangModelRegistrator.class, getAppInfo());
    }

    private static Map<YangModuleId, AppModuleInfo> getAppInfo() {
        Map<YangModuleId, AppModuleInfo> appInfo = new HashMap<>();
        appInfo.put(new DefaultYangModuleId("ietf-inet-types", "2013-07-15"),
                    new DefaultAppModuleInfo(IetfInetTypes.class, null));
        appInfo.put(new DefaultYangModuleId("ietf-yang-types", "2013-07-15"),
                    new DefaultAppModuleInfo(IetfYangTypes.class, null));

        List<String> systemFeatures = new ArrayList<>();
        systemFeatures.add("local-users");
        systemFeatures.add("authentication");
        systemFeatures.add("ntp");
        appInfo.put(new DefaultYangModuleId("ietf-system", "2014-08-06"),
                    new DefaultAppModuleInfo(IetfSystem.class, systemFeatures));
        return ImmutableMap.copyOf(appInfo);
        // TODO: Do some other registration tasks...
    }
}