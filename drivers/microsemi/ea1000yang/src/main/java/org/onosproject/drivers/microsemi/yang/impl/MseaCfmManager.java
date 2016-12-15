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
package org.onosproject.drivers.microsemi.yang.impl;

import static org.onosproject.yms.ych.YangProtocolEncodingFormat.XML;
import static org.onosproject.yms.ydt.YmsOperationType.QUERY_REPLY;

import java.util.List;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.drivers.microsemi.yang.MseaCfmNetconfService;
import org.onosproject.netconf.NetconfException;
import org.onosproject.netconf.NetconfSession;
import org.onosproject.netconf.TargetConfig;
import org.onosproject.yang.gen.v1.http.www.microsemi.com.microsemi.edge.assure.msea.cfm.rev20160229.MseaCfm;
import org.onosproject.yang.gen.v1.http.www.microsemi.com.microsemi.edge.assure.msea.cfm.rev20160229.MseaCfmOpParam;
import org.onosproject.yang.gen.v1.http.www.microsemi.com.microsemi.edge.assure.msea.cfm.rev20160229.MseaCfmService;
import org.onosproject.yang.gen.v1.http.www.microsemi.com.microsemi.edge.assure.msea.cfm.rev20160229.mseacfm.MseaCfmEventListener;
import org.onosproject.yang.gen.v1.http.www.microsemi.com.microsemi.edge.assure.msea.cfm.rev20160229.mseacfm.abortloopback.AbortLoopbackInput;
import org.onosproject.yang.gen.v1.http.www.microsemi.com.microsemi.edge.assure.msea.cfm.rev20160229.mseacfm.transmitlinktrace.TransmitLinktraceInput;
import org.onosproject.yang.gen.v1.http.www.microsemi.com.microsemi.edge.assure.msea.cfm.rev20160229.mseacfm.transmitlinktrace.TransmitLinktraceOutput;
import org.onosproject.yang.gen.v1.http.www.microsemi.com.microsemi.edge.assure.msea.cfm.rev20160229.mseacfm.transmitloopback.TransmitLoopbackInput;
import org.onosproject.yang.gen.v1.http.www.microsemi.com.microsemi.edge.assure.msea.soam.fm.rev20160229.MseaSoamFmService;
import org.onosproject.yang.gen.v1.http.www.microsemi.com.microsemi.edge.assure.msea.soam.pm.rev20160229.MseaSoamPmService;

/**
 * Implementation of the MseaCfmServiceNetconf YANG model service.
 */
