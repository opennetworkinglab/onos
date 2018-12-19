/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.models.openconfigodtn;

import com.google.common.collect.ImmutableMap;
import org.onosproject.yang.AbstractYangModelRegistrator;
import org.onosproject.yang.gen.v1.ianaiftype.rev20170119.IanaIfType;
import org.onosproject.yang.gen.v1.openconfigalarmtypes.rev20180116.OpenconfigAlarmTypes;
import org.onosproject.yang.gen.v1.openconfigifethernet.rev20180410.OpenconfigIfEthernet;
import org.onosproject.yang.gen.v1.openconfiginterfaces.rev20180424.OpenconfigInterfaces;
import org.onosproject.yang.gen.v1.openconfigplatform.rev20180603.OpenconfigPlatform;
import org.onosproject.yang.gen.v1.openconfigplatformlinecard.rev20170803.OpenconfigPlatformLinecard;
import org.onosproject.yang.gen.v1.openconfigplatformport.rev20180120.OpenconfigPlatformPort;
import org.onosproject.yang.gen.v1.openconfigplatformtransceiver.rev20180515.OpenconfigPlatformTransceiver;
import org.onosproject.yang.gen.v1.openconfigplatformtypes.rev20180505.OpenconfigPlatformTypes;
import org.onosproject.yang.gen.v1.openconfigterminaldevice.rev20170708.OpenconfigTerminalDevice;
import org.onosproject.yang.gen.v1.openconfigtransportlinecommon.rev20170908.OpenconfigTransportLineCommon;
import org.onosproject.yang.gen.v1.openconfigtransportlineprotection.rev20170908.OpenconfigTransportLineProtection;
import org.onosproject.yang.gen.v1.openconfigtransporttypes.rev20180516.OpenconfigTransportTypes;
import org.onosproject.yang.gen.v1.openconfigtypes.rev20180505.OpenconfigTypes;
import org.onosproject.yang.gen.v1.openconfigyangtypes.rev20180424.OpenconfigYangTypes;
import org.onosproject.yang.gen.v11.ietfinterfaces.rev20180220.IetfInterfaces;
import org.onosproject.yang.model.DefaultYangModuleId;
import org.onosproject.yang.model.YangModuleId;
import org.onosproject.yang.runtime.AppModuleInfo;
import org.onosproject.yang.runtime.DefaultAppModuleInfo;
import org.osgi.service.component.annotations.Component;

import java.util.Map;

/**
 * Registrator for Openconfig Models with version as per ODTN RD.
 */
@Component(immediate = true)
public class OpenConfigOdtnModelRegistrator extends AbstractYangModelRegistrator {

    public OpenConfigOdtnModelRegistrator() {
        super(OpenConfigOdtnModelRegistrator.class, getAppInfo());
    }


    @SuppressWarnings("checkstyle:MethodLength")
    private static Map<YangModuleId, AppModuleInfo> getAppInfo() {

        return ImmutableMap.<YangModuleId, AppModuleInfo>builder()
                .put(new DefaultYangModuleId("iana-if-type", "2017-01-19"),
                        new DefaultAppModuleInfo(IanaIfType.class, null))
                .put(new DefaultYangModuleId("ietf-interfaces", "2018-02-20"),
                        new DefaultAppModuleInfo(IetfInterfaces.class, null))
                .put(new DefaultYangModuleId("openconfig-alarm-types", "2019-01-16"),
                        new DefaultAppModuleInfo(OpenconfigAlarmTypes.class, null))
//                .put(new DefaultYangModuleId("openconfig-extensions", "2017-04-11"),
//                     new DefaultAppModuleInfo(OpenconfigExte.class, null))
                .put(new DefaultYangModuleId("openconfig-if-ethernet", "2018-04-10"),
                        new DefaultAppModuleInfo(OpenconfigIfEthernet.class, null))
                .put(new DefaultYangModuleId("openconfig-interfaces", "2018-04-24"),
                        new DefaultAppModuleInfo(OpenconfigInterfaces.class, null))
                .put(new DefaultYangModuleId("openconfig-platform-linecard", "2017-08-03"),
                        new DefaultAppModuleInfo(OpenconfigPlatformLinecard.class, null))
                .put(new DefaultYangModuleId("openconfig-platform-port", "2018-01-20"),
                        new DefaultAppModuleInfo(OpenconfigPlatformPort.class, null))
                .put(new DefaultYangModuleId("openconfig-platform-transceiver", "2018-05-15"),
                        new DefaultAppModuleInfo(OpenconfigPlatformTransceiver.class, null))
                .put(new DefaultYangModuleId("openconfig-platform-types", "2018-05-05"),
                        new DefaultAppModuleInfo(OpenconfigPlatformTypes.class, null))
                .put(new DefaultYangModuleId("openconfig-platform", "2018-06-03"),
                        new DefaultAppModuleInfo(OpenconfigPlatform.class, null))
                .put(new DefaultYangModuleId("openconfig-terminal-device", "2017-07-08"),
                        new DefaultAppModuleInfo(OpenconfigTerminalDevice.class, null))
                .put(new DefaultYangModuleId("openconfig-transport-line-common", "2017-09-08"),
                        new DefaultAppModuleInfo(OpenconfigTransportLineCommon.class, null))
                .put(new DefaultYangModuleId("openconfig-transport-line-protection", "2017-09-08"),
                        new DefaultAppModuleInfo(OpenconfigTransportLineProtection.class, null))
                .put(new DefaultYangModuleId("openconfig-transport-types", "2018-05-16"),
                        new DefaultAppModuleInfo(OpenconfigTransportTypes.class, null))
                .put(new DefaultYangModuleId("openconfig-types", "2018-05-05"),
                        new DefaultAppModuleInfo(OpenconfigTypes.class, null))
                .put(new DefaultYangModuleId("openconfig-yang-types", "2018-04-24"),
                        new DefaultAppModuleInfo(OpenconfigYangTypes.class, null))
                .build();
    }
}
