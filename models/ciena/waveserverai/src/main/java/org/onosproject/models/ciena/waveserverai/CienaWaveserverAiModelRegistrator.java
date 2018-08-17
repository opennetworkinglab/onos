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
package org.onosproject.models.ciena.waveserverai;

import com.google.common.collect.ImmutableMap;
import org.onosproject.yang.AbstractYangModelRegistrator;
import org.onosproject.yang.gen.v1.cienawaveserversystem.rev20180104.CienaWaveserverSystem;
import org.onosproject.yang.gen.v1.cienawaveserverport.rev20170731.CienaWaveserverPort;

import org.osgi.service.component.annotations.Component;
import org.onosproject.yang.model.DefaultYangModuleId;
import org.onosproject.yang.model.YangModuleId;
import org.onosproject.yang.runtime.AppModuleInfo;
import org.onosproject.yang.runtime.DefaultAppModuleInfo;

import java.util.HashMap;
import java.util.Map;

@Component(immediate = true)
public class CienaWaveserverAiModelRegistrator extends AbstractYangModelRegistrator {
    public CienaWaveserverAiModelRegistrator() {
        super(CienaWaveserverAiModelRegistrator.class, getAppInfo());
    }

    private static Map<YangModuleId, AppModuleInfo> getAppInfo() {
        Map<YangModuleId, AppModuleInfo> appInfo = new HashMap<>();
        appInfo.put(new DefaultYangModuleId("ciena-waveserver-system", "2018-01-04"),
                    new DefaultAppModuleInfo(CienaWaveserverSystem.class, null));
        appInfo.put(new DefaultYangModuleId("ciena-waveserver-port", "2017-07-31"),
                    new DefaultAppModuleInfo(CienaWaveserverPort.class, null));

        return ImmutableMap.copyOf(appInfo);
        // TODO: Do some other registration tasks...
    }
}