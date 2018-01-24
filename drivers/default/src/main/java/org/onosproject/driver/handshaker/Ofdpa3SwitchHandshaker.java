/*
 * Copyright 2016-present Open Networking Foundation
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

package org.onosproject.driver.handshaker;

import org.projectfloodlight.openflow.protocol.OFCapabilities;
import org.projectfloodlight.openflow.protocol.OFFeaturesReply;

import java.util.HashSet;
import java.util.Set;

/**
 * Driver for ofdpa 3 switches
 * TODO : Remove this and also remove the specific switch handler from
 * onos-drivers.xml once bug with GROUP_STATS is fixed.
 */
public class Ofdpa3SwitchHandshaker extends DefaultSwitchHandshaker {

    @Override
    public void setFeaturesReply(OFFeaturesReply featuresReply) {

        OFFeaturesReply.Builder builder = featuresReply.createBuilder();

        // do not try to set PORTS or ACTIONS,
        // they are not supported for this openflow version
        builder.setAuxiliaryId(featuresReply.getAuxiliaryId());
        builder.setDatapathId(featuresReply.getDatapathId());
        builder.setNBuffers(featuresReply.getNBuffers());
        builder.setReserved(featuresReply.getReserved());
        builder.setXid(featuresReply.getXid());

        Set<OFCapabilities> capabilities = new HashSet<>(featuresReply.getCapabilities());
        capabilities.add(OFCapabilities.GROUP_STATS);
        builder.setCapabilities(capabilities);

        super.setFeaturesReply(builder.build());
    }

}
