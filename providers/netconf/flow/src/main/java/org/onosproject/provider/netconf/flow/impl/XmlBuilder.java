/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.provider.netconf.flow.impl;

import static org.slf4j.LoggerFactory.getLogger;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.AccessList;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.list.access.list.entries.matches.AceType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.list.access.list.entries.matches.ace.type.AceEth;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.list.access.list.entries.matches.ace.type.AceIp;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.list.access.list.entries.matches.ace.type.ace.ip.AceIpVersion;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.list.access.list.entries.matches.ace.type.ace.ip.ace.ip.version.AceIpv4;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.acl.rev140520.access.list.access.list.entries.matches.ace.type.ace.ip.ace.ip.version.AceIpv6;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.PortNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.packet.fields.rev140625.acl.transport.header.fields.DestinationPortRange;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.packet.fields.rev140625.acl.transport.header.fields.SourcePortRange;
import org.slf4j.Logger;

/**
 * Xml Builder to generate the xml according to given ACL model.
 */
public class XmlBuilder {
    private final Logger log = getLogger(XmlBuilder.class);

    public String buildAclRequestXml(AccessList accessList) {
        Document doc = new Document();
        Namespace namespaceRpc = Namespace
                .getNamespace("urn:ietf:params:xml:ns:netconf:base:1.0");
        Namespace accessNamespaceRpc = Namespace
                .getNamespace("urn:ietf:params:xml:ns:yang:ietf-acl");
        doc.setRootElement(new Element("rpc", namespaceRpc)
                .setAttribute("message-id", "101"));

        /**
         * Access list elements of given ACL model.
         */
        Element access = new Element("access-list", accessNamespaceRpc);
        access.addContent(new Element("acl-name", accessNamespaceRpc)
                .setText(accessList.getAclName()));
        // access.addContent(accessEntries);

        if (!accessList.getAccessListEntries().isEmpty()
                && accessList.getAccessListEntries() != null) {
            for (int accessEntryIntVlu = 0; accessEntryIntVlu < accessList
                    .getAccessListEntries().size(); accessEntryIntVlu++) {
                access.addContent(getAccessEntries(accessEntryIntVlu,
                                                   accessList,
                                                   accessNamespaceRpc));
            }
        }

        /**
         * edit-config operation for given ACL model.
         */
        Element editConfig = new Element("edit-config", namespaceRpc);
        editConfig.addContent(new Element("target", namespaceRpc)
                .addContent(new Element("running", namespaceRpc)));
        editConfig.addContent(new Element("config", Namespace
                .getNamespace("urn:ietf:params:xml:ns:netconf:base:1.0"))
                .addContent(access));

        doc.getRootElement().addContent(editConfig);
        XMLOutputter xmlOutputter = new XMLOutputter(Format.getPrettyFormat());
        String outputString = xmlOutputter.outputString(doc);

        return outputString;
    }

