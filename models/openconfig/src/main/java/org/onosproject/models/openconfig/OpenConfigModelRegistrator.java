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
package org.onosproject.models.openconfig;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.Map;
import org.apache.felix.scr.annotations.Component;
import org.onosproject.yang.AbstractYangModelRegistrator;
import org.onosproject.yang.gen.v1.ianaiftype.rev20170330.IanaIfType;
import org.onosproject.yang.gen.v1.ietfyangtypes.rev20130715.IetfYangTypes;
import org.onosproject.yang.gen.v1.openconfiginterfaces.rev20170714.OpenconfigInterfaces;
import org.onosproject.yang.gen.v1.openconfigplatform.rev20161222.OpenconfigPlatform;
import org.onosproject.yang.gen.v1.openconfigplatformlinecard.rev20170803.OpenconfigPlatformLinecard;
import org.onosproject.yang.gen.v1.openconfigplatformport.rev20161024.OpenconfigPlatformPort;
import org.onosproject.yang.gen.v1.openconfigplatformtransceiver.rev20170708.OpenconfigPlatformTransceiver;
import org.onosproject.yang.gen.v1.openconfigterminaldevice.rev20170708.OpenconfigTerminalDevice;
import org.onosproject.yang.gen.v1.openconfigtransportlinecommon.rev20170708.OpenconfigTransportLineCommon;
import org.onosproject.yang.gen.v1.openconfigtransporttypes.rev20170816.OpenconfigTransportTypes;
import org.onosproject.yang.gen.v1.openconfigtypes.rev20170816.OpenconfigTypes;
import org.onosproject.yang.model.DefaultYangModuleId;
import org.onosproject.yang.model.YangModuleId;
import org.onosproject.yang.runtime.AppModuleInfo;
import org.onosproject.yang.runtime.DefaultAppModuleInfo;
import org.slf4j.Logger;

import com.google.common.collect.ImmutableMap;

@Component(immediate = true)
public class OpenConfigModelRegistrator extends AbstractYangModelRegistrator {

    private static final Logger log = getLogger(OpenConfigModelRegistrator.class);

    public OpenConfigModelRegistrator() {
        super(OpenConfigModelRegistrator.class, getAppInfo());
    }


    private static Map<YangModuleId, AppModuleInfo> getAppInfo() {

        return ImmutableMap.<YangModuleId, AppModuleInfo>builder()
                .put(new DefaultYangModuleId("iana-if-type", "2017-03-30"),
                     new DefaultAppModuleInfo(IanaIfType.class, null))

                // FIXME requires entry for each .yangs not covered yet

                .put(new DefaultYangModuleId("openconfig-platform", "2016-12-22"),
                     new DefaultAppModuleInfo(OpenconfigPlatform.class, null))
                .put(new DefaultYangModuleId("openconfig-interfaces", "2017-07-14"),
                     new DefaultAppModuleInfo(OpenconfigInterfaces.class, null))
                .put(new DefaultYangModuleId("openconfig-transport-types", "2017-08-16"),
                     new DefaultAppModuleInfo(OpenconfigTransportTypes.class, null))
                .put(new DefaultYangModuleId("openconfig-types", "2017-08-16"),
                     new DefaultAppModuleInfo(OpenconfigTypes.class, null))
//                .put(new DefaultYangModuleId("openconfig-extensions", "2017-08-16"),
//                     new DefaultAppModuleInfo(OpenconfigEx.class, null))
                .put(new DefaultYangModuleId("ietf-yang-types", "2013-07-15"),
                     new DefaultAppModuleInfo(IetfYangTypes.class, null))


                // minimum required for the example
                .put(new DefaultYangModuleId("openconfig-platform-linecard", "2017-08-03"),
                     new DefaultAppModuleInfo(OpenconfigPlatformLinecard.class, null))
                .put(new DefaultYangModuleId("openconfig-platform-port", "2016-10-24"),
                     new DefaultAppModuleInfo(OpenconfigPlatformPort.class, null))
                .put(new DefaultYangModuleId("openconfig-platform-transceiver", "2017-07-08"),
                     new DefaultAppModuleInfo(OpenconfigPlatformTransceiver.class, null))
                .put(new DefaultYangModuleId("openconfig-transport-line-common", "2017-07-08"),
                     new DefaultAppModuleInfo(OpenconfigTransportLineCommon.class, null))
                .put(new DefaultYangModuleId("openconfig-terminal-device", "2017-07-08"),
                     new DefaultAppModuleInfo(OpenconfigTerminalDevice.class, null))

                .build();
    }
}
