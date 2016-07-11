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
package org.onosproject.drivers.huawei.util;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.onosproject.drivers.huawei.HuaWeiL3vpnConfig.FilterType;
import org.onosproject.drivers.huawei.HuaWeiL3vpnConfig.NetconfConfigDatastoreType;
import org.onosproject.net.behaviour.NetconfBgp;
import org.onosproject.net.behaviour.NetconfBgpVrf;
import org.onosproject.net.behaviour.NetconfBgpVrfAF;
import org.onosproject.net.behaviour.NetconfImportRoute;
import org.onosproject.net.behaviour.NetconfL3vpn;
import org.onosproject.net.behaviour.NetconfL3vpnIf;
import org.onosproject.net.behaviour.NetconfL3vpnInstance;
import org.onosproject.net.behaviour.NetconfVpnInstAF;
import org.onosproject.net.behaviour.NetconfVpnTarget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DocumentConvertUtil Document Convert Util.
 */
public final class DocumentConvertUtil {
    private static final Logger log = LoggerFactory
            .getLogger(DocumentConvertUtil.class);

    /**
     * Constructs a DataConvertUtil object. Utility classes should not have a
     * public or default constructor, otherwise IDE will compile unsuccessfully.
     * This class should not be instantiated.
     */
    private DocumentConvertUtil() {
    }

    /**
     * Convert To l3vpn Document.
     *
     * @param rpcXmlns rpc element attribute xmlns
     * @param messageId message identifier
     * @param datastoreType Netconf Config Datastore Type
     * @param errorOperation error operation
     * @param configXmlns l3vpn element attribute xmlns
     * @param netconfL3vpn NetconfL3vpn
     * @return Document
     */
    public static Document convertEditL3vpnDocument(String rpcXmlns,
                                                    String messageId,
                                                    NetconfConfigDatastoreType datastoreType,
                                                    String errorOperation,
                                                    String configXmlns,
                                                    NetconfL3vpn netconfL3vpn) {
        Document rpcDoc = convertRpcDocument(rpcXmlns, messageId);
        Document editDoc = convertEditConfigDocument(datastoreType,
                                                     errorOperation);
        Document l3vpnDoc = DocumentHelper.createDocument();
        Element l3vpn = l3vpnDoc.addElement("l3vpn");
        l3vpn.addAttribute("xmlns", configXmlns);
        l3vpn.addAttribute("content-version", netconfL3vpn.contentVersion());
        l3vpn.addAttribute("format-version", netconfL3vpn.formatVersion());
        Element l3vpncommon = l3vpn.addElement("l3vpncommon");
        Element l3vpnInstances = l3vpncommon.addElement("l3vpnInstances");
        for (NetconfL3vpnInstance netconfL3vpnInstance : netconfL3vpn
                .l3vpnComm().l3vpninstances().l3vpninstances()) {
            Element l3vpnInstance = l3vpnInstances.addElement("l3vpnInstance");
            l3vpnInstance.addAttribute("operation",
                                       netconfL3vpnInstance.operation());
            l3vpnInstance.addElement("vrfName")
                    .setText(netconfL3vpnInstance.vrfName());
            Element vpnInstAFs = l3vpnInstance.addElement("vpnInstAFs");
            for (NetconfVpnInstAF netconfVpnInstAF : netconfL3vpnInstance
                    .vpnInstAFs().vpnInstAFs()) {
                Element vpnInstAF = vpnInstAFs.addElement("vpnInstAF");
                vpnInstAF.addElement("afType")
                        .setText(netconfVpnInstAF.afType());
                vpnInstAF.addElement("vrfRD").setText(netconfVpnInstAF.vrfRD());
                Element vpnTargets = vpnInstAFs.addElement("vpnTargets");
                for (NetconfVpnTarget netconfVpnTarget : netconfVpnInstAF
                        .vpnTargets().vpnTargets()) {
                    Element vpnTarget = vpnTargets.addElement("vpnTarget");
                    vpnTarget.addElement("vrfRTType")
                            .setText(netconfVpnTarget.vrfRTType());
                    vpnTarget.addElement("vrfRTValue")
                            .setText(netconfVpnTarget.vrfRTValue());
                }
            }
            Element l3vpnIfs = l3vpnInstance.addElement("l3vpnIfs");
            for (NetconfL3vpnIf netconfL3vpnIf : netconfL3vpnInstance.l3vpnIfs()
                    .l3vpnIfs()) {
                Element l3vpnIf = l3vpnIfs.addElement("l3vpnIf");
                l3vpnIf.addAttribute("operation", netconfL3vpnIf.operation());
                l3vpnIf.addElement("ifName").setText(netconfL3vpnIf.ifName());
                l3vpnIf.addElement("ipv4Addr")
                        .setText(netconfL3vpnIf.ipv4Addr());
                l3vpnIf.addElement("subnetMask")
                        .setText(netconfL3vpnIf.subnetMask());
            }
        }
        editDoc.getRootElement().element("config")
                .add(l3vpnDoc.getRootElement());
        rpcDoc.getRootElement().add(editDoc);
        return rpcDoc;
    }

