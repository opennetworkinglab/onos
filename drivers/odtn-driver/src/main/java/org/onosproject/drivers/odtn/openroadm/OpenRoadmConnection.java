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
 *
 * This work was partially supported by EC H2020 project METRO-HAUL (761727).
 */

package org.onosproject.drivers.odtn.openroadm;

import org.onlab.util.Frequency;
import org.onosproject.net.DeviceId;
import org.onosproject.net.OchSignal;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.FlowId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


class OpenRoadmConnectionBase {

    protected static final Logger log = LoggerFactory.getLogger(OpenRoadmConnection.class);

    // Parameters of the FlowRule traslated into the OpenRoadm connection
    protected DeviceId deviceId;
    protected FlowId id;
    protected int priority;

    protected OpenRoadmFlowRule.Type type; // enum (EXPRESS_LINK, ADD_LINK, DROP_LINK)
    protected OchSignal ochSignal;
    protected PortNumber inPortNumber;
    protected PortNumber outPortNumber;

    protected Port srcPort; // used to retrieve info in the annotations
    protected Port dstPort; // used to retrieve info in the annotations

    // Parameters of <roadm-connections>
    protected String connectionName;
    protected String opticalControlMode;
    protected double targetOutputPower = 0.0;
    protected String srcConnInterface; // this is an NMC interface
    protected String dstConnInterface; // this is an NMC interface

    // Parameters of associated NMC interfaces:
    // <type>openROADM-if:networkMediaChannelConnectionTerminationPoint</type>
    protected String srcNmcName;
    protected String srcNmcDescription;
    protected String srcNmcType;
    protected String srcNmcAdministrativeState;
    protected String srcNmcSupportingCircuitPack;
    protected String srcNmcSupportingInterface; // this is an MC interface (express-link) or a
                                                // physical-port (add-drop)
    protected String srcNmcSupportingPort;
    protected Frequency srcNmcFrequency; // expressed in Thz
    protected Frequency srcNmcWidth;     // expressed in Ghz

    protected String dstNmcName;
    protected String dstNmcDescription;
    protected String dstNmcType;
    protected String dstNmcAdministrativeState;
    protected String dstNmcSupportingCircuitPack;
    protected String dstNmcSupportingInterface; // this is an MC interface (express-link) or a
                                                // physical-port (add-drop)
    protected String dstNmcSupportingPort;
    protected Frequency dstNmcFrequency; // expressed in Thz
    protected Frequency dstNmcWidth;     // expressed in Ghz

    // Parameters of associated MC interfaces:
    // <type>openROADM-if:mediaChannelTrailTerminationPoint</type>
    protected String srcMcName;
    protected String srcMcDescription;
    protected String srcMcType;
    protected String srcMcAdministrativeState;
    protected String srcMcSupportingCircuitPack;
    protected String srcMcSupportingInterface; // this is a physical-port
    protected String srcMcSupportingPort;
    protected Frequency srcMcMinFrequency; // expressed in Thz
    protected Frequency srcMcMaxFrequency; // expressed in Thz

    protected String dstMcName;
    protected String dstMcDescription;
    protected String dstMcType;
    protected String dstMcAdministrativeState;
    protected String dstMcSupportingCircuitPack;
    protected String dstMcSupportingInterface; // this is a physical-port
    protected String dstMcSupportingPort;
    protected Frequency dstMcMinFrequency; // expressed in Thz
    protected Frequency dstMcMaxFrequency; // expressed in Thz



    public OpenRoadmFlowRule.Type getType() {
        return type;
    }
}



/**
 * Class that models an OpenROADM connection object (Yang leaf).
 *
 */
public class OpenRoadmConnection extends OpenRoadmConnectionBase {

    public static final String OPENROADM_CIRCUIT_PACK_NAME = "openroadm-circuit-pack-name";

    public static final String OPENROADM_PORT_NAME = "openroadm-port-name";

