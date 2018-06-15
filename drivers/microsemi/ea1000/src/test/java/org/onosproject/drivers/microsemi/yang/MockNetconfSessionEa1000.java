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
package org.onosproject.drivers.microsemi.yang;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import org.onosproject.drivers.microsemi.EA1000CfmMepProgrammableTest;
import org.onosproject.netconf.DatastoreId;
import org.onosproject.netconf.NetconfDeviceInfo;
import org.onosproject.netconf.NetconfDeviceOutputEventListener;
import org.onosproject.netconf.NetconfException;
import org.onosproject.netconf.NetconfSessionAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MockNetconfSessionEa1000 extends NetconfSessionAdapter {
    private static final Logger log = LoggerFactory
            .getLogger(MockNetconfSessionEa1000.class);

    private static final String MESSAGE_ID_STRING = "message-id";
    private static final String EQUAL = "=";
    private static final String RPC_OPEN = "<rpc ";
    private static final String RPC_CLOSE = "</rpc>";
    private static final String GET_OPEN = "<get>";
    private static final String GET_CLOSE = "</get>";
    private static final String NEW_LINE = "\n";
    private static final String SUBTREE_FILTER_OPEN = "<filter type=\"subtree\">";
    private static final String SUBTREE_FILTER_CLOSE = "</filter>";
    private static final String WITH_DEFAULT_OPEN = "<with-defaults ";
    private static final String WITH_DEFAULT_CLOSE = "</with-defaults>";
    private static final String EDIT_CONFIG_OPEN = "<edit-config>";
    private static final String EDIT_CONFIG_CLOSE = "</edit-config>";
    private static final String TARGET_OPEN = "<target>";
    private static final String TARGET_CLOSE = "</target>";
    private static final String DEFAULT_OPERATION_OPEN = "<default-operation>";
    private static final String DEFAULT_OPERATION_CLOSE = "</default-operation>";
    private static final String CONFIG_OPEN = "<config xmlns:nc=\"urn:ietf:params:xml:ns:netconf:base:1.0\">";
    private static final String CONFIG_CLOSE = "</config>";

    private static final String ENDPATTERN = "]]>]]>";
    private static final String XML_HEADER =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
    private static final String NETCONF_BASE_NAMESPACE =
            "xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\"";
    private static final String NETCONF_WITH_DEFAULTS_NAMESPACE =
            "xmlns=\"urn:ietf:params:xml:ns:yang:ietf-netconf-with-defaults\"";


    private Pattern sampleXmlRegex1 =
            Pattern.compile("(<\\?xml).*(<rpc).*(<get>).*(<filter).*"
                    + "(<system).*(<clock/>).*(</system>).*(</filter>)*.(</get>).*(</rpc).*(]]>){2}",
                    Pattern.DOTALL);

    private Pattern sampleXmlRegex2 =
            Pattern.compile("(<\\?xml).*(<rpc).*(<get>).*(<filter).*(<system-state).*(</system-state>).*"
                    + "(<system).*(</system>).*(</filter>).*(</get>).*(</rpc).*(]]>){2}",
                    Pattern.DOTALL);

    private Pattern sampleXmlRegexSaFiltering =
            Pattern.compile("(<\\?xml).*"
                    + "(<rpc).*(<get>)\\R?"
                    + "(<filter type=\"subtree\">)\\R?"
                    + "(<source-ipaddress-filtering).*(</source-ipaddress-filtering>)\\R?"
                    + "(</filter>)\\R?(</get>)\\R?(</rpc>)\\R?(]]>){2}", Pattern.DOTALL);

    private Pattern sampleXmlRegexSaFilteringErrorScenario =
            Pattern.compile("(<\\?xml).*"
                    + "(<rpc).*(<get>)\\R?"
                    + "(<filter type=\"subtree\">)\\R?"
                    + "(<source-ipaddress-filtering).*"
                    + "(<interface-eth0>)\\R?"
                    + "(<source-address-range>)\\R?"
                    + "(<range-id>)10(</range-id>)\\R?"
                    + "(</source-address-range>)\\R?"
                    + "(</interface-eth0>)\\R?"
                    + "(</source-ipaddress-filtering>)\\R?"
                    + "(</filter>)\\R?(</get>)\\R?(</rpc>)\\R?(]]>){2}", Pattern.DOTALL);

    private Pattern sampleXmlRegexEditConfigSaFilt =
            Pattern.compile("(<\\?xml).*(<rpc).*(<edit-config>).*"
                    + "(<target><running/></target>).*(<config xmlns:nc=\"urn:ietf:params:xml:ns:netconf:base:1.0\">).*"
                    + "(<source-ipaddress-filtering).*(<interface-eth0>).*"
                    + "(<source-address-range>).*(<range-id>).*(</range-id>).*"
                    + "(<name>).*(</name>).*(<ipv4-address-prefix>).*(</ipv4-address-prefix>).*"
                    + "(</source-address-range>).*(</interface-eth0>)*(</source-ipaddress-filtering>).*"
                    + "(</config>).*(</edit-config>).*(</rpc>).*(]]>){2}", Pattern.DOTALL);

    private Pattern sampleXmlRegexEditDeleteSaFilt =
            Pattern.compile("(<\\?xml).*(<rpc).*(<edit-config>)\\R?"
                    + "(<target><running/></target>)\\R?"
                    + "(<config xmlns:nc=\"urn:ietf:params:xml:ns:netconf:base:1.0\">)\\R?"
                    + "(<source-ipaddress-filtering "
                    + "xmlns=\"http://www.microsemi.com/microsemi-edge-assure/msea-sa-filtering\">)\\R?"
                    + "(<interface-eth0>)\\R?"
                    + "((<source-address-range nc:operation=\"delete\">)\\R?"
                    + "(<range-id>)[0-9]*(</range-id>)\\R?"
                    + "((<name>)[a-zA-Z0-9]*(</name>))?\\R?"
                    + "((<ipv4-address-prefix>)[0-9\\\\./]*(</ipv4-address-prefix>))?\\R?"
                    + "(</source-address-range>))++\\R?"
                    + "(</interface-eth0>)\\R?"
                    + "(</source-ipaddress-filtering>)\\R?"
                    + "(</config>)\\R?"
                    + "(</edit-config>)\\R?(</rpc>).*(]]>){2}", Pattern.DOTALL);

    private Pattern sampleXmlRegexUniEvc =
            Pattern.compile("(<\\?xml).*(<rpc).*(<get-config>)\\R?"
                    + "(<source>)\\R?(<running/>)\\R?(</source>)\\R?"
                    + "(<filter type=\"subtree\">)\\R?"
                    + "(<mef-services "
                    + "xmlns=\"http://www.microsemi.com/microsemi-edge-assure/msea-uni-evc-service\"/>)\\R?"
                    + "(</filter>)\\R?(</get-config>)\\R?(</rpc>)\\R?(]]>){2}", Pattern.DOTALL);

    private Pattern sampleXmlRegexUniEvcUni =
            Pattern.compile("(<\\?xml).*(<rpc).*(<get-config>)\\R?"
                    + "(<source>)\\R?(<running/>)\\R?(</source>)\\R?"
                    + "(<filter type=\"subtree\">)\\R?"
                    + "(<mef-services "
                    + "xmlns=\"http://www.microsemi.com/microsemi-edge-assure/msea-uni-evc-service\">)\\R?"
                    + "(<uni/>)\\R?"
                    + "(</mef-services>)\\R?"
                    + "(</filter>)\\R?(</get-config>)\\R?(</rpc>)\\R?(]]>){2}", Pattern.DOTALL);

    private Pattern sampleXmlRegexEditConfigUni1Evc =
            Pattern.compile("(<\\?xml).*(<rpc).*(<edit-config>)\\R?"
                    + "(<target><running/></target>)\\R?"
                    + "(<config xmlns:nc=\"urn:ietf:params:xml:ns:netconf:base:1.0\">)\\R?"
                    + "(<mef-services).*(<uni>)\\R?(<name>).*(</name>)\\R?"
                    + "(<evc>)\\R?(<evc-index>).*(</evc-index>)\\R?(<name>).*(</name>)\\R?"
                    + "(<evc-per-uni>)\\R?"
                    + "(<evc-per-uni-c>)\\R?"
                    + "(<ce-vlan-map>)[0-9]*(</ce-vlan-map>)\\R?"
                    + "(<ingress-bwp-group-index>)[0-9]*(</ingress-bwp-group-index>)\\R?"
                    + "(<tag-push>)\\R?(<push-tag-type>)pushStag(</push-tag-type>)\\R?"
                    + "(<outer-tag-vlan>)[0-9]*(</outer-tag-vlan>)\\R?(</tag-push>)\\R?"
                    + "((<flow-mapping>)\\R?"
                    + "(<ce-vlan-id>)[0-9]*(</ce-vlan-id>)\\R?"
                    + "(<flow-id>)[0-9]*(</flow-id>)\\R?"
                    + "(</flow-mapping>)\\R?)*"
                    + "(</evc-per-uni-c>)\\R?"
                    + "(<evc-per-uni-n>)\\R?"
                    + "(<ce-vlan-map>)[0-9\\:\\,]*(</ce-vlan-map>)\\R?"
                    + "(<ingress-bwp-group-index>)[0-9]*(</ingress-bwp-group-index>)\\R?"
                    + "(<tag-pop).*"
                    + "((<flow-mapping>)\\R?"
                    + "(<ce-vlan-id>)[0-9]*(</ce-vlan-id>)\\R?"
                    + "(<flow-id>)[0-9]*(</flow-id>)\\R?"
                    + "(</flow-mapping>)\\R?)*"
                    + "(</evc-per-uni-n>)\\R?"
                    + "(</evc-per-uni>)\\R?"
                    + "(</evc>)\\R?"
                    + "(</uni>)\\R?"
                    + "(</mef-services>)\\R?"
                    + "(</config>)\\R?(</edit-config>)\\R?(</rpc>)\\R?(]]>){2}", Pattern.DOTALL);

    private Pattern sampleXmlRegexEditConfigUni2Evc =
            Pattern.compile("(<\\?xml).*(<rpc).*(<edit-config>)\\R?"
                    + "(<target><running/></target>)\\R?"
                    + "(<config xmlns:nc=\"urn:ietf:params:xml:ns:netconf:base:1.0\">)\\R?"
                    + "(<mef-services).*(<uni>)\\R?(<name>)[0-9a-zA-Z\\-\\:]*(</name>)\\R?"
                    + "(<evc>)\\R?(<evc-index>)[0-9]*(</evc-index>)\\R?(<name>)[0-9a-zA-Z\\-\\:]*(</name>)\\R?"
                    + "(<evc-per-uni>)\\R?"
                    + "(<evc-per-uni-c>)\\R?"
                    + "(<ce-vlan-map>)[0-9\\:\\,]*(</ce-vlan-map>)\\R?"
                    + "(<ingress-bwp-group-index>)[0-9]*(</ingress-bwp-group-index>)\\R?"
                    + "(</evc-per-uni-c>)\\R?"
                    + "(<evc-per-uni-n>)\\R?"
                    + "(<ce-vlan-map>)[0-9\\:\\,]*(</ce-vlan-map>)\\R?"
                    + "(<ingress-bwp-group-index>)[0-9]*(</ingress-bwp-group-index>)\\R?"
                    + "(<evc-per-uni-service-type>).*(</evc-per-uni-service-type>)\\R?"
                    + "(<tag-push>)\\R?(<push-tag-type>)pushStag(</push-tag-type>)\\R?(<outer-tag-vlan>).*"
                    + "(</outer-tag-vlan>)\\R?(</tag-push>)\\R?"
                    + "(</evc-per-uni-n>)\\R?"
                    + "(</evc-per-uni>)\\R?"
                    + "(</evc>)\\R?"
                    + "(<evc>)\\R?(<evc-index>)[0-9]*(</evc-index>)\\R?(<name>)[0-9a-zA-Z\\-\\:]*(</name>)\\R?"
                    + "(<evc-per-uni>)\\R?"
                    + "(<evc-per-uni-c>)\\R?"
                    + "(<ce-vlan-map>)[0-9\\:\\,]*(</ce-vlan-map>)\\R?"
                    + "(<ingress-bwp-group-index>)[0-9]*(</ingress-bwp-group-index>)\\R?"
                    + "(</evc-per-uni-c>)\\R?"
                    + "(<evc-per-uni-n>)\\R?"
                    + "(<ce-vlan-map>)[0-9\\:\\,]*(</ce-vlan-map>)\\R?"
                    + "(<ingress-bwp-group-index>)[0-9]*(</ingress-bwp-group-index>)\\R?"
                    + "(</evc-per-uni-n>)\\R?"
                    + "(</evc-per-uni>)\\R?"
                    + "(</evc>)\\R?(</uni>).*"
                    + "(</mef-services>)\\R?"
                    + "(</config>)\\R?(</edit-config>)\\R?(</rpc>)\\R?(]]>){2}", Pattern.DOTALL);

    private Pattern sampleXmlRegexEditConfigUniProfiles =
            Pattern.compile("(<\\?xml).*(<rpc).*(<edit-config>).*"
                    + "(<target><running/></target>).*(<config xmlns:nc=\"urn:ietf:params:xml:ns:netconf:base:1.0\">).*"
                    + "(<mef-services).*(<profiles>).*"
                    + "(<bwp-group>).*(<group-index>).*(</group-index>).*(</bwp-group>).*"
                    + "(<bwp-group>).*(<group-index>).*(</group-index>).*"
                    + "(<bwp>).*(<cos-index>).*(</cos-index>).*(<color-mode>).*(</color-mode>).*(</bwp>).*"
                    + "(<bwp>).*(<cos-index>).*(</cos-index>).*(<color-mode>).*(</color-mode>).*(</bwp>).*"
                    + "(</bwp-group>).*"
                    + "(</profiles>).*(</mef-services>).*"
                    + "(</config>).*(</edit-config>).*(</rpc>).*(]]>){2}", Pattern.DOTALL);

    private Pattern sampleXmlRegexEditConfigEvcDelete =
            Pattern.compile("(<\\?xml).*(<rpc).*(<edit-config>).*"
                    + "(<target><running/></target>).*(<config xmlns:nc=\"urn:ietf:params:xml:ns:netconf:base:1.0\">).*"
                    + "(<mef-services).*"
                    + "(<uni>).*"
                    + "(<evc nc:operation=\"delete\">).*(<evc-index>).*(</evc-index>).*(</evc>).*"
                    + "(</uni>).*"
                    + "(</mef-services>).*"
                    + "(</config>).*(</edit-config>).*(</rpc>).*(]]>){2}", Pattern.DOTALL);

    private Pattern sampleXmlRegexGetConfigCeVlanMapsEvc =
            Pattern.compile("(<\\?xml).*(<rpc).*(<get-config>)\\R?"
                    + "(<source>)\\R?(<running/>)\\R?(</source>)\\R?"
                    + "(<filter type=\"subtree\">)\\R?"
                    + "(<mef-services).*"
                    + "(<uni>)\\R?"
                    + "(<evc>)\\R?"
                    + "(<evc-index/>)\\R?"
                    + "(<evc-per-uni>)\\R?"
                    + "(<evc-per-uni-c><ce-vlan-map/><flow-mapping/><ingress-bwp-group-index/></evc-per-uni-c>)\\R?"
                    + "(<evc-per-uni-n><ce-vlan-map/><flow-mapping/><ingress-bwp-group-index/></evc-per-uni-n>)\\R?"
                    + "(</evc-per-uni>)\\R?"
                    + "(</evc>)\\R?"
                    + "(</uni>)\\R?"
                    + "(</mef-services>)\\R?"
                    + "(</filter>)\\R?"
                    + "(</get-config>)\\R?"
                    + "(</rpc>)\\R?"
                    + "(]]>){2}", Pattern.DOTALL);

    //For test testRemoveEvcUniFlowEntries()
    private Pattern sampleXmlRegexEditConfigCeVlanMapReplace =
            Pattern.compile("(<\\?xml).*(<rpc).*(<edit-config>).*"
                    + "(<target><running/></target>).*"
                    + "(<config xmlns:nc=\"urn:ietf:params:xml:ns:netconf:base:1.0\">).*"
                    + "(<mef-services xmlns=\"http://www.microsemi.com/microsemi-edge-assure/msea-uni-evc-service\">).*"
                    + "(<uni>).*"
                    + "(<evc nc:operation=\"delete\">).*(<evc-index>.*</evc-index>).*(</evc>).*"
                    + "(<evc nc:operation=\"delete\">).*(<evc-index>.*</evc-index>).*(</evc>).*"
                    + "(<evc>).*(<evc-index>).*(</evc-index>).*(<evc-per-uni>).*"
                    + "(<evc-per-uni-c>).*"
                    + "(<ce-vlan-map nc:operation=\"replace\">).*(</ce-vlan-map>).*"
                    + "(<flow-mapping nc:operation=\"delete\">).*(<ce-vlan-id>).*(</ce-vlan-id>).*(</flow-mapping>).*"
                    + "(</evc-per-uni-c>).*"
                    + "(<evc-per-uni-n>).*"
                    + "(<ce-vlan-map nc:operation=\"replace\">).*(</ce-vlan-map>).*"
                    + "(<flow-mapping nc:operation=\"delete\">).*(<ce-vlan-id>).*(</ce-vlan-id>).*(</flow-mapping>).*"
                    + "(</evc-per-uni-n>).*"
                    + "(</evc-per-uni>).*(</evc>).*"
                    + "(<evc>).*(<evc-index>).*(</evc-index>).*(<evc-per-uni>).*"
                    + "(<evc-per-uni-c>).*"
                    + "(<ce-vlan-map nc:operation=\"replace\">).*(</ce-vlan-map>).*"
                    + "(<flow-mapping nc:operation=\"delete\">).*(<ce-vlan-id>).*(</ce-vlan-id>).*(</flow-mapping>).*"
                    + "(</evc-per-uni-c>).*"
                    + "(<evc-per-uni-n>).*"
                    + "(<ce-vlan-map nc:operation=\"replace\">).*(</ce-vlan-map>).*"
                    + "(<flow-mapping nc:operation=\"delete\">).*(<ce-vlan-id>).*(</ce-vlan-id>).*(</flow-mapping>).*"
                    + "(</evc-per-uni-n>).*"
                    + "(</evc-per-uni>).*(</evc>).*"
                    + "(</uni>).*(</mef-services>).*"
                    + "(</config>).*(</edit-config>).*(</rpc>).*(]]>){2}", Pattern.DOTALL);


    //For testPerformMeterOperationDeviceIdMeterAdd()
    private Pattern sampleXmlRegexEditConfigBwpGroup1 =
            Pattern.compile("(<\\?xml).*(<rpc).*(<edit-config>)\\R?"
                    + "(<target>\\R?<running/>\\R?</target>)\\R?"
                    + "(<config xmlns:nc=\"urn:ietf:params:xml:ns:netconf:base:1.0\">)\\R?"
                    + "(<mef-services "
                    + "xmlns=\"http://www.microsemi.com/microsemi-edge-assure/msea-uni-evc-service\">)\\R?"
                    + "(<profiles>)\\R?"
                    + "(<bwp-group>)\\R?"
                    + "(<group-index>)[0-9]*(</group-index>)\\R?"
                    + "(<bwp>)\\R?"
                    + "(<cos-index>)[0-9]*(</cos-index>)\\R?"
                    + "(<name>).*(</name>)\\R?"
                    + "(<committed-information-rate>)[0-9]*(</committed-information-rate>)\\R?"
                    + "(<committed-burst-size>)[0-9]*(</committed-burst-size>)\\R?"
                    + "(<excess-information-rate>)[0-9]*(</excess-information-rate>)\\R?"
                    + "(<excess-burst-size>)[0-9]*(</excess-burst-size>)\\R?"
                    + "(</bwp>)\\R?"
                    + "(</bwp-group>)\\R?"
                    + "(<cos>)\\R?"
                    + "(<cos-index>)[0-9](</cos-index>)\\R?"
                    + "(<name>).*(</name>)\\R?"
                    + "(<outgoing-cos-value>)[0-9]*(</outgoing-cos-value>)\\R?"
                    + "(<color-aware>true</color-aware>)\\R?"
                    + "(<color-forward>true</color-forward>)\\R?"
                    + "(<evc-cos-type-all-8-prio-to-1-evc-color>)\\R?"
                    + "(<evc-all-8-color-to>green</evc-all-8-color-to>)\\R?"
                    + "(</evc-cos-type-all-8-prio-to-1-evc-color>)\\R?"
                    + "(</cos>)\\R?"
                    + "(</profiles>)\\R?"
                    + "(</mef-services>)\\R?"
                    + "(</config>)\\R?(</edit-config>)\\R?(</rpc>)\\R?(]]>){2}", Pattern.DOTALL);

    //For testPerformMeterOperationDeviceIdMeterRemove()
    private Pattern sampleXmlRegexEditConfigBwpGroup1Delete =
    Pattern.compile("(<\\?xml).*(<rpc).*(<edit-config>)\\R?"
            + "(<target>\\R?<running/>\\R?</target>)\\R?"
            + "(<config xmlns:nc=\"urn:ietf:params:xml:ns:netconf:base:1.0\">)\\R?"
            + "(<mef-services xmlns=\"http://www.microsemi.com/microsemi-edge-assure/msea-uni-evc-service\">)\\R?"
            + "(<profiles>)\\R?"
            + "((<bwp-group nc:operation=\"delete\">)\\R?"
            + "(<group-index>)[0-9]*(</group-index>)\\R?"
            + "(<bwp>.*</bwp>)?"
            + "(</bwp-group>))++\\R?"
            + "(</profiles>)\\R?"
            + "(</mef-services>)\\R?"
            + "(</config>)\\R?(</edit-config>)\\R?(</rpc>)\\R?(]]>){2}", Pattern.DOTALL);

    private Pattern sampleXmlRegexSetCurrentDatetime =
    Pattern.compile("(<\\?xml).*(<rpc).*"
            + "(<set-current-datetime xmlns=\"urn:ietf:params:xml:ns:yang:ietf-system\">)\\R?"
            + "(<current-datetime>)"
            + "[0-9]{4}-[0-9]{2}-[0-9]{2}(T)[0-9]{2}:[0-9]{2}:[0-9]{2}[+-][0-9]{2}:[0-9]{2}"
            + "(</current-datetime>)\\R?"
            + "(</set-current-datetime>)\\R?"
            + "(</rpc>)\\R?(]]>){2}", Pattern.DOTALL);

    //For testGetConfigMseaCfmEssentials
    private Pattern sampleXmlRegexGetMseaCfmEssentials =
            Pattern.compile("(<\\?xml).*(<rpc).*(<get>)\\R?"
                    + "(<filter type=\"subtree\">)\\R?"
                    + "(<mef-cfm).*"
                    + "(<maintenance-domain>)\\R?"
                    + "(<id/>)\\R?"
                    + "(<name>)([a-zA-Z0-9\\-:\\.]){1,48}(</name>)\\R?"
                    + "(<md-level/>)?\\R?"
                    + "(<maintenance-association>)\\R?"
                    + "(<id/>)\\R?"
                    + "(<name>)([a-zA-Z0-9\\-:\\.]){1,48}(</name>)\\R?"
                    + "(<ccm-interval>)[0-9]{1,3}(ms</ccm-interval>)\\R?"
                    + "(<remote-meps/>)\\R?"
                    + "(<component-list/>)\\R?"
                    + "(<maintenance-association-end-point>)\\R?"
                    + "(<mep-identifier>)[0-9]{1,4}(</mep-identifier>)\\R?"
                    + "(<mac-address/>)?\\R?"
                    + "((<remote-mep-database>)\\R?"
                    + "(<remote-mep>)\\R?"
                    + "(<remote-mep-id/>)\\R?"
                    + "(</remote-mep>)\\R?"
                    + "(</remote-mep-database>))?\\R?"
                    + "((<msea-soam-pm:delay-measurements>)\\R?"
                    + "(<msea-soam-pm:delay-measurement>)\\R?"
                    + "((<msea-soam-pm:dm-id/>)|(<msea-soam-pm:dm-id>[0-9]*</msea-soam-pm:dm-id>))\\R?"
                    + "(</msea-soam-pm:delay-measurement>)\\R?"
                    + "(</msea-soam-pm:delay-measurements>))?\\R?"
                    + "((<msea-soam-pm:loss-measurements>)\\R?"
                    + "(<msea-soam-pm:loss-measurement>)\\R?"
                    + "((<msea-soam-pm:lm-id/>)|(<msea-soam-pm:lm-id>[0-9]*</msea-soam-pm:lm-id>))\\R?"
                    + "(</msea-soam-pm:loss-measurement>)\\R?"
                    + "(</msea-soam-pm:loss-measurements>))?\\R?"
                    + "(</maintenance-association-end-point>)\\R?"
                    + "(</maintenance-association>)\\R?"
                    + "(</maintenance-domain>)\\R?"
                    + "(</mef-cfm>)\\R?"
                    + "(</filter>)\\R?"
                    + "(</get>)\\R?"
                    + "(</rpc>)\\R?"
                    + "(]]>){2}", Pattern.DOTALL);

    //For testGetMep
    private String sampleXmlRegexGetMseaCfmFullStr =
            "(<\\?xml).*(<rpc).*(<get>)\\R?"
            + "(<filter type=\"subtree\">)\\R?"
            + "(<mef-cfm).*"
            + "(<maintenance-domain>)\\R?"
            + "(<id/>)\\R?"
            + "(<name>)([a-zA-Z0-9\\-:\\.]){1,48}(</name>)\\R?"
            + "(<md-level/>)?\\R?"
            + "(<maintenance-association>)\\R?"
            + "(<id/>)\\R?"
            + "(<name>)([a-zA-Z0-9\\-:\\.]){1,48}(</name>)\\R?"
            + "(<maintenance-association-end-point>)\\R?"
            + "(<mep-identifier>)[0-9]{1,4}(</mep-identifier>)\\R?"
            + "(<interface/>)?\\R?"
            + "(<primary-vid/>)?\\R?"
            + "(<administrative-state/>)?\\R?"
            + "(<mac-address/>)?\\R?"
            + "(<ccm-ltm-priority/>)?\\R?"
            + "(<continuity-check/>)?\\R?"
            + "(<loopback/>)?\\R?"
            + "(<linktrace/>)?\\R?"
            + "(<remote-mep-database/>)\\R?"
            + "(<msea-soam-fm:operational-state/>)\\R?"
            + "(<msea-soam-fm:connectivity-status/>)\\R?"
            + "(<msea-soam-fm:port-status/>)\\R?"
            + "(<msea-soam-fm:interface-status/>)\\R?"
            + "(<msea-soam-fm:last-defect-sent/>)\\R?"
            + "(<msea-soam-fm:rdi-transmit-status/>)\\R?"
            + "(</maintenance-association-end-point>)\\R?"
            + "(</maintenance-association>)\\R?"
            + "(</maintenance-domain>)\\R?"
            + "(</mef-cfm>)\\R?"
            + "(</filter>)\\R?"
            + "(</get>)\\R?"
            + "(</rpc>)\\R?"
            + "(]]>){2}";

    private Pattern sampleXmlRegexGetMseaCfmFull =
            Pattern.compile(sampleXmlRegexGetMseaCfmFullStr
                    .replace("(<mep-identifier>)[0-9]{1,4}(</mep-identifier>)",
                            "(<mep-identifier>)" +
                                    EA1000CfmMepProgrammableTest.MEP_111 +
                                    "(</mep-identifier>)"),
                    Pattern.DOTALL);

    private Pattern sampleXmlRegexGetMseaCfmFull2 =
            Pattern.compile(sampleXmlRegexGetMseaCfmFullStr
                    .replace("(<mep-identifier>)[0-9]{1,4}(</mep-identifier>)",
                            "(<mep-identifier>)" +
                                    EA1000CfmMepProgrammableTest.MEP_112 +
                                    "(</mep-identifier>)"),
                    Pattern.DOTALL);

    //For testGetConfigMseaCfmEssentials
    private Pattern sampleXmlRegexGetMseaDelay =
            Pattern.compile("(<\\?xml).*(<rpc).*(<get>)\\R?"
                    + "(<filter type=\"subtree\">)\\R?"
                    + "(<mef-cfm).*"
                    + "(<maintenance-domain>)\\R?"
                    + "(<id/>)\\R?"
                    + "(<name>)([a-zA-Z0-9\\-:\\.]){1,48}(</name>)\\R?"
                    + "(<maintenance-association>)\\R?"
                    + "(<id/>)\\R?"
                    + "(<name>)([a-zA-Z0-9\\-:\\.]){1,48}(</name>)\\R?"
                    + "(<maintenance-association-end-point>)\\R?"
                    + "(<mep-identifier>)[0-9]{1,4}(</mep-identifier>)\\R?"
                    + "((<msea-soam-pm:delay-measurements>)\\R?"
                    + "(<msea-soam-pm:delay-measurement>)\\R?"
                    + "((<msea-soam-pm:dm-id/>)|(<msea-soam-pm:dm-id>[0-9]*</msea-soam-pm:dm-id>))\\R?"
                    + "(<msea-soam-pm:mep-id/>)\\R?"
                    + "(<msea-soam-pm:mac-address/>)\\R?"
                    + "(<msea-soam-pm:administrative-state/>)\\R?"
                    + "(<msea-soam-pm:measurement-enable/>)\\R?"
                    + "(<msea-soam-pm:message-period/>)\\R?"
                    + "(<msea-soam-pm:priority/>)\\R?"
                    + "(<msea-soam-pm:frame-size/>)\\R?"
                    + "(<msea-soam-pm:measurement-interval/>)\\R?"
                    + "(<msea-soam-pm:number-intervals-stored/>)\\R?"
                    + "(<msea-soam-pm:session-status/>)\\R?"
                    + "(<msea-soam-pm:frame-delay-two-way/>)\\R?"
                    + "(<msea-soam-pm:inter-frame-delay-variation-two-way/>)\\R?"
                    + "(<msea-soam-pm:current-stats/>)?\\R?"
                    + "(<msea-soam-pm:history-stats/>)?\\R?"
                    + "(</msea-soam-pm:delay-measurement>)\\R?"
                    + "(</msea-soam-pm:delay-measurements>))?\\R?"
                    + "(</maintenance-association-end-point>)\\R?"
                    + "(</maintenance-association>)\\R?"
                    + "(</maintenance-domain>)\\R?"
                    + "(</mef-cfm>)\\R?"
                    + "(</filter>)\\R?"
                    + "(</get>)\\R?"
                    + "(</rpc>)\\R?"
                    + "(]]>){2}", Pattern.DOTALL);

    //For testGetConfigMseaCfmEssentials
    private Pattern sampleXmlRegexDeleteMseaCfmMep =
            Pattern.compile("(<\\?xml).*(<rpc).*(<edit-config>)\\R?"
                    + "(<target>\\R?<running/>\\R?</target>)\\R?"
                    + "(<config xmlns:nc=\"urn:ietf:params:xml:ns:netconf:base:1.0\">)\\R?"
                    + "(<mef-cfm).*"
                    + "(<maintenance-domain>)\\R?"
                    + "(<id>[0-9]{1,5}</id>)?\\R?"
                    + "((<name>)([a-zA-Z0-9\\-:\\.]){1,48}(</name>))?\\R?"
                    + "(<maintenance-association>)\\R?"
                    + "(<id>[0-9]{1,5}</id>)?\\R?"
                    + "((<name>[a-zA-Z0-9\\-:\\.]{1,48}</name>)|"
                    + "(<name-primary-vid>[0-9]{1,4}</name-primary-vid>))?\\R?"
                    + "(<maintenance-association-end-point nc:operation=\"delete\">)\\R?"
                    + "(<mep-identifier>)[0-9]{1,4}(</mep-identifier>)\\R?"
                    + "(</maintenance-association-end-point>)\\R?"
                    + "(</maintenance-association>)\\R?"
                    + "(</maintenance-domain>)\\R?"
                    + "(</mef-cfm>)\\R?"
                    + "(</config>)\\R?"
                    + "(</edit-config>)\\R?(</rpc>)\\R?(]]>){2}", Pattern.DOTALL);

    //For testGetConfigMseaCfmEssentials
    private Pattern sampleXmlRegexCreateMseaCfmMa =
            Pattern.compile("(<\\?xml).*(<rpc).*(<edit-config>)\\R?"
                    + "(<target>\\R?<running/>\\R?</target>)\\R?"
                    + "(<config xmlns:nc=\"urn:ietf:params:xml:ns:netconf:base:1.0\">)\\R?"
                    + "(<mef-cfm).*"
                    + "(<maintenance-domain>)\\R?"
                    + "(<id>)([0-9]){1,4}(</id>)\\R?"
                    + "((<md-level>)([0-9]){1}(</md-level>))?\\R?"
                    + "((<name>)([a-zA-Z0-9\\-:\\.]){1,48}(</name>))?\\R?"
                    + "((<maintenance-association>)\\R?"
                    + "(<id>)([0-9]){1,4}(</id>)\\R?"
                    + "((<ccm-interval>)(3.3ms)(</ccm-interval>))?\\R?"
                    + "((<remote-meps>)([0-9]){1,4}(</remote-meps>))*\\R?"
                    + "(((<name>)([a-zA-Z0-9\\-:\\.]){1,48}(</name>))|"
                    + "((<name-primary-vid>)([0-9]){1,4}(</name-primary-vid>)))?\\R?"
                    + "((<component-list>)\\R?"
                    + "(<vid>)([0-9]){1,4}(</vid>)\\R?"
                    + "(</component-list>))?\\R?"
                    + "(</maintenance-association>))*\\R?"
                    + "(</maintenance-domain>)\\R?"
                    + "(</mef-cfm>)\\R?"
                    + "(</config>)\\R?"
                    + "(</edit-config>)\\R?(</rpc>)\\R?(]]>){2}", Pattern.DOTALL);

    //For testGetConfigMseaCfmEssentials
    private Pattern sampleXmlRegexDeleteMseaCfmMa =
            Pattern.compile("(<\\?xml).*(<rpc).*(<edit-config>)\\R?"
                    + "(<target>\\R?<running/>\\R?</target>)\\R?"
                    + "(<config xmlns:nc=\"urn:ietf:params:xml:ns:netconf:base:1.0\">)\\R?"
                    + "(<mef-cfm).*"
                    + "(<maintenance-domain>)\\R?"
                    + "((<id/>)|((<id>)([0-9]){1,4}(</id>)))?\\R?"
                    + "((<name>)([a-zA-Z0-9\\-:\\.]){1,48}(</name>))?\\R?"
                    + "(<maintenance-association nc:operation=\"delete\">)\\R?"
                    + "((<id/>)|((<id>)([0-9]){1,4}(</id>)))?\\R?"
                    + "(((<name>)([a-zA-Z0-9\\-:\\.]){1,48}(</name>))|"
                    + "((<name-primary-vid>)([0-9]){1,4}(</name-primary-vid>)))?\\R?"
                    + "(</maintenance-association>)\\R?"
                    + "(</maintenance-domain>)\\R?"
                    + "(</mef-cfm>)\\R?"
                    + "(</config>)\\R?"
                    + "(</edit-config>)\\R?(</rpc>)\\R?(]]>){2}", Pattern.DOTALL);

    //For testDeleteMseaMepRemoteMep
    private Pattern sampleXmlRegexDeleteMseaCfmRmep =
            Pattern.compile("(<\\?xml).*(<rpc).*(<edit-config>)\\R?"
                    + "(<target>\\R?<running/>\\R?</target>)\\R?"
                    + "(<config xmlns:nc=\"urn:ietf:params:xml:ns:netconf:base:1.0\">)\\R?"
                    + "(<mef-cfm).*"
                    + "(<maintenance-domain>)\\R?"
                    + "((<id>)[0-9]{1,4}(</id>))?\\R?"
                    + "((<name>)([a-zA-Z0-9\\-:\\.]){1,48}(</name>))?\\R?"
                    + "(<maintenance-association>)\\R?"
                    + "((<id>)[0-9]{1,4}(</id>))?\\R?"
                    + "((<remote-meps nc:operation=\"delete\">)([0-9]){1,4}(</remote-meps>))*\\R?"
                    + "(((<name>)([a-zA-Z0-9\\-:\\.]){1,48}(</name>))|"
                    + "((<name-primary-vid>)([0-9]){1,4}(</name-primary-vid>)))?\\R?"
                    + "(</maintenance-association>)\\R?"
                    + "(</maintenance-domain>)\\R?"
                    + "(</mef-cfm>)\\R?"
                    + "(</config>)\\R?"
                    + "(</edit-config>)\\R?(</rpc>)\\R?(]]>){2}", Pattern.DOTALL);

    //For testDeleteMseaMd
    private Pattern sampleXmlRegexDeleteMseaCfmMd =
            Pattern.compile("(<\\?xml).*(<rpc).*(<edit-config>)\\R?"
                    + "(<target>\\R?<running/>\\R?</target>)\\R?"
                    + "(<config xmlns:nc=\"urn:ietf:params:xml:ns:netconf:base:1.0\">)\\R?"
                    + "(<mef-cfm).*"
                    + "(<maintenance-domain nc:operation=\"delete\">)\\R?"
                    + "((<id/>)|(<id>([0-9]){1,4}(</id>)))?\\R?"
                    + "(((<name>)([a-zA-Z0-9\\-:\\.]){1,48}(</name>))|\\R?"
                    + "((<name-domain-name>)([a-zA-Z0-9\\-\\.]){1,48}(</name-domain-name>)))?\\R?"
                    + "(</maintenance-domain>)\\R?"
                    + "(</mef-cfm>)\\R?"
                    + "(</config>)\\R?"
                    + "(</edit-config>)\\R?(</rpc>)\\R?(]]>){2}", Pattern.DOTALL);


    private Pattern sampleXmlRegexTransmitLoopback =
            Pattern.compile("(<\\?xml).*(<rpc).*\\R?"
                    + "(<transmit-loopback xmlns=\"http://www.microsemi.com/microsemi-edge-assure/msea-cfm\">)\\R?"
                    + "((<number-of-messages>)[0-9]*(</number-of-messages>))?\\R?"
                    + "((<data-tlv>)[a-zA-Z0-9+=/]*(</data-tlv>))?\\R?"
                    + "((<vlan-priority>)[0-9]{1}(</vlan-priority>))?\\R?"
                    + "((<vlan-drop-eligible>)((true)|(false))(</vlan-drop-eligible>))?\\R?"
                    + "(<maintenance-association-end-point>)[0-9]{1,4}(</maintenance-association-end-point>)\\R?"
                    + "(<maintenance-association>)[0-9]{1,5}(</maintenance-association>)\\R?"
                    + "(<maintenance-domain>)[0-9]{1,5}(</maintenance-domain>)\\R?"
                    + "(<target-address>)\\R?"
                    + "((<mep-id>)[0-9]{1,4}(</mep-id>))?\\R?"
                    + "((<mac-address>)[a-fA-F0-9:-]{17}(</mac-address>))?\\R?"
                    + "(</target-address>)\\R?"
                    + "(</transmit-loopback>)\\R?"
                    + "(</rpc>)\\R?"
                    + "(]]>){2}", Pattern.DOTALL);

    private Pattern sampleXmlRegexAbortLoopback =
            Pattern.compile("(<\\?xml).*(<rpc).*\\R?"
                    + "(<abort-loopback xmlns=\"http://www.microsemi.com/microsemi-edge-assure/msea-cfm\">)\\R?"
                    + "(<maintenance-association-end-point>)[0-9]{1,4}(</maintenance-association-end-point>)\\R?"
                    + "(<maintenance-association>)[0-9]{1,5}(</maintenance-association>)\\R?"
                    + "(<maintenance-domain>)[0-9]{1,5}(</maintenance-domain>)\\R?"
                    + "(</abort-loopback>)\\R?"
                    + "(</rpc>)\\R?"
                    + "(]]>){2}", Pattern.DOTALL);

    //For testCreateDm()
    private Pattern sampleXmlRegexEditConfigDmCreate =
        Pattern.compile("(<\\?xml).*(<rpc).*(<edit-config>)\\R?"
            + "(<target>\\R?<running/>\\R?</target>)\\R?"
            + "(<config xmlns:nc=\"urn:ietf:params:xml:ns:netconf:base:1.0\">)\\R?"
            + "(<mef-cfm xmlns=\"http://www.microsemi.com/microsemi-edge-assure/msea-cfm\">)\\R?"
            + "(<maintenance-domain>)\\R?"
            + "(<id>)[0-9]*(</id>)\\R?"
            + "(<maintenance-association>)\\R?"
            + "(<id>)[0-9]*(</id>)\\R?"
            + "(<maintenance-association-end-point>)\\R?"
            + "(<mep-identifier>)[0-9]*(</mep-identifier>)\\R?"
            + "(<delay-measurements xmlns=\"http://www.microsemi.com/microsemi-edge-assure/msea-soam-pm\">)\\R?"
            + "(<delay-measurement>)\\R?"
            + "(<dm-id>)[0-9]*(</dm-id>)\\R?"
            + "((<administrative-state>)(true|false)(</administrative-state>))?\\R?"
            + "((<message-period>)(1000ms|100ms|10ms|3ms)(</message-period>))?\\R?"
            + "(<priority>)[0-9]*(</priority>)\\R?"
            + "((<frame-size>)[0-9]*(</frame-size>))?\\R?"
            + "(<mep-id>)[0-9]*(</mep-id>)\\R?"
            + "(</delay-measurement>)\\R?"
            + "(</delay-measurements>)\\R?"
            + "(</maintenance-association-end-point>)\\R?"
            + "(</maintenance-association>)\\R?"
            + "(</maintenance-domain>)\\R?"
            + "(</mef-cfm>)\\R?"
            + "(</config>)\\R?"
            + "(</edit-config>)\\R?(</rpc>)\\R?(]]>){2}", Pattern.DOTALL);


    private static final String SAMPLE_SYSTEM_REPLY = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<rpc-reply xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\" message-id=\"1\">\n"
            + "<data xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">\n"
            + "<system xmlns=\"urn:ietf:params:xml:ns:yang:ietf-system\">\n"
            + "<clock><timezone-name>Etc/UTC</timezone-name></clock>\n"
            + "</system>\n"
            + "</data>\n"
            + "</rpc-reply>";

    private static final String SAMPLE_SYSTEM_REPLY_INIT = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<rpc-reply xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\" message-id=\"1\">"
            + "<data xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">\n"
            + "<system xmlns=\"urn:ietf:params:xml:ns:yang:ietf-system\">\n"
            + "<longitude xmlns=\"http://www.microsemi.com/microsemi-edge-assure/msea-system\">-8.4683990</longitude>\n"
            + "<latitude xmlns=\"http://www.microsemi.com/microsemi-edge-assure/msea-system\">51.9036140</latitude>\n"
            + "</system>\n"
            + "<system-state xmlns=\"urn:ietf:params:xml:ns:yang:ietf-system\""
            + " xmlns:sysms=\"http://www.microsemi.com/microsemi-edge-assure/msea-system\">\n"
            + "<platform>\n"
            + "<os-release>4.4.0-53-generic</os-release>\n"
            + "<sysms:device-identification>\n"
            + "<sysms:serial-number>EA1000 unit test.</sysms:serial-number>\n"
            + "</sysms:device-identification>\n"
            + "</platform>\n"
            + "</system-state>\n"
            + "</data>"
            + "</rpc-reply>";

    private static final String SAMPLE_MSEASAFILTERING_FE_REPLY = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<rpc-reply xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\" message-id=\"1\">\n"
            + "<data xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">\n"
            + "<source-ipaddress-filtering "
            + "xmlns=\"http://www.microsemi.com/microsemi-edge-assure/msea-sa-filtering\">\n"
            + "<interface-eth0>\n"
            + "<filter-admin-state>inactive</filter-admin-state>\n"
            + "<source-address-range>\n"
            + "<range-id>1</range-id>\n"
            + "<ipv4-address-prefix>10.10.10.10/16</ipv4-address-prefix>\n"
            + "<name>Filter1</name>\n"
            + "</source-address-range>\n"
            + "<source-address-range>\n"
            + "<range-id>2</range-id>\n"
            + "<ipv4-address-prefix>20.30.40.50/18</ipv4-address-prefix>\n"
            + "<name>Flow:5e0000abaa2772</name>\n"
            + "</source-address-range>\n"
            + "</interface-eth0>\n"
            + "</source-ipaddress-filtering>\n"
            + "</data>\n"
            + "</rpc-reply>";

    private static final String SAMPLE_ERROR_REPLY = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<rpc-reply xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\" message-id=\"1\">\n"
            + "<rpc-error>"
            + "<error-type>application</error-type>"
            + "<error-tag>data-missing</error-tag>"
            + "<error-severity>error</error-severity>"
            + "<error-message>Request could not be completed because " +
                "the relevant data model content does not exist.</error-message>"
            + "</rpc-error>"
            + "</rpc-reply>";

    private static final String SAMPLE_MSEAEVCUNI_REPLY_INIT = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<rpc-reply xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\" message-id=\"1\">\n"
            + "<data xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">\n"
            + "<mef-services xmlns=\"http://www.microsemi.com/microsemi-edge-assure/msea-uni-evc-service\"/>"
            + "</data>\n"
            + "</rpc-reply>";

    private static final String SAMPLE_MSEAEVCUNI_FE_REPLY = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<rpc-reply xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\" message-id=\"1\">\n"
            + "<data xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">\n"
            + "<mef-services xmlns=\"http://www.microsemi.com/microsemi-edge-assure/msea-uni-evc-service\">\n"
            + "<uni>\n"
            + "<name>Flow:7557655abfecd57865</name>\n"
            + "<evc>\n"
            + "<evc-index>7</evc-index\n>"
            + "<name>evc-7</name>\n"
            + "<evc-per-uni>\n"
            + "<evc-per-uni-c>\n"
            + "<ce-vlan-map>10</ce-vlan-map>\n"
            + "<ingress-bwp-group-index>1</ingress-bwp-group-index>\n"
            + "<tag-push>\n"
            + "<push-tag-type>pushStag</push-tag-type>\n"
            + "<outer-tag-vlan>3</outer-tag-vlan>\n"
            + "</tag-push>\n"
            + "<flow-mapping>\n"
            + "<ce-vlan-id>10</ce-vlan-id>\n"
            + "<flow-id>27021600672053710</flow-id>\n"
            + "</flow-mapping>\n"
            + "<evc-per-uni-service-type>epl</evc-per-uni-service-type>\n"
            + "</evc-per-uni-c>\n"
            + "<evc-per-uni-n>\n"
            + "<ce-vlan-map>11</ce-vlan-map>\n"
            + "<ingress-bwp-group-index>0</ingress-bwp-group-index>\n"
            + "<flow-mapping>\n"
            + "<ce-vlan-id>11</ce-vlan-id>\n"
            + "<flow-id>27021600672053711</flow-id>\n"
            + "</flow-mapping>\n"
            + "</evc-per-uni-n>\n"
            + "</evc-per-uni>\n"
            + "<uni-evc-id>EA1000-Uni-from-ONOS_5</uni-evc-id>\n"
            + "<evc-status>\n"
            + "<operational-state>unknown</operational-state>\n"
            + "<max-mtu-size>9600</max-mtu-size>\n"
            + "<max-num-uni>2</max-num-uni>\n"
            + "</evc-status>\n"
            + "</evc>\n"
            + "<evc>\n"
            + "<evc-index>8</evc-index>\n"
            + "<name>evc-8</name>\n"
            + "<evc-per-uni>\n"
            + "<evc-per-uni-c>\n"
            + "<ce-vlan-map>12:14,20:22,25</ce-vlan-map>\n"
            + "<ingress-bwp-group-index>0</ingress-bwp-group-index>\n"
            + "<tag-pop />\n"
            + "<evc-per-uni-service-type>epl</evc-per-uni-service-type>\n"
            + "<flow-mapping>\n"
            + "<ce-vlan-id>12</ce-vlan-id>\n"
            + "<flow-id>27021600672053712</flow-id>\n"
            + "</flow-mapping>\n"
            + "<flow-mapping>\n"
            + "<ce-vlan-id>13</ce-vlan-id>\n"
            + "<flow-id>27021600672053713</flow-id>\n"
            + "</flow-mapping>\n"
            + "<flow-mapping>\n"
            + "<ce-vlan-id>14</ce-vlan-id>\n"
            + "<flow-id>27021600672053714</flow-id>\n"
            + "</flow-mapping>\n"
            + "<flow-mapping>\n"
            + "<ce-vlan-id>20</ce-vlan-id>\n"
            + "<flow-id>27021600672053720</flow-id>\n"
            + "</flow-mapping>\n"
            + "<flow-mapping>\n"
            + "<ce-vlan-id>21</ce-vlan-id>\n"
            + "<flow-id>27021600672053721</flow-id>\n"
            + "</flow-mapping>\n"
            + "<flow-mapping>\n"
            + "<ce-vlan-id>22</ce-vlan-id>\n"
            + "<flow-id>27021600672053722</flow-id>\n"
            + "</flow-mapping>\n"
            + "<flow-mapping>\n"
            + "<ce-vlan-id>25</ce-vlan-id>\n"
            + "<flow-id>27021600672053725</flow-id>\n"
            + "</flow-mapping>\n"
            + "</evc-per-uni-c>\n"
            + "<evc-per-uni-n>\n"
            + "<ce-vlan-map>13</ce-vlan-map>\n"
            + "<ingress-bwp-group-index>0</ingress-bwp-group-index>\n"
            + "<flow-mapping>\n"
            + "<ce-vlan-id>13</ce-vlan-id>\n"
            + "<flow-id>27021600672053713</flow-id>\n"
            + "</flow-mapping>\n"
            + "</evc-per-uni-n>\n"
            + "</evc-per-uni>\n"
            + "<uni-evc-id>EA1000-Uni-from-ONOS_5</uni-evc-id>\n"
            + "<evc-status>\n"
            + "<operational-state>unknown</operational-state>\n"
            + "<max-mtu-size>9600</max-mtu-size>\n"
            + "<max-num-uni>2</max-num-uni>\n"
            + "</evc-status>\n"
            + "</evc>\n"
            + "</uni>\n"
            + "<profiles>\n"
            + "<bwp-group>\n"
            + "<group-index>0</group-index>\n"
            + "</bwp-group>\n"
            + "</profiles>\n"
            + "</mef-services>\n"
            + "</data>\n"
            + "</rpc-reply>";

    private static final String SAMPLE_MSEAEVCUNI_CEVLANMAP_EVC_REPLY = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<rpc-reply xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\" message-id=\"1\">\n"
            + "<data xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">\n"
            + "<mef-services xmlns=\"http://www.microsemi.com/microsemi-edge-assure/msea-uni-evc-service\">\n"
            + "<uni>\n"

            + "<evc><evc-index>1</evc-index>\n"
            + "<evc-per-uni>\n"
            + "<evc-per-uni-c>\n"
            + "<ce-vlan-map>101</ce-vlan-map>\n"
            + "<flow-mapping>\n"
            + "<ce-vlan-id>101</ce-vlan-id>\n"
            + "<flow-id>27021598629213101</flow-id>\n"
            + "</flow-mapping>\n"
            + "</evc-per-uni-c>\n"
            + "<evc-per-uni-n>\n"
            + "<ce-vlan-map>102</ce-vlan-map>\n"
            + "<flow-mapping>\n"
            + "<ce-vlan-id>102</ce-vlan-id>\n"
            + "<flow-id>27021598629213102</flow-id>\n"
            + "</flow-mapping>\n"
            + "</evc-per-uni-n>\n"
            + "</evc-per-uni>\n"
            + "</evc>\n"

            + "<evc><evc-index>7</evc-index>\n"
            + "<evc-per-uni>\n"
            + "<evc-per-uni-c>\n"
            + "<ce-vlan-map>700,710,720</ce-vlan-map>\n"
            + "<flow-mapping>\n"
            + "<ce-vlan-id>700</ce-vlan-id>\n"
            + "<flow-id>27021598629213700</flow-id>\n"
            + "</flow-mapping>\n"
            + "<flow-mapping>\n"
            + "<ce-vlan-id>710</ce-vlan-id>\n"
            + "<flow-id>27021598629213710</flow-id>\n"
            + "</flow-mapping>\n"
            + "<flow-mapping>\n"
            + "<ce-vlan-id>720</ce-vlan-id>\n"
            + "<flow-id>27021598629213720</flow-id>\n"
            + "</flow-mapping>\n"
            + "</evc-per-uni-c>\n"
            + "<evc-per-uni-n>\n"
            + "<ce-vlan-map>701:703</ce-vlan-map>\n"
            + "<flow-mapping>\n"
            + "<ce-vlan-id>701</ce-vlan-id>\n"
            + "<flow-id>27021598629213701</flow-id>\n"
            + "</flow-mapping>\n"
            + "<flow-mapping>\n"
            + "<ce-vlan-id>702</ce-vlan-id>\n"
            + "<flow-id>27021598629213702</flow-id>\n"
            + "</flow-mapping>\n"
            + "<flow-mapping>\n"
            + "<ce-vlan-id>703</ce-vlan-id>\n"
            + "<flow-id>27021598629213703</flow-id>\n"
            + "</flow-mapping>\n"
            + "</evc-per-uni-n>\n"
            + "</evc-per-uni>\n"
            + "</evc>\n"

            + "<evc><evc-index>8</evc-index>\n"
            + "<evc-per-uni>\n"
            + "<evc-per-uni-c>\n<ce-vlan-map>800,810,820</ce-vlan-map>\n</evc-per-uni-c>\n"
            + "<evc-per-uni-n>\n<ce-vlan-map>801:803</ce-vlan-map>\n</evc-per-uni-n>\n"
            + "</evc-per-uni>\n"
            + "</evc>\n"

            + "</uni>\n"
            + "</mef-services>\n"
            + "</data>\n"
            + "</rpc-reply>";

    private static final String SAMPLE_MSEACFM_MD_MA_MEP_ESSENTIALS_REPLY =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<rpc-reply xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\" message-id=\"47\">"
            + "<data>"
            + "<mef-cfm xmlns=\"http://www.microsemi.com/microsemi-edge-assure/msea-cfm\" "
            + "xmlns:msea-soam-fm=\"http://www.microsemi.com/microsemi-edge-assure/msea-soam-fm\" "
            + "xmlns:msea-soam-pm=\"http://www.microsemi.com/microsemi-edge-assure/msea-soam-pm\">"
            + "<maintenance-domain>"
            + "<id>1</id>"
            + "<name>md-1</name>"
            + "<maintenance-association>"
            + "<id>1</id>"
            + "<name>ma-1-1</name>"
            + "<maintenance-association-end-point>"
            + "<mep-identifier>1</mep-identifier>"
            + "<mac-address>00:b0:ae:03:ff:31</mac-address>"
            + "<remote-mep-database>"
            + "<remote-mep>"
            + "<remote-mep-id>1</remote-mep-id>"
            + "</remote-mep>"
            + "<remote-mep>"
            + "<remote-mep-id>2</remote-mep-id>"
            + "</remote-mep>"
            + "</remote-mep-database>"
            + "<msea-soam-pm:delay-measurements>"
            + "<msea-soam-pm:delay-measurement>"
            + "<msea-soam-pm:dm-id>1</msea-soam-pm:dm-id>"
            + "</msea-soam-pm:delay-measurement>"
            + "</msea-soam-pm:delay-measurements>"
            + "<msea-soam-pm:loss-measurements>"
            + "<msea-soam-pm:loss-measurement>"
            + "<msea-soam-pm:lm-id>1</msea-soam-pm:lm-id>"
            + "</msea-soam-pm:loss-measurement>"
            + "<msea-soam-pm:loss-measurement>"
            + "<msea-soam-pm:lm-id>2</msea-soam-pm:lm-id>"
            + "</msea-soam-pm:loss-measurement>"
            + "</msea-soam-pm:loss-measurements>"
            + "</maintenance-association-end-point>"
            + "</maintenance-association>"
            + "</maintenance-domain>"
            + "</mef-cfm>"
            + "</data>"
            + "</rpc-reply>";

    private static final String SAMPLE_MSEACFM_MD_MA_MEP_FULL_REPLY =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<rpc-reply xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\" message-id=\"47\">"
            + "<data>"
            + "<mef-cfm xmlns=\"http://www.microsemi.com/microsemi-edge-assure/msea-cfm\" "
            + "xmlns:msea-soam-fm=\"http://www.microsemi.com/microsemi-edge-assure/msea-soam-fm\" "
            + "xmlns:msea-soam-pm=\"http://www.microsemi.com/microsemi-edge-assure/msea-soam-pm\">"
            + "<maintenance-domain>"
            + "<id>1</id>"
            + "<name>" + EA1000CfmMepProgrammableTest.MD_ID_1 + "</name>"
            + "<maintenance-association>"
            + "<id>1</id>"
            + "<name>" + EA1000CfmMepProgrammableTest.MA_ID_11 + "</name>"
            + "<ccm-interval>3.3ms</ccm-interval>"
            + "<maintenance-association-end-point>"
            + "<mep-identifier>" + EA1000CfmMepProgrammableTest.MEP_111 + "</mep-identifier>"
            + "<interface>eth0</interface>"
            + "<primary-vid>20</primary-vid>"
            + "<administrative-state>true</administrative-state>"
            + "<mac-address>00:b0:ae:03:ff:31</mac-address>"
            + "<ccm-ltm-priority>5</ccm-ltm-priority>"
            + "<continuity-check>"
                + "<cci-enabled>true</cci-enabled>"
                + "<fng-state>defect-reported</fng-state>"
                + "<highest-priority-defect-found>remote-invalid-ccm</highest-priority-defect-found>"
                + "<active-defects>remote-rdi remote-invalid-ccm</active-defects>"
                + "<ccm-sequence-error-count>0</ccm-sequence-error-count>"
                + "<sent-ccms>197</sent-ccms>"
            + "</continuity-check>"
            + "<loopback>"
            + "</loopback>"
            + "<linktrace>"
            + "</linktrace>"
            + "<remote-mep-database>"
                + "<remote-mep>"
                    + "<remote-mep-id>1</remote-mep-id>"
                    + "<remote-mep-state>failed</remote-mep-state>"
                    + "<failed-ok-time>54654654</failed-ok-time>"
                    + "<mac-address>aa:bb:cc:dd:ee:ff</mac-address>"
                    + "<rdi>false</rdi>"
                    + "<port-status-tlv>no-status-tlv</port-status-tlv>"
                    + "<interface-status-tlv>dormant</interface-status-tlv>"
                + "</remote-mep>"
                + "<remote-mep>"
                    + "<remote-mep-id>2</remote-mep-id>"
                    + "<remote-mep-state>failed</remote-mep-state>"
                    + "<failed-ok-time>54654654</failed-ok-time>"
                    + "<mac-address>aa:bb:cc:dd:ee:ff</mac-address>"
                    + "<rdi>false</rdi>"
                    + "<port-status-tlv>no-status-tlv</port-status-tlv>"
                    + "<interface-status-tlv>dormant</interface-status-tlv>"
                + "</remote-mep>"
            + "</remote-mep-database>"
            + "<msea-soam-fm:operational-state>enabled</msea-soam-fm:operational-state>"
            + "<msea-soam-fm:connectivity-status>partially-active</msea-soam-fm:connectivity-status>"
            + "<msea-soam-fm:port-status>up</msea-soam-fm:port-status>"
            + "<msea-soam-fm:interface-status>up</msea-soam-fm:interface-status>"
            + "<msea-soam-fm:last-defect-sent>remote-rdi remote-mac-error</msea-soam-fm:last-defect-sent>"
            + "<msea-soam-fm:rdi-transmit-status>true</msea-soam-fm:rdi-transmit-status>"
            + "</maintenance-association-end-point>"
            + "</maintenance-association>"
            + "</maintenance-domain>"
            + "</mef-cfm>"
            + "</data>"
            + "</rpc-reply>";


    /**
     * With an empty <last-defect-sent />. Retrieved from simulator.
     */
    private static final String SAMPLE_MSEACFM_MD_MA_MEP_FULL_REPLY2 =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<rpc-reply xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\" message-id=\"47\">"
            + "<data>"
            + "<mef-cfm xmlns=\"http://www.microsemi.com/microsemi-edge-assure/msea-cfm\" "
            + "xmlns:msea-soam-fm=\"http://www.microsemi.com/microsemi-edge-assure/msea-soam-fm\" "
            + "xmlns:msea-soam-pm=\"http://www.microsemi.com/microsemi-edge-assure/msea-soam-pm\">"
            + "<maintenance-domain>"
                + "<id>1</id>"
                + "<name>" + EA1000CfmMepProgrammableTest.MD_ID_1 + "</name>"
                + "<maintenance-association>"
                    + "<id>1</id>"
                    + "<name>" + EA1000CfmMepProgrammableTest.MA_ID_11 + "</name>"
                    + "<maintenance-association-end-point>"
                        + "<mep-identifier>" + EA1000CfmMepProgrammableTest.MEP_112 + "</mep-identifier>"
                        + "<interface>eth0</interface>"
                        + "<administrative-state>true</administrative-state>"
                        + "<ccm-ltm-priority>4</ccm-ltm-priority>"
                        + "<continuity-check>"
                            + "<cci-enabled>true</cci-enabled>"
                            + "<fng-state>report-defect</fng-state>"
                            + "<highest-priority-defect-found>remote-mac-error</highest-priority-defect-found>"
                            + "<active-defects> remote-mac-error invalid-ccm</active-defects>"
                            + "<last-error-ccm>U2FtcGxlIGxhc3QgZXJyb3IgY2NtAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
                            + "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=="
                            + "</last-error-ccm>"
                            + "<ccm-sequence-error-count>10</ccm-sequence-error-count>"
                            + "<sent-ccms>15</sent-ccms>"
                        + "</continuity-check>"
                        + "<mac-address>53:65:61:6e:20:43</mac-address>"
                        + "<msea-soam-fm:port-status>no-status-tlv</msea-soam-fm:port-status>"
                        + "<msea-soam-fm:interface-status>no-status-tlv</msea-soam-fm:interface-status>"
                        + "<msea-soam-fm:last-defect-sent />"
                        + "<msea-soam-fm:rdi-transmit-status>false</msea-soam-fm:rdi-transmit-status>"
                        + "<loopback>"
                            + "<replies-received>123</replies-received>"
                            + "<replies-transmitted>456</replies-transmitted>"
                        + "</loopback>"
                        + "<remote-mep-database>"
                            + "<remote-mep>"
                                + "<remote-mep-id>20</remote-mep-id>"
                                + "<remote-mep-state>ok</remote-mep-state>"
                                + "<failed-ok-time>150859498</failed-ok-time>"
                                + "<mac-address>53:65:61:6e:20:43</mac-address>"
                                + "<rdi>true</rdi>"
                                + "<port-status-tlv>up</port-status-tlv>"
                                + "<interface-status-tlv>no-status-tlv</interface-status-tlv>"
                            + "</remote-mep>"
                            + "<remote-mep>"
                                + "<remote-mep-id>30</remote-mep-id>"
                                + "<remote-mep-state>ok</remote-mep-state>"
                                + "<failed-ok-time>150859498</failed-ok-time>"
                                + "<mac-address>53:65:61:6e:20:43</mac-address>"
                                + "<rdi>true</rdi>"
                                + "<port-status-tlv>no-status-tlv</port-status-tlv>"
                                + "<interface-status-tlv>down</interface-status-tlv>"
                            + "</remote-mep>"
                        + "</remote-mep-database>"
                        + "<linktrace>"
                            + "<unexpected-replies-received>0</unexpected-replies-received>"
                            + "<msea-soam-fm:ltm-msgs-transmitted>2</msea-soam-fm:ltm-msgs-transmitted>"
                            + "<msea-soam-fm:ltm-msgs-received>2</msea-soam-fm:ltm-msgs-received>"
                            + "<msea-soam-fm:ltr-msgs-transmitted>2</msea-soam-fm:ltr-msgs-transmitted>"
                            + "<msea-soam-fm:ltr-msgs-received>2</msea-soam-fm:ltr-msgs-received>"
                            + "<linktrace-database />"
                        + "</linktrace>"
                    + "</maintenance-association-end-point>"
                + "</maintenance-association>"
            + "</maintenance-domain>"
            + "</mef-cfm>"
            + "</data>"
            + "</rpc-reply>";

    private static final String SAMPLE_MSEACFM_DELAY_MEASUREMENT_FULL_REPLY =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<rpc-reply xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\" message-id=\"47\">"
            + "<data>"
            + "<mef-cfm xmlns=\"http://www.microsemi.com/microsemi-edge-assure/msea-cfm\" "
            + "xmlns:msea-soam-fm=\"http://www.microsemi.com/microsemi-edge-assure/msea-soam-fm\" "
            + "xmlns:msea-soam-pm=\"http://www.microsemi.com/microsemi-edge-assure/msea-soam-pm\">"
            + "<maintenance-domain>"
            + "<id>1</id>"
            + "<name>md-1</name>"
            + "<maintenance-association>"
            + "<id>1</id>"
            + "<name>ma-1-1</name>"
            + "<maintenance-association-end-point>"
            + "<mep-identifier>1</mep-identifier>"
            + "<msea-soam-pm:delay-measurements>"
            + "<msea-soam-pm:delay-measurement>"
            + "<msea-soam-pm:dm-id>1</msea-soam-pm:dm-id>"
            + "<msea-soam-pm:mep-id>10</msea-soam-pm:mep-id>"
            + "<msea-soam-pm:administrative-state>true</msea-soam-pm:administrative-state>"
            + "<msea-soam-pm:measurement-enable>frame-delay-two-way-bins "
            + "frame-delay-two-way-max</msea-soam-pm:measurement-enable>"
            + "<msea-soam-pm:message-period>3ms</msea-soam-pm:message-period>"
            + "<msea-soam-pm:priority>6</msea-soam-pm:priority>"
            + "<msea-soam-pm:frame-size>1000</msea-soam-pm:frame-size>"
            + "<msea-soam-pm:measurement-interval>15</msea-soam-pm:measurement-interval>"
            + "<msea-soam-pm:number-intervals-stored>32</msea-soam-pm:number-intervals-stored>"
            + "<msea-soam-pm:session-status>active</msea-soam-pm:session-status>"
            + "<msea-soam-pm:frame-delay-two-way>100</msea-soam-pm:frame-delay-two-way>"
            + "<msea-soam-pm:inter-frame-delay-variation-two-way>101</msea-soam-pm:inter-frame-delay-variation-two-way>"
//            + "<msea-soam-pm:current-stats>"
//            + "</msea-soam-pm:current-stats>"
//            + "<msea-soam-pm:history-stats>"
//            + "</msea-soam-pm:history-stats>"
            + "</msea-soam-pm:delay-measurement>"
            + "</msea-soam-pm:delay-measurements>"
            + "</maintenance-association-end-point>"
            + "</maintenance-association>"
            + "</maintenance-domain>"
            + "</mef-cfm>"
            + "</data>"
            + "</rpc-reply>";

    private static final String SAMPLE_REPLY_OK = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<rpc-reply xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\" message-id=\"2\">"
            + "<ok/>"
            + "</rpc-reply>";

    private NetconfDeviceInfo deviceInfo;

    private final AtomicInteger messageIdInteger = new AtomicInteger(0);

    public MockNetconfSessionEa1000(NetconfDeviceInfo deviceInfo) throws NetconfException {
        this.deviceInfo = deviceInfo;
    }

    @Override
    public CompletableFuture<String> request(String request) throws NetconfException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String get(String request) throws NetconfException {

        return sendRequest(request);
    }

    @Override
    public String get(String filterSchema, String withDefaultsMode) throws NetconfException {
        StringBuilder rpc = new StringBuilder(XML_HEADER);
        rpc.append(RPC_OPEN);
        rpc.append(MESSAGE_ID_STRING);
        rpc.append(EQUAL);
        rpc.append("\"");
        rpc.append(messageIdInteger.get());
        rpc.append("\"  ");
        rpc.append(NETCONF_BASE_NAMESPACE).append(">\n");
        rpc.append(GET_OPEN).append(NEW_LINE);
        if (filterSchema != null) {
            rpc.append(SUBTREE_FILTER_OPEN).append(NEW_LINE);
            rpc.append(filterSchema).append(NEW_LINE);
            rpc.append(SUBTREE_FILTER_CLOSE).append(NEW_LINE);
        }
        if (withDefaultsMode != null) {
            rpc.append(WITH_DEFAULT_OPEN).append(NETCONF_WITH_DEFAULTS_NAMESPACE).append(">");
            rpc.append(withDefaultsMode).append(WITH_DEFAULT_CLOSE).append(NEW_LINE);
        }
        rpc.append(GET_CLOSE).append(NEW_LINE);
        rpc.append(RPC_CLOSE).append(NEW_LINE);
        rpc.append(ENDPATTERN);
        String reply = sendRequest(rpc.toString());
        checkReply(reply);
        return reply;
    }

    @Override
    public String doWrappedRpc(String request) throws NetconfException {
        StringBuilder rpc = new StringBuilder(XML_HEADER);
        rpc.append(RPC_OPEN);
        rpc.append(MESSAGE_ID_STRING);
        rpc.append(EQUAL);
        rpc.append("\"");
        rpc.append(messageIdInteger.get());
        rpc.append("\"  ");
        rpc.append(NETCONF_BASE_NAMESPACE).append(">\n");
        rpc.append(request);
        rpc.append(RPC_CLOSE).append(NEW_LINE);
        rpc.append(ENDPATTERN);
        String reply = sendRequest(rpc.toString());
        checkReply(reply);
        return reply;
    }

    @Override
    public String requestSync(String request) throws NetconfException {
        if (!request.contains(ENDPATTERN)) {
            request = request + NEW_LINE + ENDPATTERN;
        }
        String reply = sendRequest(request);
        checkReply(reply);
        return reply;
    }

    @Override
    public String getConfig(DatastoreId targetConfiguration, String configurationSchema) throws NetconfException {
        StringBuilder rpc = new StringBuilder(XML_HEADER);
        rpc.append("<rpc ");
        rpc.append(MESSAGE_ID_STRING);
        rpc.append(EQUAL);
        rpc.append("\"");
        rpc.append(messageIdInteger.get());
        rpc.append("\"  ");
        rpc.append("xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">\n");
        rpc.append("<get-config>\n");
        rpc.append("<source>\n");
        rpc.append("<").append(targetConfiguration).append("/>");
        rpc.append("</source>");
        if (configurationSchema != null) {
            rpc.append("<filter type=\"subtree\">\n");
            rpc.append(configurationSchema).append("\n");
            rpc.append("</filter>\n");
        }
        rpc.append("</get-config>\n");
        rpc.append("</rpc>\n");
        rpc.append(ENDPATTERN);
        String reply = sendRequest(rpc.toString());
        return checkReply(reply) ? reply : "ERROR " + reply;
    }

    @Override
    public boolean editConfig(String newConfiguration) throws NetconfException {
        return editConfig(DatastoreId.RUNNING, null, newConfiguration);
    }

    @Override
    public boolean editConfig(DatastoreId targetConfiguration, String mode, String newConfiguration)
            throws NetconfException {
        newConfiguration = newConfiguration.trim();
        StringBuilder rpc = new StringBuilder(XML_HEADER);
        rpc.append(RPC_OPEN);
        rpc.append(MESSAGE_ID_STRING);
        rpc.append(EQUAL);
        rpc.append("\"");
        rpc.append(messageIdInteger.get());
        rpc.append("\"  ");
        rpc.append(NETCONF_BASE_NAMESPACE).append(">\n");
        rpc.append(EDIT_CONFIG_OPEN).append("\n");
        rpc.append(TARGET_OPEN);
        rpc.append("<").append(targetConfiguration).append("/>");
        rpc.append(TARGET_CLOSE).append("\n");
        if (mode != null) {
            rpc.append(DEFAULT_OPERATION_OPEN);
            rpc.append(mode);
            rpc.append(DEFAULT_OPERATION_CLOSE).append("\n");
        }
        rpc.append(CONFIG_OPEN).append("\n");
        rpc.append(newConfiguration);
        rpc.append(CONFIG_CLOSE).append("\n");
        rpc.append(EDIT_CONFIG_CLOSE).append("\n");
        rpc.append(RPC_CLOSE);
        rpc.append(ENDPATTERN);
        log.debug(rpc.toString());
        String reply = sendRequest(rpc.toString());
        return checkReply(reply);
    }

    @Override
    public boolean copyConfig(String targetConfiguration, String newConfiguration) throws NetconfException {
        return copyConfig(DatastoreId.datastore(targetConfiguration), newConfiguration);
    }

    @Override
    public void startSubscription() throws NetconfException {
        // TODO Auto-generated method stub

    }

    @Override
    public void startSubscription(String filterSchema) throws NetconfException {
        // TODO Auto-generated method stub

    }

    @Override
    public void endSubscription() throws NetconfException {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean lock() throws NetconfException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean unlock() throws NetconfException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean close() throws NetconfException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public String getSessionId() {
        return "mockSessionId";
    }

    @Override
    public void addDeviceOutputListener(NetconfDeviceOutputEventListener listener) {
        // TODO Auto-generated method stub

    }

    @Override
    public void removeDeviceOutputListener(NetconfDeviceOutputEventListener listener) {
        // TODO Auto-generated method stub

    }

    @Override
    protected boolean checkReply(String reply) {
        if (reply != null) {
            if (!reply.contains("<rpc-error>")) {
                log.debug("Device {} sent reply {}", deviceInfo, reply);
                return true;
            } else if (reply.contains("<ok/>")
                    || (reply.contains("<rpc-error>")
                    && reply.contains("warning"))) {
                log.debug("Device {} sent reply {}", deviceInfo, reply);
                return true;
            }
        }
        log.warn("Device {} has error in reply {}", deviceInfo, reply);
        return false;
    }

    private String sendRequest(String request) throws NetconfException {
        log.info("Mocking NETCONF Session send request: \n" + request);

        if (sampleXmlRegex1.matcher(request).matches()) {
            return SAMPLE_SYSTEM_REPLY;

        } else if (sampleXmlRegex2.matcher(request).matches()) {
            return SAMPLE_SYSTEM_REPLY_INIT;

        } else if (sampleXmlRegexSaFilteringErrorScenario.matcher(request).matches()) {
            return SAMPLE_ERROR_REPLY;

        } else if (sampleXmlRegexSaFiltering.matcher(request).matches()) {
            return SAMPLE_MSEASAFILTERING_FE_REPLY;

        } else if (sampleXmlRegexEditConfigSaFilt.matcher(request).matches()) {
            return SAMPLE_REPLY_OK;

        } else if (sampleXmlRegexEditDeleteSaFilt.matcher(request).matches()) {
            return SAMPLE_REPLY_OK;

        } else if (sampleXmlRegexUniEvc.matcher(request).matches()) {
            return SAMPLE_MSEAEVCUNI_REPLY_INIT;

        } else if (sampleXmlRegexUniEvcUni.matcher(request).matches()) {
            return SAMPLE_MSEAEVCUNI_FE_REPLY;

        } else if (sampleXmlRegexEditConfigUni1Evc.matcher(request).matches()) {
            return SAMPLE_REPLY_OK;

        } else if (sampleXmlRegexEditConfigUni2Evc.matcher(request).matches()) {
            return SAMPLE_REPLY_OK;

        } else if (sampleXmlRegexEditConfigUniProfiles.matcher(request).matches()) {
            return SAMPLE_REPLY_OK;

        } else if (sampleXmlRegexEditConfigEvcDelete.matcher(request).matches()) {
            return SAMPLE_REPLY_OK;

        } else if (sampleXmlRegexGetConfigCeVlanMapsEvc.matcher(request).matches()) {
            return SAMPLE_MSEAEVCUNI_CEVLANMAP_EVC_REPLY;

        } else if (sampleXmlRegexEditConfigCeVlanMapReplace.matcher(request).matches()) {
            return SAMPLE_REPLY_OK;

        } else if (sampleXmlRegexEditConfigBwpGroup1.matcher(request).matches()) {
            return SAMPLE_REPLY_OK;

        } else if (sampleXmlRegexEditConfigBwpGroup1Delete.matcher(request).matches()) {
            return SAMPLE_REPLY_OK;

        } else if (sampleXmlRegexSetCurrentDatetime.matcher(request).matches()) {
            return SAMPLE_REPLY_OK;

        } else if (sampleXmlRegexGetMseaCfmEssentials.matcher(request).matches()) {
            return SAMPLE_MSEACFM_MD_MA_MEP_ESSENTIALS_REPLY;

        } else if (sampleXmlRegexGetMseaCfmFull.matcher(request).matches()) {
            return SAMPLE_MSEACFM_MD_MA_MEP_FULL_REPLY;

        } else if (sampleXmlRegexGetMseaCfmFull2.matcher(request).matches()) {
            return SAMPLE_MSEACFM_MD_MA_MEP_FULL_REPLY2;

        } else if (sampleXmlRegexDeleteMseaCfmMep.matcher(request).matches()) {
            return SAMPLE_REPLY_OK;

        } else if (sampleXmlRegexCreateMseaCfmMa.matcher(request).matches()) {
            return SAMPLE_REPLY_OK;

        } else if (sampleXmlRegexDeleteMseaCfmMa.matcher(request).matches()) {
            return SAMPLE_REPLY_OK;

        } else if (sampleXmlRegexDeleteMseaCfmRmep.matcher(request).matches()) {
            return SAMPLE_REPLY_OK;

        } else if (sampleXmlRegexDeleteMseaCfmMd.matcher(request).matches()) {
            return SAMPLE_REPLY_OK;

        } else if (sampleXmlRegexGetMseaDelay.matcher(request).matches()) {
            return SAMPLE_MSEACFM_DELAY_MEASUREMENT_FULL_REPLY;

        } else if (sampleXmlRegexEditConfigDmCreate.matcher(request).matches()) {
            return SAMPLE_REPLY_OK;

        } else if (sampleXmlRegexTransmitLoopback.matcher(request).matches()) {
            return SAMPLE_REPLY_OK;

        } else if (sampleXmlRegexAbortLoopback.matcher(request).matches()) {
            return SAMPLE_REPLY_OK;

        } else {
            throw new NetconfException("MocknetconfSession. No sendRequest() case for query: " +
                    request);
        }
    }
}
