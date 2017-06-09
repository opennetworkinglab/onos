/*
 * Copyright 2017-present Open Networking Laboratory
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
package org.onosproject.drivers.microsemi.yang;

import com.google.common.collect.ImmutableMap;
import org.apache.felix.scr.annotations.Component;
import org.onosproject.yang.AbstractYangModelRegistrator;
import org.onosproject.yang.gen.v1.entitystatetcmib.rev20051122.EntityStateTcMib;
import org.onosproject.yang.gen.v1.fpgainternal.rev20151130.FpgaInternal;
import org.onosproject.yang.gen.v1.ianacrypthash.rev20140806.IanaCryptHash;
import org.onosproject.yang.gen.v1.ianaiftype.rev20140508.IanaIfType;
import org.onosproject.yang.gen.v1.ieeetypes.rev20080522.IeeeTypes;
import org.onosproject.yang.gen.v1.ietfinettypes.rev20130715.IetfInetTypes;
import org.onosproject.yang.gen.v1.ietfinterfaces.rev20140508.IetfInterfaces;
import org.onosproject.yang.gen.v1.ietfnetconf.rev20110601.IetfNetconf;
import org.onosproject.yang.gen.v1.ietfnetconfacm.rev20120222.IetfNetconfAcm;
import org.onosproject.yang.gen.v1.ietfnetconfmonitoring.rev20101004.IetfNetconfMonitoring;
import org.onosproject.yang.gen.v1.ietfnetconfnotifications.rev20120206.IetfNetconfNotifications;
import org.onosproject.yang.gen.v1.ietfnetconfwithdefaults.rev20100609.IetfNetconfWithDefaults;
import org.onosproject.yang.gen.v1.ietfsystem.rev20140806.IetfSystem;
import org.onosproject.yang.gen.v1.ietfsystemmicrosemi.rev20160505.IetfSystemMicrosemi;
import org.onosproject.yang.gen.v1.ietfsystemtlsauth.rev20140524.IetfSystemTlsAuth;
import org.onosproject.yang.gen.v1.ietfx509certtoname.rev20130326.IetfX509CertToName;
import org.onosproject.yang.gen.v1.ietfyangtypes.rev20130715.IetfYangTypes;
import org.onosproject.yang.gen.v1.mseacfm.rev20160229.MseaCfm;
import org.onosproject.yang.gen.v1.mseasafiltering.rev20160412.MseaSaFiltering;
import org.onosproject.yang.gen.v1.mseasoamfm.rev20160229.MseaSoamFm;
import org.onosproject.yang.gen.v1.mseasoampm.rev20160229.MseaSoamPm;
import org.onosproject.yang.gen.v1.mseatypes.rev20160229.MseaTypes;
import org.onosproject.yang.gen.v1.mseaunievcinterface.rev20160317.MseaUniEvcInterface;
import org.onosproject.yang.gen.v1.mseaunievcservice.rev20160317.MseaUniEvcService;
import org.onosproject.yang.gen.v1.ncnotifications.rev20080714.NcNotifications;
import org.onosproject.yang.gen.v1.netopeercfgnetopeer.rev20130214.NetopeerCfgnetopeer;
import org.onosproject.yang.gen.v1.notifications.rev20080714.Notifications;
import org.onosproject.yang.gen.v1.rfc2544.rev20151020.Rfc2544;
import org.onosproject.yang.gen.v1.svcactivationtypes.rev20151027.SvcActivationTypes;
import org.onosproject.yang.gen.v1.y1564.rev20151029.Y1564;
import org.onosproject.yang.model.DefaultYangModuleId;
import org.onosproject.yang.model.YangModuleId;
import org.onosproject.yang.runtime.AppModuleInfo;
import org.onosproject.yang.runtime.DefaultAppModuleInfo;

import java.util.HashMap;
import java.util.Map;

/**
 * Representation of Microsemi model registrator which registers Microsemi device
 * models.
 */
@Component(immediate = true)
public class MicrosemiModelRegistrator extends AbstractYangModelRegistrator {

    public MicrosemiModelRegistrator() {
        super(IetfSystem.class, getAppInfo());
    }