    /**
     * Convert To bgp Document.
     *
     * @param rpcXmlns rpc element attribute xmlns
     * @param messageId message identifier
     * @param datastoreType Netconf Config Datastore Type
     * @param errorOperation error operation
     * @param configXmlns l3vpn element attribute xmlns
     * @param netconfBgp NetconfBgp
     * @return Document
     */
    public static Document convertEditBgpDocument(String rpcXmlns,
                                                  String messageId,
                                                  NetconfConfigDatastoreType datastoreType,
                                                  String errorOperation,
                                                  String configXmlns,
                                                  NetconfBgp netconfBgp) {
        Document rpcDoc = convertRpcDocument(rpcXmlns, messageId);
        Document editDoc = convertEditConfigDocument(datastoreType,
                                                     errorOperation);
        Document bgpnDoc = DocumentHelper.createDocument();
        Element bgp = bgpnDoc.addElement("bgp");
        bgp.addAttribute("xmlns", configXmlns);
        bgp.addAttribute("content-version", netconfBgp.contentVersion());
        bgp.addAttribute("format-version", netconfBgp.formatVersion());
        Element bgpcommon = bgp.addElement("bgpcommon");
        Element bgpVrfs = bgpcommon.addElement("bgpVrfs");
        for (NetconfBgpVrf netconfBgpVrf : netconfBgp.bgpcomm().bgpVrfs()
                .bgpVrfs()) {
            Element bgpVrf = bgpVrfs.addElement("bgpVrf");
            bgpVrf.addAttribute("operation", netconfBgpVrf.operation());
            bgpVrf.addElement("vrfName").setText(netconfBgpVrf.vrfName());
            Element bgpVrfAFs = bgpVrf.addElement("bgpVrfAFs");
            for (NetconfBgpVrfAF netconfBgpVrfAF : netconfBgpVrf.bgpVrfAFs()
                    .bgpVrfAFs()) {
                Element bgpVrfAF = bgpVrfAFs.addElement("bgpVrfAF");
                bgpVrfAF.addElement("afType").setText(netconfBgpVrfAF.afType());
                Element importRoutes = bgpVrfAF.addElement("importRoutes");
                for (NetconfImportRoute netconfImportRoute : netconfBgpVrfAF
                        .importRoutes().importRoutes()) {
                    Element importRoute = importRoutes
                            .addElement("importRoute");
                    importRoute.addAttribute("operation",
                                             netconfImportRoute.operation());
                    importRoute.addElement("importProtocol")
                            .setText(netconfImportRoute.importProtocol());
                }
            }
        }
        editDoc.getRootElement().element("config")
                .add(bgpnDoc.getRootElement());
        rpcDoc.getRootElement().add(editDoc);
        return rpcDoc;
    }

    /**
     * Convert To Rpc Document.
     *
     * @param xmlns xmlns
     * @param messageId message identifier
     * @return Document
     */
    public static Document convertRpcDocument(String xmlns, String messageId) {
        Document doc = DocumentHelper.createDocument();
        Element rpc = doc.addElement("rpc");
        rpc.addAttribute("xmlns", xmlns);
        rpc.addAttribute("message-id", messageId);
        return doc;
    }

    /**
     * Convert To GET Document.
     *
     * @param type FilterType
     * @return Document
     */
    public static Document convertGetDocument(FilterType type) {
        Document doc = DocumentHelper.createDocument();
        Element get = doc.addElement("get");
        get.addElement("filter");
        get.addAttribute("type", type.name().toLowerCase());
        return doc;
    }

    /**
     * Convert To Edit Config Document.
     *
     * @param type Netconf Config Datastore Type
     * @param errorOperation error operation
     * @return Document
     */
    public static Document convertEditConfigDocument(NetconfConfigDatastoreType type,
                                                     String errorOperation) {
        Document doc = DocumentHelper.createDocument();
        Element editConfig = doc.addElement("edit-config");
        Element target = editConfig.addElement("target");
        target.addElement(type.name().toLowerCase());
        Element operation = editConfig.addElement("error-operation");
        operation.setText(errorOperation);
        editConfig.addElement("config");
        return doc;
    }

}
