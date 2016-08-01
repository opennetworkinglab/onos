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

package org.onosproject.vtnweb.gui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.onlab.packet.IpAddress;
import org.onosproject.ui.RequestHandler;
import org.onosproject.ui.UiMessageHandler;
import org.onosproject.ui.table.TableModel;
import org.onosproject.ui.table.TableRequestHandler;
import org.onosproject.vtnrsc.FixedIp;
import org.onosproject.vtnrsc.FlowClassifier;
import org.onosproject.vtnrsc.FlowClassifierId;
import org.onosproject.vtnrsc.PortChain;
import org.onosproject.vtnrsc.PortPair;
import org.onosproject.vtnrsc.PortPairGroup;
import org.onosproject.vtnrsc.PortPairGroupId;
import org.onosproject.vtnrsc.PortPairId;
import org.onosproject.vtnrsc.VirtualPort;
import org.onosproject.vtnrsc.VirtualPortId;
import org.onosproject.vtnrsc.flowclassifier.FlowClassifierService;
import org.onosproject.vtnrsc.portchain.PortChainService;
import org.onosproject.vtnrsc.portpair.PortPairService;
import org.onosproject.vtnrsc.portpairgroup.PortPairGroupService;
import org.onosproject.vtnrsc.virtualport.VirtualPortService;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableSet;

/**
 * Message handler for service function chain view related messages.
 */
public class SfcViewMessageHandler extends UiMessageHandler {

    private static final String SLASH = " -> ";
    private static final String NONE = "none";
    private static final String SFCTYPE = "Service Function Chain";

    private static final String SFC_DATA_REQ = "sfcDataRequest";
    private static final String SFC_DATA_RESP = "sfcDataResponse";
    private static final String SFCS = "sfcs";

    private static final String ID = "id";
    private static final String STATE = "_iconid_state";
    private static final String PORTCHAINNAME = "portChainName";
    private static final String SFS = "sfs";
    private static final String TYPE = "type";
    private static final String SRCIP = "srcIp";
    private static final String DSTIP = "dstIp";

    private static final String[] COL_IDS = {
                                             ID, STATE, PORTCHAINNAME, SFS, TYPE, SRCIP, DSTIP
    };

    private static final String ICON_ID_ONLINE = "active";
    private static final String ICON_ID_OFFLINE = "inactive";

    @Override
    protected Collection<RequestHandler> createRequestHandlers() {
        return ImmutableSet.of(new SfcDataRequest());
    }

    // handler for sfc table requests
    private final class SfcDataRequest extends TableRequestHandler {

        private static final String NO_ROWS_MESSAGE = "No Service Function Chain found";

        private SfcDataRequest() {
            super(SFC_DATA_REQ, SFC_DATA_RESP, SFCS);
        }

        @Override
        protected String[] getColumnIds() {
            return COL_IDS;
        }

        @Override
        protected String defaultColumnId() {
            return PORTCHAINNAME;
        }

        @Override
        protected String noRowsMessage(ObjectNode payload) {
            return NO_ROWS_MESSAGE;
        }

        @Override
        protected void populateTable(TableModel tm, ObjectNode payload) {
            PortChainService pcs = get(PortChainService.class);
            Iterable<PortChain> portChains = pcs.getPortChains();
            portChains.forEach(pchain -> populateRow(tm.addRow(), pchain));
        }

        //populate the row of service function chain
        private void populateRow(TableModel.Row row, PortChain pchain) {
            PortChainIpRange portChainIpRange = portChainIpRange(pchain);
            List<VirtualPort> vpList = sfcPorts(pchain);
            row.cell(ID, pchain.portChainId().value().toString())
                .cell(STATE, sfcState(vpList))
                .cell(PORTCHAINNAME, pchain.name())
                .cell(SFS, sfcHosts(vpList))
                .cell(TYPE, SFCTYPE)
                .cell(SRCIP, portChainIpRange.srcip())
                .cell(DSTIP, portChainIpRange.dstip());
        }

        //PortChainIpRange
        private PortChainIpRange portChainIpRange(PortChain pchain) {
            List<FlowClassifierId> flowClassifierList = pchain.flowClassifiers();
            FlowClassifierService fcs = get(FlowClassifierService.class);
            StringBuffer srcipbuf = new StringBuffer();
            StringBuffer dstipbuf = new StringBuffer();
            if (flowClassifierList != null) {
                flowClassifierList.forEach(fcid -> {
                    FlowClassifier fc = fcs.getFlowClassifier(fcid);
                    String srcip = fc.srcIpPrefix().toString();
                    String dstip = fc.dstIpPrefix().toString();
                    srcipbuf.append(srcip).append(SLASH);
                    dstipbuf.append(dstip).append(SLASH);
                });
            }
            String srcip = NONE;
            String dstip = NONE;
            if (srcipbuf.length() > 0) {
                srcip = srcipbuf.substring(0, srcipbuf.length() - SLASH.length());
            }
            if (dstipbuf.length() > 0) {
                dstip = dstipbuf.substring(0, dstipbuf.length() - SLASH.length());
            }
            PortChainIpRange portChainIpRange = new PortChainIpRange(srcip, dstip);
            return portChainIpRange;
        }

        //the VirtualPorts of service function chain
        private List<VirtualPort> sfcPorts(PortChain pchain) {
            List<PortPairGroupId> portPairGroupList = pchain.portPairGroups();
            PortPairGroupService ppgs = get(PortPairGroupService.class);
            PortPairService pps = get(PortPairService.class);
            VirtualPortService vps = get(VirtualPortService.class);
            List<VirtualPort> vpList = new ArrayList<VirtualPort>();
            if (portPairGroupList != null) {
                portPairGroupList.forEach(ppgid -> {
                    PortPairGroup ppg = ppgs.getPortPairGroup(ppgid);
                    List<PortPairId> portPairList = ppg.portPairs();
                    if (portPairList != null) {
                        portPairList.forEach(ppid -> {
                            PortPair pp = pps.getPortPair(ppid);
                            VirtualPort vp = vps.getPort(VirtualPortId.portId(pp.ingress()));
                            vpList.add(vp);
                        });
                    }
                });
            }
            return vpList;
        }

        //the state of service function chain
        private String sfcState(List<VirtualPort> vpList) {
            for (VirtualPort vp : vpList) {
                if (vp.state().equals(VirtualPort.State.DOWN)) {
                    return ICON_ID_OFFLINE;
                }
            }
            return ICON_ID_ONLINE;
        }

        //the hosts of service function chain
        private String sfcHosts(List<VirtualPort> vpList) {
            StringBuffer hostsbuf = new StringBuffer();
            for (VirtualPort vp : vpList) {
                Iterator<FixedIp> fixedIps = vp.fixedIps().iterator();
                if (fixedIps.hasNext()) {
                    FixedIp fixedIp = fixedIps.next();
                    IpAddress ip = fixedIp.ip();
                    hostsbuf.append(ip.toString()).append(SLASH);
                }
            }
            if (hostsbuf.length() > 0) {
                return hostsbuf.substring(0, hostsbuf.length() - SLASH.length());
            }
            return hostsbuf.toString();
        }

        //source ip prefix and destination ip prefix
        private final class PortChainIpRange {
            private final String srcip;
            private final String dstip;

            private PortChainIpRange(String srcip, String dstip) {
                this.srcip = srcip;
                this.dstip = dstip;
            }

            public String srcip() {
                return srcip;
            }

            public String dstip() {
                return dstip;
            }
        }
    }
}
