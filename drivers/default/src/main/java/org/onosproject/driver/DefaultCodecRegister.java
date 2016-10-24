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
package org.onosproject.driver;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onosproject.codec.CodecService;
import org.onosproject.driver.extensions.MoveExtensionTreatment;
import org.onosproject.driver.extensions.NiciraMatchNshSi;
import org.onosproject.driver.extensions.NiciraMatchNshSpi;
import org.onosproject.driver.extensions.NiciraResubmit;
import org.onosproject.driver.extensions.NiciraResubmitTable;
import org.onosproject.driver.extensions.NiciraSetNshContextHeader;
import org.onosproject.driver.extensions.NiciraSetNshSi;
import org.onosproject.driver.extensions.NiciraSetNshSpi;
import org.onosproject.driver.extensions.NiciraSetTunnelDst;
import org.onosproject.driver.extensions.Ofdpa3MatchMplsL2Port;
import org.onosproject.driver.extensions.Ofdpa3MatchOvid;
import org.onosproject.driver.extensions.Ofdpa3SetMplsL2Port;
import org.onosproject.driver.extensions.Ofdpa3SetMplsType;
import org.onosproject.driver.extensions.Ofdpa3SetOvid;
import org.onosproject.driver.extensions.Ofdpa3SetQosIndex;
import org.onosproject.driver.extensions.OfdpaMatchVlanVid;
import org.onosproject.driver.extensions.OfdpaSetVlanVid;
import org.onosproject.driver.extensions.OplinkAttenuation;
import org.onosproject.driver.extensions.codec.MoveExtensionTreatmentCodec;
import org.onosproject.driver.extensions.codec.NiciraMatchNshSiCodec;
import org.onosproject.driver.extensions.codec.NiciraMatchNshSpiCodec;
import org.onosproject.driver.extensions.codec.NiciraResubmitCodec;
import org.onosproject.driver.extensions.codec.NiciraResubmitTableCodec;
import org.onosproject.driver.extensions.codec.NiciraSetNshContextHeaderCodec;
import org.onosproject.driver.extensions.codec.NiciraSetNshSiCodec;
import org.onosproject.driver.extensions.codec.NiciraSetNshSpiCodec;
import org.onosproject.driver.extensions.codec.NiciraSetTunnelDstCodec;
import org.onosproject.driver.extensions.codec.Ofdpa3MatchMplsL2PortCodec;
import org.onosproject.driver.extensions.codec.Ofdpa3MatchOvidCodec;
import org.onosproject.driver.extensions.codec.Ofdpa3SetMplsL2PortCodec;
import org.onosproject.driver.extensions.codec.Ofdpa3SetMplsTypeCodec;
import org.onosproject.driver.extensions.codec.Ofdpa3SetOvidCodec;
import org.onosproject.driver.extensions.codec.Ofdpa3SetQosIndexCodec;
import org.onosproject.driver.extensions.codec.OfdpaMatchVlanVidCodec;
import org.onosproject.driver.extensions.codec.OfdpaSetVlanVidCodec;
import org.onosproject.driver.extensions.codec.OplinkAttenuationCodec;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Codec register for default drivers.
 */
@Component(immediate = true)
public class DefaultCodecRegister {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CodecService codecService;

    @Activate
    public void activate() {
        codecService.registerCodec(MoveExtensionTreatment.class, new MoveExtensionTreatmentCodec());
        codecService.registerCodec(NiciraMatchNshSi.class, new NiciraMatchNshSiCodec());
        codecService.registerCodec(NiciraMatchNshSpi.class, new NiciraMatchNshSpiCodec());
        codecService.registerCodec(NiciraResubmit.class, new NiciraResubmitCodec());
        codecService.registerCodec(NiciraResubmitTable.class, new NiciraResubmitTableCodec());
        codecService.registerCodec(NiciraSetNshSi.class, new NiciraSetNshSiCodec());
        codecService.registerCodec(NiciraSetNshSpi.class, new NiciraSetNshSpiCodec());
        codecService.registerCodec(NiciraSetTunnelDst.class, new NiciraSetTunnelDstCodec());
        codecService.registerCodec(NiciraSetNshContextHeader.class, new NiciraSetNshContextHeaderCodec());
        codecService.registerCodec(OfdpaMatchVlanVid.class, new OfdpaMatchVlanVidCodec());
        codecService.registerCodec(OfdpaSetVlanVid.class, new OfdpaSetVlanVidCodec());
        codecService.registerCodec(Ofdpa3SetMplsType.class, new Ofdpa3SetMplsTypeCodec());
        codecService.registerCodec(Ofdpa3MatchOvid.class, new Ofdpa3MatchOvidCodec());
        codecService.registerCodec(Ofdpa3SetOvid.class, new Ofdpa3SetOvidCodec());
        codecService.registerCodec(Ofdpa3SetQosIndex.class, new Ofdpa3SetQosIndexCodec());
        codecService.registerCodec(Ofdpa3MatchMplsL2Port.class, new Ofdpa3MatchMplsL2PortCodec());
        codecService.registerCodec(Ofdpa3SetMplsL2Port.class, new Ofdpa3SetMplsL2PortCodec());
        codecService.registerCodec(OplinkAttenuation.class, new OplinkAttenuationCodec());
        log.info("Registered default driver codecs.");
    }

    @Deactivate
    public void deactivate() {
        codecService.unregisterCodec(MoveExtensionTreatment.class);
        codecService.unregisterCodec(NiciraMatchNshSi.class);
        codecService.unregisterCodec(NiciraMatchNshSpi.class);
        codecService.unregisterCodec(NiciraResubmit.class);
        codecService.unregisterCodec(NiciraResubmitTable.class);
        codecService.unregisterCodec(NiciraSetNshSi.class);
        codecService.unregisterCodec(NiciraSetNshSpi.class);
        codecService.unregisterCodec(NiciraSetTunnelDst.class);
        codecService.unregisterCodec(NiciraSetNshContextHeader.class);
        codecService.unregisterCodec(OfdpaMatchVlanVid.class);
        codecService.unregisterCodec(OfdpaSetVlanVid.class);
        codecService.unregisterCodec(Ofdpa3SetMplsType.class);
        codecService.unregisterCodec(Ofdpa3MatchOvid.class);
        codecService.unregisterCodec(Ofdpa3SetOvid.class);
        codecService.unregisterCodec(Ofdpa3SetQosIndex.class);
        codecService.unregisterCodec(Ofdpa3MatchMplsL2Port.class);
        codecService.unregisterCodec(Ofdpa3SetMplsL2Port.class);
        codecService.unregisterCodec(OplinkAttenuation.class);
        log.info("Unregistered default driver codecs.");
    }
}
