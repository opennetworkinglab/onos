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
package org.onosproject.models.microsemi;

import com.google.common.collect.ImmutableMap;
import org.onosproject.yang.AbstractYangModelRegistrator;
import org.onosproject.yang.gen.v1.ietfsystemmicrosemi.rev20160505.IetfSystemMicrosemi;
import org.apache.felix.scr.annotations.Component;
import org.onosproject.yang.gen.v1.mseacfm.rev20160229.MseaCfm;
import org.onosproject.yang.gen.v1.mseasoampm.rev20160229.MseaSoamPm;
import org.onosproject.yang.gen.v1.mseasoamfm.rev20160229.MseaSoamFm;
import org.onosproject.yang.gen.v1.mseasafiltering.rev20160412.MseaSaFiltering;
import org.onosproject.yang.gen.v1.mseaunievcservice.rev20160317.MseaUniEvcService;
import org.onosproject.yang.model.DefaultYangModuleId;
import org.onosproject.yang.model.YangModuleId;
import org.onosproject.yang.runtime.AppModuleInfo;
import org.onosproject.yang.runtime.DefaultAppModuleInfo;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Component(immediate = true)
public class MicrosemiModelRegistrator extends AbstractYangModelRegistrator {
    public MicrosemiModelRegistrator() {
        super(MicrosemiModelRegistrator.class, getAppInfo());
    }

    private static Map<YangModuleId, AppModuleInfo> getAppInfo() {
        Map<YangModuleId, AppModuleInfo> appInfo = new HashMap<>();
        List<String> sysMicrosemiFeatures = new ArrayList<>();
        sysMicrosemiFeatures.add("serial-number");
        appInfo.put(new DefaultYangModuleId("ietf-system-microsemi", "2016-05-05"),
                    new DefaultAppModuleInfo(IetfSystemMicrosemi.class, sysMicrosemiFeatures));

        appInfo.put(new DefaultYangModuleId("msea-uni-evc-service", "2016-03-17"),
                new DefaultAppModuleInfo(MseaUniEvcService.class, null));
        appInfo.put(new DefaultYangModuleId("msea-cfm", "2016-02-29"),
                new DefaultAppModuleInfo(MseaCfm.class, null));
        appInfo.put(new DefaultYangModuleId("msea-soam-fm", "2016-02-29"),
                new DefaultAppModuleInfo(MseaSoamFm.class, null));
        appInfo.put(new DefaultYangModuleId("msea-soam-pm", "2016-02-29"),
                new DefaultAppModuleInfo(MseaSoamPm.class, null));
        appInfo.put(new DefaultYangModuleId("msea-sa-filtering", "2016-04-12"),
                new DefaultAppModuleInfo(MseaSaFiltering.class, null));

        return ImmutableMap.copyOf(appInfo);
        // TODO: Do some other registration tasks...
    }
}