    /**
     * access entries operation for given ACL model.
     */
    private Element getAccessEntries(int accessEntryIntVlu,
                                     AccessList accessList,
                                     Namespace accessNamespaceRpc) {

        /**
         * Port Number
         */

        int srcPortRangeLower = 0;
        int srcPortRangeUpper = 0;
        int destPortRangeLower = 0;
        int destPortRangeUpper = 0;

        String sourceIpAdd = "";
        String destinationIpAdd = "";

        /*
         * checking accessList is null or not
         */
        if (accessList != null) {
            /*
             * checking list entries are empty or null
             */
            if (!accessList.getAccessListEntries().isEmpty()
                    && accessList.getAccessListEntries() != null) {
                AceType aceType = accessList.getAccessListEntries()
                        .get(accessEntryIntVlu).getMatches().getAceType();

                if (aceType instanceof AceIp) {
                    AceIp aceIp = (AceIp) aceType;
                    SourcePortRange sourcePortRange = aceIp
                            .getSourcePortRange();
                    if (sourcePortRange != null) {
                        PortNumber lowerPort = sourcePortRange.getLowerPort();
                        PortNumber upperPort = sourcePortRange.getUpperPort();

                        if (lowerPort != null) {
                            srcPortRangeLower = lowerPort.getValue();
                        }
                        if (upperPort != null) {
                            srcPortRangeUpper = upperPort.getValue();
                        }
                    }
                    DestinationPortRange destinationPortRange = aceIp
                            .getDestinationPortRange();

                    if (destinationPortRange != null) {
                        PortNumber lowerPort = destinationPortRange
                                .getLowerPort();
                        if (lowerPort != null) {
                            destPortRangeLower = lowerPort.getValue();
                        }

                        PortNumber upperPort = destinationPortRange
                                .getUpperPort();
                        if (upperPort != null) {
                            destPortRangeUpper = upperPort.getValue();
                        }

                    }

                    AceIpVersion aceIpVersion = aceIp.getAceIpVersion();
                    if (aceIpVersion instanceof AceIpv4) {
                        AceIpv4 obj = (AceIpv4) aceIpVersion;
                        destinationIpAdd = obj.getDestinationIpv4Address()
                                .getValue();
                        sourceIpAdd = obj.getSourceIpv4Address().getValue();
                    } else if (aceIpVersion instanceof AceIpv6) {
                        AceIpv6 obj = (AceIpv6) aceIpVersion;
                        destinationIpAdd = obj.getDestinationIpv6Address()
                                .getValue();
                        sourceIpAdd = obj.getSourceIpv6Address().getValue();
                    }
                } else if (aceType instanceof AceEth) {
                    log.debug("Need to add execution loging for Ace Type Ethernet");
                }
            }
        }

        /**
         * Matches elements to define IP address & Port range for given ACL
         * model.
         */
        Element matchesElement = new Element("matches", accessNamespaceRpc);
        if (String.valueOf(srcPortRangeLower) != null
                && !String.valueOf(srcPortRangeLower).isEmpty()) {

            matchesElement.addContent(new Element("source-port-range",
                                                  accessNamespaceRpc)
                    .addContent(new Element("lower-port", accessNamespaceRpc)
                            .setText(String.valueOf(srcPortRangeLower))));

            matchesElement.addContent(new Element("source-port-range",
                                                  accessNamespaceRpc)
                    .addContent(new Element("upper-port", accessNamespaceRpc)
                            .setText(String.valueOf(srcPortRangeUpper))));

            matchesElement.addContent(new Element("destination-port-range",
                                                  accessNamespaceRpc)
                    .addContent(new Element("lower-port", accessNamespaceRpc)
                            .setText(String.valueOf(destPortRangeLower))));

            matchesElement.addContent(new Element("destination-port-range",
                                                  accessNamespaceRpc)
                    .addContent(new Element("upper-port", accessNamespaceRpc)
                            .setText(String.valueOf(destPortRangeUpper))));
        }

        if (destinationIpAdd != null && !destinationIpAdd.isEmpty()) {
            matchesElement.addContent(new Element("destination-ipv4-address",
                                                  accessNamespaceRpc)
                    .setText(destinationIpAdd));
        }
        if (sourceIpAdd != null && !sourceIpAdd.isEmpty()) {
            matchesElement.addContent(new Element("source-ipv4-address",
                                                  accessNamespaceRpc)
                    .setText(sourceIpAdd));
        }

        /**
         * Access entries elements for given ACL model.
         */
        Element accessEntries = new Element("access-list-entries",
                                            accessNamespaceRpc);
        accessEntries.addContent(new Element("rule-name", accessNamespaceRpc)
                .setText(accessList.getAccessListEntries()
                        .get(accessEntryIntVlu).getRuleName()));
        accessEntries.addContent(matchesElement);
        accessEntries.addContent(new Element("actions", accessNamespaceRpc)
                .addContent(new Element("deny", accessNamespaceRpc)));

        return accessEntries;
    }
}