    public static final String OPENROADM_LOGICAL_CONNECTION_POINT =
      "openroadm-logical-connection-point";

    /**
     * Constructor.
     *
     * @param openRoadmName name of the Connection.
     * @param xc the associated OpenRoadmFlowRule.
     * @param deviceService ONOS device service.
     */
    public OpenRoadmConnection(String openRoadmName, OpenRoadmFlowRule xc,
                               DeviceService deviceService) {
      connectionName = openRoadmName;
      deviceId = xc.deviceId();
      id = xc.id();
      priority = xc.priority();

      inPortNumber = xc.inPort();
      outPortNumber = xc.outPort();
      ochSignal = xc.ochSignal();
      type = xc.type();

      srcPort = deviceService.getPort(deviceId, xc.inPort());
      dstPort = deviceService.getPort(deviceId, xc.outPort());

      // Conversion from ochSignal (center frequency + diameter) to OpenRoadm
      // Media Channel (start - end)
      Frequency freqRadius = Frequency.ofHz(
          xc.ochSignal().channelSpacing().frequency().asHz() / 2);
      Frequency centerFreq = xc.ochSignal().centralFrequency();

      // e.g. DEG1-TTP-RX
      String srcTag =
          srcPort.annotations().value(OPENROADM_LOGICAL_CONNECTION_POINT) +
          "-" + centerFreq.asTHz();

      // e.g. DEG2-TTP-TX or SRG2-PP1-TX
      String dstTag =
          dstPort.annotations().value(OPENROADM_LOGICAL_CONNECTION_POINT) +
          "-" + centerFreq.asTHz();

      srcMcMinFrequency = centerFreq.subtract(freqRadius);
      srcMcMaxFrequency = centerFreq.add(freqRadius);
      dstMcMinFrequency = srcMcMinFrequency;
      dstMcMaxFrequency = srcMcMaxFrequency;
      srcNmcFrequency = centerFreq;
      dstNmcFrequency = centerFreq;
      srcNmcWidth = xc.ochSignal().channelSpacing().frequency();
      dstNmcWidth = xc.ochSignal().channelSpacing().frequency();

      srcMcSupportingInterface =
          "OMS-" +
          srcPort.annotations().value(OPENROADM_LOGICAL_CONNECTION_POINT);
      dstMcSupportingInterface =
          "OMS-" +
          dstPort.annotations().value(OPENROADM_LOGICAL_CONNECTION_POINT);

      // Media Channel Interfaces
      srcMcName = "MC-TTP-" + srcTag;
      srcMcSupportingCircuitPack =
          srcPort.annotations().value(OPENROADM_CIRCUIT_PACK_NAME);
      srcMcSupportingPort = srcPort.annotations().value(OPENROADM_PORT_NAME);

      dstMcName = "MC-TTP-" + dstTag;
      dstMcSupportingCircuitPack =
          dstPort.annotations().value(OPENROADM_CIRCUIT_PACK_NAME);
      dstMcSupportingPort = dstPort.annotations().value(OPENROADM_PORT_NAME);

      // Network Media Channel Interfaces
      srcNmcName = "NMC-CTP-" + srcTag;
      srcConnInterface = srcNmcName;
      srcNmcSupportingInterface = srcMcName;
      srcNmcSupportingCircuitPack =
          srcPort.annotations().value(OPENROADM_CIRCUIT_PACK_NAME);
      srcNmcSupportingPort = srcPort.annotations().value(OPENROADM_PORT_NAME);

      dstNmcName = "NMC-CTP-" + dstTag;
      dstConnInterface = dstNmcName;
      dstNmcSupportingInterface = dstMcName;
      dstNmcSupportingCircuitPack =
          dstPort.annotations().value(OPENROADM_CIRCUIT_PACK_NAME);
      dstNmcSupportingPort = dstPort.annotations().value(OPENROADM_PORT_NAME);
    }

    protected String getConnectionName() {
        return connectionName;
    }
}