@Component(immediate = true, inherit = true)
@Service
public class MseaCfmManager extends AbstractYangServiceImpl
    implements MseaCfmNetconfService {

    public static final String MSEA_CFM = "org.onosproject.drivers.microsemi.yang.mseacfmservice";

    @Activate
    public void activate() {
        appId = coreService.registerApplication(MSEA_CFM);
        ych = ymsService.getYangCodecHandler();
        ych.addDeviceSchema(MseaCfmService.class);
        ych.addDeviceSchema(MseaSoamFmService.class);
        ych.addDeviceSchema(MseaSoamPmService.class);
        log.info("MseaCfmService Started");
    }

    @Deactivate
    public void deactivate() {
        super.deactivate();
        ymsService.unRegisterService(this, MseaCfmService.class);
        ymsService.unRegisterService(this, MseaSoamFmService.class);
        ymsService.unRegisterService(this, MseaSoamPmService.class);
        ych = null;
        log.info("MseaCfmService Stopped");
    }

    @Override
    public MseaCfm getMepEssentials(String mdName, String maName, int mepId,
            NetconfSession session) throws NetconfException {
        if (session == null) {
            throw new NetconfException("Session is null when calling getMepEssentials()");
        }

        String xmlQueryStr = buildMepQueryString(mdName, maName, mepId);
        log.debug("Sending <get> for " +
                " query on NETCONF session " + session.getSessionId() +
                ":\n" + xmlQueryStr);

        String xmlResult = session.get(xmlQueryStr, null);
        //FIXME Line is removed because YCH decode does not know how to handle it
        xmlResult = xmlResult.replaceAll("(<ccm-interval>)(3.3ms|10ms|100ms|1s)(</ccm-interval>)", "");
        xmlResult = xmlResult.replaceAll("(<active-defects/>)", "");

        List<Object> objectList = ych.decode(xmlResult, XML, QUERY_REPLY);
        if (objectList != null && objectList.size() > 0) {
            Object systemObject = objectList.get(0);
            return (MseaCfm) systemObject;
        } else {
            throw new NetconfException("Failure of YCH decode - could not parse as MseaCfm: " + xmlResult);
        }
    }

    @Override
    public MseaCfm getSoamDm(String mdName, String maName, int mepId,
            int dmId, NetconfSession session) throws NetconfException {
        String xmlQueryStr = buildDmQueryString(mdName, maName, mepId, dmId);
        log.debug("Sending <get> for " +
                " query on NETCONF session " + session.getSessionId() +
                ":\n" + xmlQueryStr);

        String xmlResult = session.get(xmlQueryStr, null);

        List<Object> objectList = ych.decode(xmlResult, XML, QUERY_REPLY);
        if (objectList != null && objectList.size() > 0) {
            Object systemObject = objectList.get(0);
            return (MseaCfm) systemObject;
        } else {
            throw new NetconfException("Failure of YCH decode - could not parse as MseaCfm: " + xmlResult);
        }
    }

    @Override
    public void setMseaCfm(MseaCfmOpParam mseaCfm, NetconfSession session, TargetConfig targetDs)
            throws NetconfException {
        setNetconfObject(mseaCfm, session, targetDs);
    }

    /**
     * Call RPCs on the device through NETCONF.
     */
    @Override
    public void transmitLoopback(TransmitLoopbackInput inputVar, NetconfSession session) throws NetconfException {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void abortLoopback(AbortLoopbackInput inputVar, NetconfSession session) throws NetconfException {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public TransmitLinktraceOutput transmitLinktrace(TransmitLinktraceInput inputVar, NetconfSession session)
            throws NetconfException {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void addListener(MseaCfmEventListener listener) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void removeListener(MseaCfmEventListener listener) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private String buildMepQueryString(String mdName, String maName, int mepId) {
        StringBuilder rpc = new StringBuilder();

        rpc.append("<mef-cfm xmlns=\"http://www.microsemi.com/microsemi-edge-assure/msea-cfm\" ");
        rpc.append(" xmlns:msea-soam-fm=\"http://www.microsemi.com/microsemi-edge-assure/msea-soam-fm\" ");
        rpc.append("xmlns:msea-soam-pm=\"http://www.microsemi.com/microsemi-edge-assure/msea-soam-pm\">\n");
        rpc.append("<maintenance-domain>\n");
        rpc.append("<id/>\n");
        rpc.append("<name>" + mdName + "</name>\n");
        rpc.append("<md-level/>\n");
        rpc.append("<maintenance-association>\n");
        rpc.append("<id/>\n");
        rpc.append("<name>" + maName + "</name>\n");
//        rpc.append("<ccm-interval>10ms</ccm-interval>\n"); //Have to omit for the moment - YMS problem
        rpc.append("<remote-meps/>\n");
        rpc.append("<component-list/>\n");
        rpc.append("<maintenance-association-end-point>\n");
        rpc.append("<mep-identifier>" + mepId + "</mep-identifier>\n");
        rpc.append("<interface/>\n");
        rpc.append("<primary-vid/>\n");
        rpc.append("<administrative-state/>\n");
        rpc.append("<ccm-ltm-priority/>\n");
        rpc.append("<continuity-check/>\n");
        rpc.append("<mac-address/>\n");
        rpc.append("<msea-soam-fm:port-status/>\n");
        rpc.append("<msea-soam-fm:interface-status/>\n");
//        rpc.append("<msea-soam-fm:last-defect-sent/>\n");//Have to omit for the moment - YMS problem
        rpc.append("<msea-soam-fm:rdi-transmit-status/>\n");
        rpc.append("<loopback/>\n");
        rpc.append("<remote-mep-database/>\n");
        rpc.append("<linktrace/>\n");
        rpc.append("</maintenance-association-end-point>\n");
        rpc.append("</maintenance-association>\n");
        rpc.append("</maintenance-domain>\n");
        rpc.append("</mef-cfm>");

        return rpc.toString();
    }


    private String buildDmQueryString(String mdName, String maName, int mepId, int dmId) {
        StringBuilder rpc = new StringBuilder();

        rpc.append("<mef-cfm xmlns=\"http://www.microsemi.com/microsemi-edge-assure/msea-cfm\" ");
        rpc.append(" xmlns:msea-soam-fm=\"http://www.microsemi.com/microsemi-edge-assure/msea-soam-fm\" ");
        rpc.append("xmlns:msea-soam-pm=\"http://www.microsemi.com/microsemi-edge-assure/msea-soam-pm\">\n");
        rpc.append("<maintenance-domain>\n");
        rpc.append("<id/>\n");
        rpc.append("<name>" + mdName + "</name>\n");
        rpc.append("<md-level/>\n");
        rpc.append("<maintenance-association>\n");
        rpc.append("<id/>\n");
        rpc.append("<name>" + maName + "</name>\n");
//        rpc.append("<ccm-interval>10ms</ccm-interval>\n"); //Have to omit for the moment - YMS problem
        rpc.append("<maintenance-association-end-point>\n");
        rpc.append("<mep-identifier>" + mepId + "</mep-identifier>\n");
        rpc.append("<msea-soam-pm:delay-measurements>");
        rpc.append("<msea-soam-pm:delay-measurement>");
        rpc.append("<msea-soam-pm:dm-id>" + dmId + "</msea-soam-pm:dm-id>");
        rpc.append("</msea-soam-pm:delay-measurement>");
        rpc.append("</msea-soam-pm:delay-measurements>");
        rpc.append("</maintenance-association-end-point>\n");
        rpc.append("</maintenance-association>\n");
        rpc.append("</maintenance-domain>\n");
        rpc.append("</mef-cfm>");

        return rpc.toString();
    }

}