    private static Map<YangModuleId, AppModuleInfo> getAppInfo() {
        Map<YangModuleId, AppModuleInfo> appInfo = new HashMap<>();

        appInfo.put(new DefaultYangModuleId("fpga-internal", "2015-11-30"),
                new DefaultAppModuleInfo(FpgaInternal.class, null));
        appInfo.put(new DefaultYangModuleId("iana-if-type", "2014-05-08"),
                new DefaultAppModuleInfo(IanaIfType.class, null));
        appInfo.put(new DefaultYangModuleId("ietf-yang-types", "2013-07-15"),
                new DefaultAppModuleInfo(IetfYangTypes.class, null));
        appInfo.put(new DefaultYangModuleId("msea-sa-filtering", "2016-04-12"),
                new DefaultAppModuleInfo(MseaSaFiltering.class, null));
        appInfo.put(new DefaultYangModuleId("ietf-x509-cert-to-name", "2013-03-26"),
                new DefaultAppModuleInfo(IetfX509CertToName.class, null));
        appInfo.put(new DefaultYangModuleId("ietf-system", "2014-08-06"),
                new DefaultAppModuleInfo(IetfSystem.class, null));
        appInfo.put(new DefaultYangModuleId("msea-types", "2016-02-29"),
                new DefaultAppModuleInfo(MseaTypes.class, null));
        appInfo.put(new DefaultYangModuleId("ietf-inet-types", "2013-07-15"),
                new DefaultAppModuleInfo(IetfInetTypes.class, null));
        appInfo.put(new DefaultYangModuleId("ietf-netconf-with-defaults", "2010-06-09"),
                new DefaultAppModuleInfo(IetfNetconfWithDefaults.class, null));
        appInfo.put(new DefaultYangModuleId("msea-uni-evc-service", "2016-03-17"),
                new DefaultAppModuleInfo(MseaUniEvcService.class, null));
        appInfo.put(new DefaultYangModuleId("ietf-netconf-monitoring", "2010-10-04"),
                new DefaultAppModuleInfo(IetfNetconfMonitoring.class, null));
        appInfo.put(new DefaultYangModuleId("ietf-netconf-acm", "2012-02-22"),
                new DefaultAppModuleInfo(IetfNetconfAcm.class, null));
        appInfo.put(new DefaultYangModuleId("ietf-system-tls-auth", "2014-05-24"),
                new DefaultAppModuleInfo(IetfSystemTlsAuth.class, null));
        appInfo.put(new DefaultYangModuleId("rfc-2544", "2015-10-20"),
                new DefaultAppModuleInfo(Rfc2544.class, null));
        appInfo.put(new DefaultYangModuleId("msea-cfm", "2016-02-29"),
                new DefaultAppModuleInfo(MseaCfm.class, null));
        appInfo.put(new DefaultYangModuleId("netopeer-cfgnetopeer", "2013-02-14"),
                new DefaultAppModuleInfo(NetopeerCfgnetopeer.class, null));
        appInfo.put(new DefaultYangModuleId("ENTITY-STATE-TC-MIB", "2005-11-22"),
                new DefaultAppModuleInfo(EntityStateTcMib.class, null));
        appInfo.put(new DefaultYangModuleId("msea-soam-fm", "2016-02-29"),
                new DefaultAppModuleInfo(MseaSoamFm.class, null));
        appInfo.put(new DefaultYangModuleId("ietf-netconf-notifications", "2012-02-06"),
                new DefaultAppModuleInfo(IetfNetconfNotifications.class, null));
        appInfo.put(new DefaultYangModuleId("nc-notifications", "2008-07-14"),
                new DefaultAppModuleInfo(NcNotifications.class, null));
        appInfo.put(new DefaultYangModuleId("iana-crypt-hash", "2014-08-06"),
                new DefaultAppModuleInfo(IanaCryptHash.class, null));
        appInfo.put(new DefaultYangModuleId("msea-uni-evc-interface", "2016-03-17"),
                new DefaultAppModuleInfo(MseaUniEvcInterface.class, null));
        appInfo.put(new DefaultYangModuleId("msea-soam-pm", "2016-02-29"),
                new DefaultAppModuleInfo(MseaSoamPm.class, null));
        appInfo.put(new DefaultYangModuleId("ieee-types", "2008-05-22"),
                new DefaultAppModuleInfo(IeeeTypes.class, null));
        appInfo.put(new DefaultYangModuleId("svc-activation-types", "2015-10-27"),
                new DefaultAppModuleInfo(SvcActivationTypes.class, null));
        appInfo.put(new DefaultYangModuleId("ietf-netconf", "2011-06-01"),
                new DefaultAppModuleInfo(IetfNetconf.class, null));
        appInfo.put(new DefaultYangModuleId("ietf-system-microsemi", "2016-05-05"),
                new DefaultAppModuleInfo(IetfSystemMicrosemi.class, null));
        appInfo.put(new DefaultYangModuleId("notifications", "2008-07-14"),
                new DefaultAppModuleInfo(Notifications.class, null));
        appInfo.put(new DefaultYangModuleId("y-1564", "2015-10-29"),
                new DefaultAppModuleInfo(Y1564.class, null));
        appInfo.put(new DefaultYangModuleId("ietf-interfaces", "2014-05-08"),
                new DefaultAppModuleInfo(IetfInterfaces.class, null));
        return ImmutableMap.copyOf(appInfo);
    }
}
