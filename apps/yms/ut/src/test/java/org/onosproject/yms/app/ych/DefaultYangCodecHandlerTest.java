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

package org.onosproject.yms.app.ych;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.onosproject.yms.ych.YangProtocolEncodingFormat.XML;
import static org.onosproject.yms.ydt.YmsOperationType.QUERY_CONFIG_REQUEST;
import static org.onosproject.yms.ydt.YmsOperationType.QUERY_REQUEST;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.onosproject.yang.gen.v1.ych.combined.rev20160524.CombinedOpParam;
import org.onosproject.yang.gen.v1.ych.combined.rev20160524.combined.AsNum;
import org.onosproject.yang.gen.v1.ych.combined.rev20160524.combined.Attributes;
import org.onosproject.yang.gen.v1.ych.combined.rev20160524.combined.DefaultAttributes;
import org.onosproject.yang.gen.v1.ych.combined.rev20160524.combined.Metric;
import org.onosproject.yang.gen.v1.ych.combined.rev20160524.combined.PathId;
import org.onosproject.yang.gen.v1.ych.combined.rev20160524.combined.attributes.Aigp;
import org.onosproject.yang.gen.v1.ych.combined.rev20160524.combined.attributes.BgpParameters;
import org.onosproject.yang.gen.v1.ych.combined.rev20160524.combined.attributes.DefaultAigp;
import org.onosproject.yang.gen.v1.ych.combined.rev20160524.combined.attributes.DefaultBgpParameters;
import org.onosproject.yang.gen.v1.ych.combined.rev20160524.combined.attributes.DefaultLocalPref;
import org.onosproject.yang.gen.v1.ych.combined.rev20160524.combined.attributes.DefaultMultiExitDisc;
import org.onosproject.yang.gen.v1.ych.combined.rev20160524.combined.attributes.DefaultOrigin;
import org.onosproject.yang.gen.v1.ych.combined.rev20160524.combined.attributes.DefaultUnrecognizedAttributes;
import org.onosproject.yang.gen.v1.ych.combined.rev20160524.combined.attributes.LocalPref;
import org.onosproject.yang.gen.v1.ych.combined.rev20160524.combined.attributes.MultiExitDisc;
import org.onosproject.yang.gen.v1.ych.combined.rev20160524.combined.attributes.Origin;
import org.onosproject.yang.gen.v1.ych.combined.rev20160524.combined.attributes.UnrecognizedAttributes;
import org.onosproject.yang.gen.v1.ych.combined.rev20160524.combined.attributes.aigp.AigpTlv;
import org.onosproject.yang.gen.v1.ych.combined.rev20160524.combined.attributes.aigp.DefaultAigpTlv;
import org.onosproject.yang.gen.v1.ych.combined.rev20160524.combined.attributes.bgpparameters.DefaultOptionalCapabilities;
import org.onosproject.yang.gen.v1.ych.combined.rev20160524.combined.attributes.bgpparameters.OptionalCapabilities;
import org.onosproject.yang.gen.v1.ych.combined.rev20160524.combined.attributes.bgpparameters.optionalcapabilities.Cparameters;
import org.onosproject.yang.gen.v1.ych.combined.rev20160524.combined.attributes.bgpparameters.optionalcapabilities.DefaultCparameters;
import org.onosproject.yang.gen.v1.ych.combined.rev20160524.combined.attributes.bgpparameters.optionalcapabilities.cparameters.As4BytesCapability;
import org.onosproject.yang.gen.v1.ych.combined.rev20160524.combined.attributes.bgpparameters.optionalcapabilities.cparameters.DefaultAs4BytesCapability;
import org.onosproject.yang.gen.v1.ych.empty.container.rev20160524.EmptyContainerOpParam;
import org.onosproject.yang.gen.v1.ych.empty.container.rev20160524.emptycontainer.EmptyContainer;
import org.onosproject.yang.gen.v1.ych.purchasing.supervisor.rev20160524.YchPurchasingsupervisor.OnosYangOpType;
import org.onosproject.yang.gen.v1.ych.purchasing.supervisor.rev20160524.YchPurchasingsupervisorOpParam;
import org.onosproject.yang.gen.v1.ych.purchasing.supervisor.rev20160524.ychpurchasingsupervisor.DefaultYchPurchasingSupervisor;
import org.onosproject.yang.gen.v1.ych.purchasing.supervisor.rev20160524.ychpurchasingsupervisor.YchPurchasingSupervisor;
import org.onosproject.yang.gen.v1.ych.purchasing.supervisor.rev20160524.ychpurchasingsupervisor.ychpurchasingsupervisor.DefaultYchIsManager;
import org.onosproject.yang.gen.v1.ydt.customs.supervisor.rev20160524.CustomssupervisorOpParam;
import org.onosproject.yang.gen.v1.ydt.material.supervisor.rev20160524.MaterialsupervisorOpParam;
import org.onosproject.yang.gen.v1.ydt.material.supervisor.rev20160524.materialsupervisor.DefaultSupervisor;
import org.onosproject.yang.gen.v1.ydt.material.supervisor.rev20160524.materialsupervisor.Supervisor;
import org.onosproject.yang.gen.v1.ydt.merchandiser.supervisor.rev20160524.MerchandisersupervisorOpParam;
import org.onosproject.yang.gen.v1.ydt.root.rev20160524.LogisticsManagerOpParam;
import org.onosproject.yang.gen.v1.ydt.trading.supervisor.rev20160524.TradingsupervisorOpParam;
import org.onosproject.yms.app.ych.defaultcodecs.YangCodecRegistry;
import org.onosproject.yms.app.ysr.DefaultYangSchemaRegistry;
import org.onosproject.yms.app.ysr.TestYangSchemaNodeProvider;
import org.onosproject.yms.ych.YangCompositeEncoding;

/**
 * Unit test case for default codec handler.
 */
public class DefaultYangCodecHandlerTest {
    private TestYangSchemaNodeProvider testYangSchemaNodeProvider =
            new TestYangSchemaNodeProvider();
    private static final String AM_XML = "Incorrect XML generated: ";
    private static final String AM_OBJ = "Incorrect object generated: ";
    private static final String EMPTY_CONTAINER = "EmptyContainerOpParam";
    private static final String LOGISTIC_MOD = "LogisticsManagerOpParam";
    private static final String MERCHA_MOD = "MerchandisersupervisorOpParam";
    private static final String PURCH_MOD = "YchPurchasingsupervisorOpParam";

    /**
     * Returns the xml string for customssupervisor module.
     *
     * @return the xml string for customssupervisor module
     */
    private static String customsXml() {
        return "<filter xmlns=\"ydt.filter-type\" type=\"subtree\">" +
                "<supervisor xmlns=\"ydt.customs-supervisor\">" +
                "Customssupervisor</supervisor>" +
                "</filter>";
    }

    /**
     * Returns the xml string for purchasesupervisor with empty selection node.
     *
     * @return the xml string for purchasesupervisor with empty selection node
     */
    private static String purchaseXmlEmptySelectionNode() {
        return "<filter xmlns=\"ydt.filter-type\" type=\"subtree\">" +
                "<ych-purchasing-supervisor xmlns=\"ych.purchasing-supervisor\">" +
                "<ych-purchasing-specialist/>" +
                "</ych-purchasing-supervisor>" +
                "</filter>";
    }

    /**
     * Returns the xml string for merchandisersupervisor module.
     *
     * @return the xml string for merchandisersupervisor module
     */
    private static String merchandXml() {
        return "<config xmlns=\"ydt.root\" " +
                "xmlns:nc=\"urn:ietf:params:xml:ns:netconf:base:1.0\">" +
                "<supervisor xmlns=\"ydt.Merchandiser-supervisor\">" +
                "Merchandisersupervisor</supervisor>" +
                "</config>";
    }

    /**
     * Returns the xml string for tradingsupervisor module.
     *
     * @return the xml string for tradingsupervisor module
     */
    private static String tradingXml() {
        return "<config xmlns=\"ydt.root\" " +
                "xmlns:nc=\"urn:ietf:params:xml:ns:netconf:base:1.0\">" +
                "<supervisor xmlns=\"ydt.trading-supervisor\">" +
                "Tradingsupervisor</supervisor>" +
                "</config>";
    }

    /**
     * Returns the xml string for customssupervisor module.
     *
     * @return the xml string for customssupervisor module
     */
    private static String customsCompositeXml() {
        return "<filter xmlns=\"ydt.filter-type\">" +
                "<supervisor xmlns=\"ydt.customs-supervisor\">" +
                "Customssupervisor</supervisor></filter>";
    }

    /**
     * Returns the xml string for customssupervisor module with filter-type.
     *
     * @return the xml string for customssupervisor module with filter-type
     */
    private static String customsEmptyXml() {
        return "<filter xmlns=\"ydt.filter-type\" type=\"subtree\">" +
                "</filter>";
    }

    /**
     * Returns the xml string for materialsupervisor module.
     *
     * @return the xml string for materialsupervisor module
     */
    private static String materialXml() {
        return "<filter xmlns=\"ydt.filter-type\" type=\"subtree\">" +
                "<supervisor xmlns=\"ydt.material-supervisor\">" +
                "<name>abc1</name><departmentId>xyz1</departmentId>" +
                "</supervisor>" +
                "<supervisor xmlns=\"ydt.material-supervisor\"" +
                "><name>abc2</name><departmentId>xyz2</departmentId>" +
                "</supervisor>" +
                "<supervisor xmlns=\"ydt.material-supervisor\"" +
                "><name>abc3</name><departmentId>xyz3</departmentId>" +
                "</supervisor>" +
                "<supervisor xmlns=\"ydt.material-supervisor\"" +
                "><name>abc4</name><departmentId>xyz4</departmentId>" +
                "</supervisor>" +
                "<supervisor xmlns=\"ydt.material-supervisor\"" +
                "><name>abc5</name><departmentId>xyz5</departmentId>" +
                "</supervisor>" +
                "</filter>";
    }

    /**
     * Returns the xml string for EmptyContainer module.
     *
     * @return the xml string for EmptyContainer module
     */
    private static String containerEmptyXml() {
        return "<filter xmlns=\"ydt.filter-type\" type=\"subtree\">" +
                "</filter>";
    }

    /**
     * Returns the xml string for Combined module.
     *
     * @return the xml string for Combined module
     */
    private static String listTestXml() {
        return "<filter xmlns=\"ydt.filter-type\" type=\"subtree\">" +
                "<attributes xmlns=\"ych:combined\">" +
                "<origin><value>123</value></origin>" +
                "<multi-exit-disc><med>456</med></multi-exit-disc>" +
                "<local-pref><pref>23</pref></local-pref>" +
                "<aigp><aigp-tlv><metric>456</metric></aigp-tlv></aigp>" +
                "<unrecognized-attributes><partial>false</partial>" +
                "<transitive>false</transitive><type>1</type>" +
                "<value>QUJD</value></unrecognized-attributes>" +
                "<unrecognized-attributes><partial>true</partial>" +
                "<transitive>true</transitive><type>2</type>" +
                "<value>QUJD</value></unrecognized-attributes>" +
                "<unrecognized-attributes><partial>true</partial>" +
                "<transitive>false</transitive><type>3</type>" +
                "<value>QUJD</value></unrecognized-attributes>" +
                "<unrecognized-attributes><partial>false</partial>" +
                "<transitive>true</transitive><type>4</type>" +
                "<value>QUJD</value></unrecognized-attributes>" +
                "<bgp-parameters><optional-capabilities><c-parameters>" +
                "<as4-bytes-capability><as-number>11</as-number>" +
                "</as4-bytes-capability></c-parameters>" +
                "</optional-capabilities><optional-capabilities>" +
                "<c-parameters><as4-bytes-capability>" +
                "<as-number>22</as-number></as4-bytes-capability>" +
                "</c-parameters></optional-capabilities>" +
                "<optional-capabilities><c-parameters><as4-bytes-capability>" +
                "<as-number>33</as-number></as4-bytes-capability>" +
                "</c-parameters></optional-capabilities></bgp-parameters>" +
                "<bgp-parameters><optional-capabilities><c-parameters>" +
                "<as4-bytes-capability><as-number>11</as-number>" +
                "</as4-bytes-capability></c-parameters>" +
                "</optional-capabilities><optional-capabilities>" +
                "<c-parameters><as4-bytes-capability>" +
                "<as-number>22</as-number></as4-bytes-capability>" +
                "</c-parameters></optional-capabilities>" +
                "<optional-capabilities><c-parameters><as4-bytes-capability>" +
                "<as-number>33</as-number></as4-bytes-capability>" +
                "</c-parameters></optional-capabilities>" +
                "</bgp-parameters></attributes></filter>";
    }

    /**
     * Returns the xml string for ych-purchasingsupervisor module.
     *
     * @return the XML string for ych-purchasingsupervisor module
     */
    private static String purchaseXml() {
        return "<config xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\" " +
                "xmlns:nc=\"urn:ietf:params:xml:ns:netconf:base:1.0\">" +
                "<ych-purchasing-supervisor xmlns=\"ych.purchasing-supervisor\" " +
                "nc:operation=\"create\">" +
                "<ych-purchasing-specialist>purchasingSpecialist" +
                "</ych-purchasing-specialist>" +
                "<ych-purchasing-support>support</ych-purchasing-support>" +
                "<ych-is-manager/>" +
                "</ych-purchasing-supervisor>" +
                "</config>";
    }

    /**
     * Returns the xml string for ych-purchasingsupervisor module with BitSet options.
     *
     * @return the XML string for ych-purchasingsupervisor module
     */
    private static String purchaseXmlOptions(BitSet options) {
        boolean isFirst = true;

        StringBuffer sb = new StringBuffer();
        sb.append("<config xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\" ");
        sb.append("xmlns:nc=\"urn:ietf:params:xml:ns:netconf:base:1.0\">");
        sb.append("<ych-purchasing-supervisor xmlns=\"ych.purchasing-supervisor\" " +
                "nc:operation=\"create\">");
        sb.append("<ych-purchasing-support>support</ych-purchasing-support>");
        if (options == null || options.isEmpty()) {
            sb.append("<ych-purchasing-options/>");
        } else {
            sb.append("<ych-purchasing-options>");
            for (int i = 0; i < 4; i++) {
                if (options.get(i)) {
                    if (isFirst) {
                        isFirst = false;
                    } else {
                        sb.append(' ');
                    }
                    sb.append("option" + i);
                }
            }
            sb.append("</ych-purchasing-options>");
        }
        sb.append("</ych-purchasing-supervisor>");
        sb.append("</config>");

        return sb.toString();
    }

    /**
     * Returns the xml string for employeeid module.
     *
     * @return the xml string for employeeid module
     */
    private static String emplyIdXml() {
        return "<config xmlns=\"ydt.root\">" +
                "<employeeid xmlns=\"ydt.employee-id\">" +
                "<employeeid>Employ1</employeeid>" +
                "<employeeid>Employ2</employeeid>" +
                "<employeeid>Employ3</employeeid>" +
                "<employeeid>Employ4</employeeid>" +
                "<employeeid>Employ5</employeeid>" +
                "</employeeid>" +
                "</config>";
    }

    /**
     * Returns the xml string for warehousesupervisor module.
     *
     * @return the xml string for warehousesupervisor module
     */
    private static String wareHseXml() {
        return "<config xmlns=\"ydt.root\">" +
                "<warehousesupervisor xmlns=\"ydt.warehouse-supervisor\">" +
                "<supervisor>supervisor1</supervisor>" +
                "<supervisor>supervisor2</supervisor>" +
                "<supervisor>supervisor3</supervisor>" +
                "<supervisor>supervisor4</supervisor>" +
                "<supervisor>supervisor5</supervisor>" +
                "</warehousesupervisor>" +
                "</config>";
    }

    /**
     * Returns the xml string for more than one module.
     *
     * @return the xml string for more than one module
     */
    private static String multiModuleXml() {
        return "<config xmlns=\"ydt.root\">" +
                "<customssupervisor xmlns=\"ydt.customs-supervisor\">" +
                "<supervisor>Customssupervisor</supervisor>" +
                "</customssupervisor>" +
                "<merchandisersupervisor xmlns=\"ydt.Merchandiser-supervisor\">" +
                "<supervisor>Merchandisersupervisor</supervisor>" +
                "</merchandisersupervisor>" +
                "<materialsupervisor xmlns=\"ydt.material-supervisor\">" +
                "<supervisor>" +
                "<name>abc1</name>" +
                "<departmentId>xyz1</departmentId>" +
                "</supervisor>" +
                "<supervisor>" +
                "<name>abc2</name>" +
                "<departmentId>xyz2</departmentId>" +
                "</supervisor>" +
                "<supervisor>" +
                "<name>abc3</name>" +
                "<departmentId>xyz3</departmentId>" +
                "</supervisor>" +
                "<supervisor>" +
                "<name>abc4</name>" +
                "<departmentId>xyz4</departmentId>" +
                "</supervisor>" +
                "<supervisor>" +
                "<name>abc5</name>" +
                "<departmentId>xyz5</departmentId>" +
                "</supervisor>" +
                "</materialsupervisor>" +
                "<ych-purchasingsupervisor xmlns=\"ych.purchasing-supervisor\">" +
                "<ych-purchasing-supervisor>" +
                "<ych-purchasing-specialist>purchasingSpecialist" +
                "</ych-purchasing-specialist>" +
                "<ych-purchasing-support>support</ych-purchasing-support>" +
                "</ych-purchasing-supervisor>" +
                "</ych-purchasingsupervisor>" +
                "<warehousesupervisor xmlns=\"ydt.warehouse-supervisor\">" +
                "<supervisor>supervisor1</supervisor>" +
                "<supervisor>supervisor2</supervisor>" +
                "<supervisor>supervisor3</supervisor>" +
                "<supervisor>supervisor4</supervisor>" +
                "<supervisor>supervisor5</supervisor>" +
                "</warehousesupervisor>" +
                "<tradingsupervisor xmlns=\"ydt.trading-supervisor\">" +
                "<supervisor>Tradingsupervisor</supervisor>" +
                "</tradingsupervisor>" +
                "<employeeid xmlns=\"ydt.employee-id\">" +
                "<employeeid>Employ1</employeeid>" +
                "<employeeid>Employ2</employeeid>" +
                "<employeeid>Employ3</employeeid>" +
                "<employeeid>Employ4</employeeid>" +
                "<employeeid>Employ5</employeeid>" +
                "</employeeid>" +
                "</config>";
    }

    /**
     * Returns the xml string for more than one module.
     *
     * @return the xml string for more than one module
     */
    private String multipleAppxml() {
        return "<filter xmlns=\"ydt.filter-type\" type=\"subtree\"><supervisor" +
                " xmlns=\"ydt.customs-supervisor\">Customssupervisor" +
                "</supervisor><supervisor xmlns=\"ydt.material-supervisor\"" +
                "><name>abc1</name><departmentId>xyz1</departmentId" +
                "></supervisor><supervisor xmlns=\"ydt.material-supervisor\">" +
                "<name>abc2</name><departmentId>xyz2</departmentId>" +
                "</supervisor><supervisor xmlns=\"ydt" +
                ".material-supervisor\"><name>abc3</name><departmentId>xyz3" +
                "</departmentId></supervisor><supervisor xmlns=\"ydt" +
                ".material-supervisor\"><name>abc4</name><departmentId>xyz4" +
                "</departmentId></supervisor><supervisor xmlns=\"ydt" +
                ".material-supervisor\"><name>abc5</name><departmentId>xyz5" +
                "</departmentId></supervisor></filter>";
    }

    /**
     * Unit test case in which verifying xml string for module object with leaf
     * for composite encode.
     */
    @Test
    public void proceessCodecHandlerForCompositeEnc() {
        testYangSchemaNodeProvider.processSchemaRegistry(null);
        DefaultYangSchemaRegistry schemaRegistry = testYangSchemaNodeProvider
                .getDefaultYangSchemaRegistry();

        // Creating the object
        Object object = CustomssupervisorOpParam.builder()
                .supervisor("Customssupervisor").build();

        // Get the xml string and compare
        Map<String, String> tagAttr = new HashMap<String, String>();
        tagAttr.put("type", "subtree");

        YangCodecRegistry.initializeDefaultCodec();
        DefaultYangCodecHandler defaultYangCodecHandler =
                new DefaultYangCodecHandler(schemaRegistry);
        YangCompositeEncoding xml =
                defaultYangCodecHandler
                        .encodeCompositeOperation("filter", "ydt.filter-type",
                                                  object, XML, null);

        assertNull("customs-super: resource id not null",
                   xml.getResourceIdentifier());
        assertEquals(AM_XML + "customs-super: comp res info",
                     customsCompositeXml(), xml.getResourceInformation());

        // Creating the object
        object = MerchandisersupervisorOpParam.builder()
                .supervisor("Merchandisersupervisor").build();

        // Get the xml string and compare
        xml = defaultYangCodecHandler.encodeCompositeOperation("config",
                                                               "ydt.root",
                                                               object, XML,
                                                               null);
        assertNull("merch-super: res id not null", xml.getResourceIdentifier());
        assertEquals(AM_XML + "merch-super: comp res info",
                     merchandXml(), xml.getResourceInformation());

        // Creating the object
        object = TradingsupervisorOpParam.builder()
                .supervisor("Tradingsupervisor").build();

        // Get the xml string and compare
        xml = defaultYangCodecHandler
                .encodeCompositeOperation("config", "ydt.root", object, XML,
                                          null);
        assertNull("trading-super: res id not null",
                   xml.getResourceIdentifier());
        assertEquals(AM_XML + "trading-super: comp res info",
                     tradingXml(), xml.getResourceInformation());
    }

    /**
     * Unit test case in which verifying xml string for module object with leaf.
     */
    @Test
    public void proceessCodecHandlerForLeaf() {
        testYangSchemaNodeProvider.processSchemaRegistry(null);
        DefaultYangSchemaRegistry schemaRegistry = testYangSchemaNodeProvider
                .getDefaultYangSchemaRegistry();
        List<Object> yangModuleList = new ArrayList<>();

        // Creating the object
        Object object = CustomssupervisorOpParam.builder()
                .supervisor("Customssupervisor").build();
        yangModuleList.add(object);

        // Get the xml string and compare
        Map<String, String> tagAttr = new HashMap<String, String>();
        tagAttr.put("type", "subtree");

        YangCodecRegistry.initializeDefaultCodec();
        DefaultYangCodecHandler defaultYangCodecHandler =
                new DefaultYangCodecHandler(schemaRegistry);
        String xml =
                defaultYangCodecHandler.encodeOperation("filter",
                                                        "ydt.filter-type",
                                                        tagAttr, yangModuleList,
                                                        XML, null);

        assertEquals(AM_XML + "customs-super: leaf info", customsXml(), xml);

        // Creating the object
        object = MerchandisersupervisorOpParam.builder()
                .supervisor("Merchandisersupervisor").build();
        yangModuleList.clear();
        yangModuleList.add(object);

        // Get the xml string and compare
        xml = defaultYangCodecHandler.encodeOperation("config", "ydt.root",
                                                      null, yangModuleList,
                                                      XML, null);
        assertEquals(AM_XML + "merchandiser-super: leaf info", merchandXml(),
                     xml);

        // Creating the object
        object = TradingsupervisorOpParam.builder()
                .supervisor("Tradingsupervisor").build();
        yangModuleList.clear();
        yangModuleList.add(object);

        // Get the xml string and compare
        xml = defaultYangCodecHandler.encodeOperation("config", "ydt.root",
                                                      null, yangModuleList,
                                                      XML, null);
        assertEquals(AM_XML + "trading-super: leaf info", tradingXml(), xml);
    }

    /**
     * Unit test case in which verifying xml string for module object with
     * empty leaf.
     */
    @Test
    public void proceessCodecHandlerForEmptyLeaf() {
        testYangSchemaNodeProvider.processSchemaRegistry(null);
        DefaultYangSchemaRegistry schemaRegistry = testYangSchemaNodeProvider
                .getDefaultYangSchemaRegistry();
        List<Object> yangModuleList = new ArrayList<>();

        // Creating the object
        Object object = CustomssupervisorOpParam.builder().supervisor("")
                .build();
        yangModuleList.add(object);

        // Get the xml string and compare
        Map<String, String> tagAttr = new HashMap<String, String>();
        tagAttr.put("type", "subtree");

        YangCodecRegistry.initializeDefaultCodec();
        DefaultYangCodecHandler codecHandler =
                new DefaultYangCodecHandler(schemaRegistry);
        String xml = codecHandler.encodeOperation("filter", "ydt.filter-type",
                                                  tagAttr, yangModuleList,
                                                  XML, null);

        assertEquals(AM_XML + "customs-super: leaf is not empty",
                     customsEmptyXml(), xml);
    }

    /**
     * Unit test case in which verifying xml string for more than one module
     * object.
     */
    @Test
    public void proceessCodecHandlerForMultipleApp() {
        testYangSchemaNodeProvider.processSchemaRegistry(null);
        DefaultYangSchemaRegistry schemaRegistry = testYangSchemaNodeProvider
                .getDefaultYangSchemaRegistry();
        List<Object> yangModuleList = new ArrayList<>();

        // Creating the object
        Object object = CustomssupervisorOpParam.builder()
                .supervisor("Customssupervisor").build();
        yangModuleList.add(object);

        // Creating the object
        Supervisor supervisor1 = new DefaultSupervisor.SupervisorBuilder()
                .name("abc1").departmentId("xyz1").build();
        Supervisor supervisor2 = new DefaultSupervisor.SupervisorBuilder()
                .name("abc2").departmentId("xyz2").build();
        Supervisor supervisor3 = new DefaultSupervisor.SupervisorBuilder()
                .name("abc3").departmentId("xyz3").build();
        Supervisor supervisor4 = new DefaultSupervisor.SupervisorBuilder()
                .name("abc4").departmentId("xyz4").build();
        Supervisor supervisor5 = new DefaultSupervisor.SupervisorBuilder()
                .name("abc5").departmentId("xyz5").build();

        Object object1 = MaterialsupervisorOpParam.builder()
                .addToSupervisor(supervisor1)
                .addToSupervisor(supervisor2)
                .addToSupervisor(supervisor3)
                .addToSupervisor(supervisor4)
                .addToSupervisor(supervisor5).build();
        yangModuleList.add(object1);

        // Get the xml string and compare
        Map<String, String> tagAttr = new HashMap<String, String>();
        tagAttr.put("type", "subtree");

        YangCodecRegistry.initializeDefaultCodec();
        DefaultYangCodecHandler codecHandler =
                new DefaultYangCodecHandler(schemaRegistry);
        String xml = codecHandler.encodeOperation("filter", "ydt.filter-type",
                                                  tagAttr, yangModuleList,
                                                  XML, null);

        assertEquals(AM_XML + "for multiple applications",
                     multipleAppxml(), xml);
    }

    /**
     * Unit test case in which verifying xml string for module object with list.
     */
    @Test
    public void proceessCodecHandlerForList() {
        testYangSchemaNodeProvider.processSchemaRegistry(null);
        DefaultYangSchemaRegistry schemaRegistry =
                testYangSchemaNodeProvider.getDefaultYangSchemaRegistry();
        List<Object> yangModuleList = new ArrayList<>();

        // Creating the object
        Supervisor supervisor1 = new DefaultSupervisor.SupervisorBuilder()
                .name("abc1").departmentId("xyz1").build();
        Supervisor supervisor2 = new DefaultSupervisor.SupervisorBuilder()
                .name("abc2").departmentId("xyz2").build();
        Supervisor supervisor3 = new DefaultSupervisor.SupervisorBuilder()
                .name("abc3").departmentId("xyz3").build();
        Supervisor supervisor4 = new DefaultSupervisor.SupervisorBuilder()
                .name("abc4").departmentId("xyz4").build();
        Supervisor supervisor5 = new DefaultSupervisor.SupervisorBuilder()
                .name("abc5").departmentId("xyz5").build();

        Object object = MaterialsupervisorOpParam.builder()
                .addToSupervisor(supervisor1)
                .addToSupervisor(supervisor2)
                .addToSupervisor(supervisor3)
                .addToSupervisor(supervisor4)
                .addToSupervisor(supervisor5).build();

        yangModuleList.add(object);

        // Get the xml string and compare
        Map<String, String> tagAttr = new HashMap<String, String>();
        tagAttr.put("type", "subtree");

        YangCodecRegistry.initializeDefaultCodec();
        DefaultYangCodecHandler codecHandler =
                new DefaultYangCodecHandler(schemaRegistry);
        String xml = codecHandler.encodeOperation("filter", "ydt.filter-type",
                                                  tagAttr, yangModuleList,
                                                  XML, null);
        assertEquals(AM_XML + "material-super: list info", materialXml(), xml);
    }

    /**
     * Unit test case in which verifying xml string for module object with
     * empty container.
     */
    @Test
    public void proceessCodecHandlerForEmptyContainer() {
        testYangSchemaNodeProvider.processSchemaRegistry(null);
        DefaultYangSchemaRegistry schemaRegistry =
                testYangSchemaNodeProvider.getDefaultYangSchemaRegistry();
        List<Object> yangModuleList = new ArrayList<>();

        // Creating the object
        EmptyContainer emptyContainer = EmptyContainerOpParam.builder()
                .emptyContainer();
        Object object = EmptyContainerOpParam.builder()
                .emptyContainer(emptyContainer).build();

        yangModuleList.add(object);

        // Get the xml string and compare
        Map<String, String> tagAttr = new HashMap<String, String>();
        tagAttr.put("type", "subtree");

        YangCodecRegistry.initializeDefaultCodec();
        DefaultYangCodecHandler codecHandler =
                new DefaultYangCodecHandler(schemaRegistry);
        String xml = codecHandler.encodeOperation("filter", "ydt.filter-type",
                                                  tagAttr, yangModuleList,
                                                  XML, null);
        assertEquals(AM_XML + "empty-contain: container is not empty",
                     containerEmptyXml(), xml);
    }

    /**
     * Unit test case in which verifying xml string for module object with list
     * inside list.
     */
    @Test
    public void proceessCodecHandlerForListInsideList() {
        testYangSchemaNodeProvider.processSchemaRegistry(null);
        DefaultYangSchemaRegistry schemaRegistry =
                testYangSchemaNodeProvider.getDefaultYangSchemaRegistry();
        List<Object> yangModuleList = new ArrayList<>();

        // Creating the object
        PathId pathId = new PathId(123);
        Origin origin = new DefaultOrigin.OriginBuilder().value(pathId)
                .build();
        MultiExitDisc multiExitDisc = new DefaultMultiExitDisc
                .MultiExitDiscBuilder().med(456).build();
        LocalPref localPref = new DefaultLocalPref.LocalPrefBuilder()
                .pref(23).build();
        Metric metric = new Metric(456);
        AigpTlv aigpTlv = new DefaultAigpTlv.AigpTlvBuilder().metric(metric)
                .build();
        Aigp aigp = new DefaultAigp.AigpBuilder().aigpTlv(aigpTlv).build();

        UnrecognizedAttributes unrecognizedAttributes1 =
                new DefaultUnrecognizedAttributes
                        .UnrecognizedAttributesBuilder()
                        .partial(false).transitive(false).type((short) 1)
                        .value("ABC".getBytes()).build();

        UnrecognizedAttributes unrecognizedAttributes2 =
                new DefaultUnrecognizedAttributes
                        .UnrecognizedAttributesBuilder()
                        .partial(true).transitive(true).type((short) 2)
                        .value("ABC".getBytes())
                        .build();

        UnrecognizedAttributes unrecognizedAttributes3 =
                new DefaultUnrecognizedAttributes
                        .UnrecognizedAttributesBuilder()
                        .partial(true).transitive(false).type((short) 3)
                        .value("ABC".getBytes())
                        .build();

        UnrecognizedAttributes unrecognizedAttributes4 =
                new DefaultUnrecognizedAttributes
                        .UnrecognizedAttributesBuilder()
                        .partial(false).transitive(true).type((short) 4)
                        .value("ABC".getBytes()).build();

        AsNum asNum1 = new AsNum(11);
        As4BytesCapability as4BytesCapability1 =
                new DefaultAs4BytesCapability.As4BytesCapabilityBuilder()
                        .asNumber(asNum1).build();
        Cparameters cparameters1 = new DefaultCparameters.CparametersBuilder()
                .as4BytesCapability(as4BytesCapability1)
                .build();
        OptionalCapabilities optionalCapabilities1 =
                new DefaultOptionalCapabilities.OptionalCapabilitiesBuilder()
                        .cParameters(cparameters1).build();

        AsNum asNum2 = new AsNum(22);
        As4BytesCapability as4BytesCapability2 =
                new DefaultAs4BytesCapability.As4BytesCapabilityBuilder()
                        .asNumber(asNum2).build();
        Cparameters cparameters2 = new DefaultCparameters.CparametersBuilder()
                .as4BytesCapability(as4BytesCapability2)
                .build();
        OptionalCapabilities optionalCapabilities2 =
                new DefaultOptionalCapabilities.OptionalCapabilitiesBuilder()
                        .cParameters(cparameters2).build();

        AsNum asNum3 = new AsNum(33);
        As4BytesCapability as4BytesCapability3 =
                new DefaultAs4BytesCapability.As4BytesCapabilityBuilder()
                        .asNumber(asNum3).build();
        Cparameters cparameters3 = new DefaultCparameters.CparametersBuilder()
                .as4BytesCapability(as4BytesCapability3)
                .build();
        OptionalCapabilities optionalCapabilities3 =
                new DefaultOptionalCapabilities.OptionalCapabilitiesBuilder()
                        .cParameters(cparameters3).build();

        BgpParameters bgpParameters1 =
                new DefaultBgpParameters.BgpParametersBuilder()
                        .addToOptionalCapabilities(optionalCapabilities1)
                        .addToOptionalCapabilities(optionalCapabilities2)
                        .addToOptionalCapabilities(optionalCapabilities3)
                        .build();

        AsNum asNum4 = new AsNum(11);
        As4BytesCapability as4BytesCapability4 = new DefaultAs4BytesCapability
                .As4BytesCapabilityBuilder()
                .asNumber(asNum4).build();
        Cparameters cparameters4 = new DefaultCparameters.CparametersBuilder()
                .as4BytesCapability(as4BytesCapability4)
                .build();
        OptionalCapabilities optionalCapabilities4 =
                new DefaultOptionalCapabilities.OptionalCapabilitiesBuilder()
                        .cParameters(cparameters4).build();

        AsNum asNum5 = new AsNum(22);
        As4BytesCapability as4BytesCapability5 =
                new DefaultAs4BytesCapability.As4BytesCapabilityBuilder()
                        .asNumber(asNum5).build();
        Cparameters cparameters5 =
                new DefaultCparameters.CparametersBuilder()
                        .as4BytesCapability(as4BytesCapability5)
                        .build();
        OptionalCapabilities optionalCapabilities5 =
                new DefaultOptionalCapabilities.OptionalCapabilitiesBuilder()
                        .cParameters(cparameters5).build();

        AsNum asNum6 = new AsNum(33);
        As4BytesCapability as4BytesCapability6 =
                new DefaultAs4BytesCapability.As4BytesCapabilityBuilder()
                        .asNumber(asNum6).build();
        Cparameters cparameters6 =
                new DefaultCparameters.CparametersBuilder()
                        .as4BytesCapability(as4BytesCapability6)
                        .build();
        OptionalCapabilities optionalCapabilities6 =
                new DefaultOptionalCapabilities.OptionalCapabilitiesBuilder()
                        .cParameters(cparameters6).build();

        BgpParameters bgpParameters2 =
                new DefaultBgpParameters.BgpParametersBuilder()
                        .addToOptionalCapabilities(optionalCapabilities4)
                        .addToOptionalCapabilities(optionalCapabilities5)
                        .addToOptionalCapabilities(optionalCapabilities6)
                        .build();

        Attributes attributes = new DefaultAttributes.AttributesBuilder()
                .origin(origin)
                .multiExitDisc(multiExitDisc)
                .localPref(localPref)
                .aigp(aigp)
                .addToUnrecognizedAttributes(unrecognizedAttributes1)
                .addToUnrecognizedAttributes(unrecognizedAttributes2)
                .addToUnrecognizedAttributes(unrecognizedAttributes3)
                .addToUnrecognizedAttributes(unrecognizedAttributes4)
                .addToBgpParameters(bgpParameters1)
                .addToBgpParameters(bgpParameters2).build();
        Object object = CombinedOpParam.builder().attributes(attributes)
                .build();

        yangModuleList.add(object);

        // Get the xml string and compare
        Map<String, String> tagAttr = new HashMap<String, String>();
        tagAttr.put("type", "subtree");

        YangCodecRegistry.initializeDefaultCodec();
        DefaultYangCodecHandler codecHandler =
                new DefaultYangCodecHandler(schemaRegistry);
        String xml = codecHandler.encodeOperation("filter", "ydt.filter-type",
                                                  tagAttr, yangModuleList,
                                                  XML, null);
        assertEquals(AM_XML + "combined: list info", listTestXml(), xml);
    }

//    TODO negative scenario will be handled later
    /**
     * Unit test case in which verifying xml string for module object with
     * container.
     */
    @Test
    public void proceessCodecHandlerForContainer() {
        testYangSchemaNodeProvider.processSchemaRegistry(null);
        DefaultYangSchemaRegistry schemaRegistry =
                testYangSchemaNodeProvider.getDefaultYangSchemaRegistry();
        List<Object> yangModuleList = new ArrayList<>();

        // Creating the object
        YchPurchasingSupervisor supervisor =
                new DefaultYchPurchasingSupervisor
                        .YchPurchasingSupervisorBuilder()
                        .ychPurchasingSpecialist("purchasingSpecialist")
                        .ychPurchasingSupport("support")
                        .ychIsManager(DefaultYchIsManager.builder().build())
                        .yangYchPurchasingSupervisorOpType(OnosYangOpType.CREATE).build();
        Object object = YchPurchasingsupervisorOpParam.builder()
                .ychPurchasingSupervisor(supervisor).build();
        yangModuleList.add(object);

        YangCodecRegistry.initializeDefaultCodec();
        DefaultYangCodecHandler codecHandler =
                new DefaultYangCodecHandler(schemaRegistry);
        String xml = codecHandler.encodeOperation("config", "urn:ietf:params:xml:ns:netconf:base:1.0",
                                                  null, yangModuleList,
                                                  XML, null);
        assertEquals(AM_XML + "puchas-super: container info", purchaseXml(),
                     xml);
    }

//    /**
//     * Unit test case in which verifying xml string for module object with
//     * leaf list.
//     */
//    @Test
//    public void proceessCodecHandlerForLeafList() {
//        testYangSchemaNodeProvider.processSchemaRegistry(null);
//        DefaultYangSchemaRegistry schemaRegistry =
//                testYangSchemaNodeProvider.getDefaultYangSchemaRegistry();
//        List<Object> yangModuleList = new ArrayList<>();
//
//        // Creating the object
//        EmployeeidOpParam.EmployeeidBuilder employeeidBuilder =
//                EmployeeidOpParam.builder();
//        employeeidBuilder.addToEmployeeid("Employ1");
//        employeeidBuilder.addToEmployeeid("Employ2");
//        employeeidBuilder.addToEmployeeid("Employ3");
//        employeeidBuilder.addToEmployeeid("Employ4");
//        employeeidBuilder.addToEmployeeid("Employ5");
//
//        Object object = employeeidBuilder.build();
//        yangModuleList.add(object);
//
//        // Get the xml string and compare
//        YangCodecRegistry.initializeDefaultCodec();
//        DefaultYangCodecHandler codecHandler =
//                new DefaultYangCodecHandler(schemaRegistry);
//        String xml = codecHandler.encodeOperation("config", "ydt.root", null,
//                                                  yangModuleList, XML, null);
//        assertEquals(AM_XML + "employ-id: leaf-list info", emplyIdXml(), xml);
//        WarehousesupervisorOpParam.WarehousesupervisorBuilder warehsebldr =
//                WarehousesupervisorOpParam.builder();
//        warehsebldr.addToSupervisor("supervisor1");
//        warehsebldr.addToSupervisor("supervisor2");
//        warehsebldr.addToSupervisor("supervisor3");
//        warehsebldr.addToSupervisor("supervisor4");
//        warehsebldr.addToSupervisor("supervisor5");
//
//        object = warehsebldr.build();
//        yangModuleList.clear();
//        yangModuleList.add(object);
//
//
//        // Get the xml string and compare
//        xml = codecHandler.encodeOperation("config", "ydt.root", null,
//                                           yangModuleList, XML, null);
//
//        assertEquals(AM_XML + "warehouse-super: leaf-list info", wareHseXml(),
//                     xml);
//    }

//    /**
//     * Unit test case in which verifying xml string for multiple module object.
//     */
//    @Test
//    public void proceessCodecHandlerForMultipleModule() {
//        testYangSchemaNodeProvider.processSchemaRegistry(null);
//        DefaultYangSchemaRegistry schemaRegistry =
//                testYangSchemaNodeProvider.getDefaultYangSchemaRegistry();
//
//        List<Object> yangModuleList = new ArrayList<>();
//        YangCodecRegistry.initializeDefaultCodec();
//        DefaultYangCodecHandler codecHandler =
//                new DefaultYangCodecHandler(schemaRegistry);
//
//        // Creating the object for customssupervisor module
//        Object object = CustomssupervisorOpParam.builder()
//                .supervisor("Customssupervisor").build();
//        yangModuleList.add(object);
//
//        // Creating the object for merchandisersupervisor module
//        object = MerchandisersupervisorOpParam.builder()
//                .supervisor("Merchandisersupervisor").build();
//        yangModuleList.add(object);
//
//        // Creating the object for materialsupervisor module
//        Supervisor supervisor1 = new DefaultSupervisor.SupervisorBuilder()
//                .name("abc1").departmentId("xyz1").build();
//        Supervisor supervisor2 = new DefaultSupervisor.SupervisorBuilder()
//                .name("abc2").departmentId("xyz2").build();
//        Supervisor supervisor3 = new DefaultSupervisor.SupervisorBuilder()
//                .name("abc3").departmentId("xyz3").build();
//        Supervisor supervisor4 = new DefaultSupervisor.SupervisorBuilder()
//                .name("abc4").departmentId("xyz4").build();
//        Supervisor supervisor5 = new DefaultSupervisor.SupervisorBuilder()
//                .name("abc5").departmentId("xyz5").build();
//
//        object = MaterialsupervisorOpParam.builder()
//                .addToSupervisor(supervisor1)
//                .addToSupervisor(supervisor2)
//                .addToSupervisor(supervisor3)
//                .addToSupervisor(supervisor4)
//                .addToSupervisor(supervisor5).build();
//
//        yangModuleList.add(object);
//
//        // Creating the object for YchPurchasingsupervisor module
//        YchPurchasingSupervisor purSupervisor =
//                new DefaultYchPurchasingSupervisor
//                        .YchPurchasingSupervisorBuilder()
//                        .ychPurchasingSpecialist("purchasingSpecialist")
//                        .ychPurchasingSupport("support").build();
//        object = YchPurchasingsupervisorOpParam.builder()
//                .ychPurchasingSupervisor(purSupervisor).build();
//        yangModuleList.add(object);
//
//        // Creating the object for warehousesupervisor module
//        WarehousesupervisorOpParam.WarehousesupervisorBuilder warehsebldr =
//                WarehousesupervisorOpParam.builder();
//        warehsebldr.addToSupervisor("supervisor1");
//        warehsebldr.addToSupervisor("supervisor2");
//        warehsebldr.addToSupervisor("supervisor3");
//        warehsebldr.addToSupervisor("supervisor4");
//        warehsebldr.addToSupervisor("supervisor5");
//
//        object = warehsebldr.build();
//        yangModuleList.add(object);
//
//        // Creating the object for tradingsupervisor module
//        object = TradingsupervisorOpParam.builder()
//                .supervisor("Tradingsupervisor").build();
//        yangModuleList.add(object);
//
//        List<String> employeeid = EmployeeidOpParam.builder().employeeid();
//        if (employeeid == null) {
//            employeeid = new ArrayList<>();
//        }
//        employeeid.add("Employ1");
//        employeeid.add("Employ2");
//        employeeid.add("Employ3");
//        employeeid.add("Employ4");
//        employeeid.add("Employ5");
//
//        // Creating the object for employeeid module
//        object = EmployeeidOpParam.builder().employeeid(employeeid).build();
//        yangModuleList.add(object);
//
//        // Get the xml string and compare
//        String xml = codecHandler.encodeOperation("config", "ydt.root", null,
//                                                  yangModuleList, XML, null);
//        assertEquals(AM_XML + "multiple: module info", multiModuleXml(), xml);
//    }

    /**
     * Unit test case in which verifying object for xml string with config as
     * root name and multiple module.
     */
    @Test
    public void proceessCodecDecodeFunctionForListInsideList() {
        String path = "src/test/resources/ychTestResourceFiles/combinedrootname.xml";
        StringBuilder sb = new StringBuilder();
        String sCurrentLine;
        testYangSchemaNodeProvider.processSchemaRegistry(null);
        DefaultYangSchemaRegistry schemaRegistry =
                testYangSchemaNodeProvider.getDefaultYangSchemaRegistry();

        YangCodecRegistry.initializeDefaultCodec();
        DefaultYangCodecHandler defaultYangCodecHandler =
                new DefaultYangCodecHandler(schemaRegistry);

        try (BufferedReader br = new BufferedReader(new FileReader(path))) {

            while ((sCurrentLine = br.readLine()) != null) {
                sb.append(sCurrentLine);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // TODO YOB and YTB need to do some changes for binary
        // Verify the received object list
        /*objectList = defaultYangCodecHandler.decode(sb.toString(),
                                                    XML_ENCODING,
                                                    EDIT_CONFIG_REQUEST);
        Iterator<Object> iterator = objectList.iterator();
        while (iterator.hasNext()) {
            Object object = iterator.next();
            if (object.getClass().getSimpleName().equals("CombinedOpParam")) {
                CombinedOpParam combinedOpParam = (CombinedOpParam) object;

                PathId pathId = new PathId(123);
                Origin origin = new DefaultOrigin.OriginBuilder()
                        .value(pathId).build();
                assertTrue(combinedOpParam.attributes().origin()
                                   .equals(origin));

                MultiExitDisc multiExitDisc = new DefaultMultiExitDisc
                        .MultiExitDiscBuilder().med(456).build();
                assertTrue(combinedOpParam.attributes().multiExitDisc()
                                   .equals(multiExitDisc));

                LocalPref localPref = new DefaultLocalPref.LocalPrefBuilder()
                        .pref(23).build();
                assertTrue(combinedOpParam.attributes().localPref()
                                   .equals(localPref));

                Metric metric = new Metric(456);
                AigpTlv aigpTlv = new DefaultAigpTlv.AigpTlvBuilder()
                        .metric(metric).build();
                Aigp aigp = new DefaultAigp.AigpBuilder().aigpTlv(aigpTlv)
                        .build();
                assertTrue(combinedOpParam.attributes().aigp().equals(aigp));

                UnrecognizedAttributes unrecognizedAttributes1 =
                        new DefaultUnrecognizedAttributes
                                .UnrecognizedAttributesBuilder()
                                .partial(false).transitive(false).type((short) 1)
                                .value("ABC".getBytes()).build();

                UnrecognizedAttributes unrecognizedAttributes2 =
                        new DefaultUnrecognizedAttributes
                                .UnrecognizedAttributesBuilder()
                                .partial(true).transitive(true).type((short) 2)
                                .value("BCA".getBytes()).build();

                UnrecognizedAttributes unrecognizedAttributes3 =
                        new DefaultUnrecognizedAttributes
                                .UnrecognizedAttributesBuilder()
                                .partial(true).transitive(false).type((short) 3)
                                .value("CAB".getBytes()).build();

                UnrecognizedAttributes unrecognizedAttributes4 =
                        new DefaultUnrecognizedAttributes
                                .UnrecognizedAttributesBuilder()
                                .partial(false).transitive(true).type((short) 4)
                                .value("111".getBytes()).build();

                AsNum asNum1 = new AsNum(11);
                As4BytesCapability as4BytesCapability1 =
                        new DefaultAs4BytesCapability
                                .As4BytesCapabilityBuilder()
                                .asNumber(asNum1).build();
                Cparameters cparameters1 = new DefaultCparameters
                        .CparametersBuilder()
                        .as4BytesCapability(as4BytesCapability1)
                        .build();
                OptionalCapabilities optionalCapabilities1 =
                        new DefaultOptionalCapabilities
                                .OptionalCapabilitiesBuilder()
                                .cParameters(cparameters1).build();

                AsNum asNum2 = new AsNum(22);
                As4BytesCapability as4BytesCapability2 =
                        new DefaultAs4BytesCapability
                                .As4BytesCapabilityBuilder()
                                .asNumber(asNum2).build();
                Cparameters cparameters2 = new DefaultCparameters
                        .CparametersBuilder()
                        .as4BytesCapability(as4BytesCapability2)
                        .build();
                OptionalCapabilities optionalCapabilities2 =
                        new DefaultOptionalCapabilities
                                .OptionalCapabilitiesBuilder()
                                .cParameters(cparameters2).build();

                AsNum asNum3 = new AsNum(33);
                As4BytesCapability as4BytesCapability3 =
                        new DefaultAs4BytesCapability
                                .As4BytesCapabilityBuilder()
                                .asNumber(asNum3).build();
                Cparameters cparameters3 =
                        new DefaultCparameters.CparametersBuilder()
                                .as4BytesCapability(as4BytesCapability3)
                                .build();
                OptionalCapabilities optionalCapabilities3 =
                        new DefaultOptionalCapabilities
                                .OptionalCapabilitiesBuilder()
                                .cParameters(cparameters3).build();

                BgpParameters bgpParameters1 =
                        new DefaultBgpParameters.BgpParametersBuilder()
                                .addToOptionalCapabilities(optionalCapabilities1)
                                .addToOptionalCapabilities(optionalCapabilities2)
                                .addToOptionalCapabilities(optionalCapabilities3)
                                .build();

                AsNum asNum4 = new AsNum(11);
                As4BytesCapability as4BytesCapability4 =
                        new DefaultAs4BytesCapability
                                .As4BytesCapabilityBuilder()
                                .asNumber(asNum4).build();
                Cparameters cparameters4 =
                        new DefaultCparameters.CparametersBuilder()
                                .as4BytesCapability(as4BytesCapability4)
                                .build();
                OptionalCapabilities optionalCapabilities4 =
                        new DefaultOptionalCapabilities
                                .OptionalCapabilitiesBuilder()
                                .cParameters(cparameters4).build();

                AsNum asNum5 = new AsNum(22);
                As4BytesCapability as4BytesCapability5 =
                        new DefaultAs4BytesCapability
                                .As4BytesCapabilityBuilder()
                                .asNumber(asNum5).build();
                Cparameters cparameters5 =
                        new DefaultCparameters.CparametersBuilder()
                                .as4BytesCapability(as4BytesCapability5)
                                .build();
                OptionalCapabilities optionalCapabilities5 =
                        new DefaultOptionalCapabilities
                                .OptionalCapabilitiesBuilder()
                                .cParameters(cparameters5).build();

                AsNum asNum6 = new AsNum(33);
                As4BytesCapability as4BytesCapability6 =
                        new DefaultAs4BytesCapability
                                .As4BytesCapabilityBuilder()
                                .asNumber(asNum6).build();
                Cparameters cparameters6 =
                        new DefaultCparameters.CparametersBuilder()
                                .as4BytesCapability(as4BytesCapability6)
                                .build();
                OptionalCapabilities optionalCapabilities6 =
                        new DefaultOptionalCapabilities
                                .OptionalCapabilitiesBuilder()
                                .cParameters(cparameters6).build();

                BgpParameters bgpParameters2 =
                        new DefaultBgpParameters.BgpParametersBuilder()
                                .addToOptionalCapabilities(optionalCapabilities4)
                                .addToOptionalCapabilities(optionalCapabilities5)
                                .addToOptionalCapabilities(optionalCapabilities6)
                                .build();

                Attributes attributes =
                        new DefaultAttributes.AttributesBuilder()
                                .origin(origin)
                                .multiExitDisc(multiExitDisc)
                                .localPref(localPref)
                                .aigp(aigp)
                                .addToUnrecognizedAttributes(unrecognizedAttributes1)
                                .addToUnrecognizedAttributes(unrecognizedAttributes2)
                                .addToUnrecognizedAttributes(unrecognizedAttributes3)
                                .addToUnrecognizedAttributes(unrecognizedAttributes4)
                                .addToBgpParameters(bgpParameters1)
                                .addToBgpParameters(bgpParameters2).build();
            } else {
                assertTrue(false);
            }
        }*/
    }

//    /**
//     * Unit test case in which verifying object for xml string with config as root name and
//     * operation type.
//     */
//    @Test
//    public void proceessCodecDecodeFunctionForOperTypeTest() {
//        String path = "src/test/resources/ychTestResourceFiles/configrootnameOperationType.xml";
//        testYangSchemaNodeProvider.processSchemaRegistry(null);
//        DefaultYangSchemaRegistry schemaRegistry =
//                testYangSchemaNodeProvider.getDefaultYangSchemaRegistry();
//
//        YangCodecRegistry.initializeDefaultCodec();
//        DefaultYangCodecHandler defaultYangCodecHandler =
//                new DefaultYangCodecHandler(schemaRegistry);
//
//        StringBuilder sb = new StringBuilder();
//        String sCurrentLine;
//
//        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
//
//            while ((sCurrentLine = br.readLine()) != null) {
//                sb.append(sCurrentLine);
//            }
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        // Verify the received object list
//        List<Object> objectList =
//                defaultYangCodecHandler.decode(sb.toString(),
//                                               XML, EDIT_CONFIG_REQUEST);
//        Iterator<Object> iterator = objectList.iterator();
//        while (iterator.hasNext()) {
//            Object object = iterator.next();
//            if (object.getClass().getSimpleName()
//                    .equals(LOGISTIC_MOD)) {
//                LogisticsManagerOpParam logistics =
//                        (LogisticsManagerOpParam) object;
//                DefaultPurchasingSupervisor purchasingSupervisor =
//                        (DefaultPurchasingSupervisor) logistics
//                                .purchasingSupervisor();
//
//                assertEquals(AM_OBJ + "purchase-super: operation type", DELETE,
//                             purchasingSupervisor.yangPurchasingSupervisorOpType());
//                assertEquals(AM_OBJ + "customs-super: leaf value", "abc",
//                             logistics.customsSupervisor());
//                assertEquals(AM_OBJ + "purchase-spec: leaf value", "bcd",
//                             logistics.purchasingSupervisor()
//                                     .purchasingSpecialist());
//                assertEquals(AM_OBJ + "purchase-support: leaf value",
//                             "cde", logistics.purchasingSupervisor()
//                                     .support());
//
//            } else if (object.getClass().getSimpleName()
//                    .equals(MERCHA_MOD)) {
//                MerchandisersupervisorOpParam merchandisersupervisorOpParam =
//                        (MerchandisersupervisorOpParam) object;
//                assertEquals(AM_OBJ + "merchandiser-super: leaf value",
//                             "abc", merchandisersupervisorOpParam.supervisor());
//            } else {
//                assertEquals(AM_OBJ, LOGISTIC_MOD, object
//                        .getClass().getSimpleName());
//                assertEquals(AM_OBJ, MERCHA_MOD, object
//                        .getClass().getSimpleName());
//            }
//        }
//    }

    /**
     * Validate the leaf value for purchasing specialist.
     *
     * @param objectList object list
     */
    private void processPurchasingSpecObj(List<Object> objectList) {
        Iterator<Object> iterator = objectList.iterator();
        while (iterator.hasNext()) {
            Object object = iterator.next();
            if ("LogisticsManagerOpParam".equals(object.getClass().getSimpleName())) {
                LogisticsManagerOpParam logisticsManagerOpParam =
                        (LogisticsManagerOpParam) object;
                assertEquals(AM_OBJ + "purchasing-spec: leaf value", "bcd",
                             logisticsManagerOpParam.purchasingSupervisor()
                                     .purchasingSpecialist());
            } else {
                assertEquals(AM_OBJ, "LogisticsManagerOpParam", object
                        .getClass().getSimpleName());
            }
        }
    }

    /**
     * Validate the leaf value for merchandiser supervisor.
     *
     * @param objectList object list
     */
    private void processMerchandiserObj(List<Object> objectList) {
        Iterator<Object> iterator = objectList.iterator();
        while (iterator.hasNext()) {
            Object object = iterator.next();
            if (object.getClass().getSimpleName()
                    .equals(MERCHA_MOD)) {
                MerchandisersupervisorOpParam merchandisersupervisorOpParam =
                        (MerchandisersupervisorOpParam) object;
                assertEquals(AM_OBJ + "merchandiser-super: leaf value", "abc",
                             merchandisersupervisorOpParam.supervisor());
            } else {
                assertEquals(AM_OBJ, MERCHA_MOD, object
                        .getClass().getSimpleName());
            }
        }
    }

    /**
     * Unit test case in which verifying object for xml string with get and
     * filter as root name.
     */
    @Test
    public void proceessCodecDecodeFunctionForGet() {
        String path = "src/test/resources/ychTestResourceFiles/getrootname.xml";
        testYangSchemaNodeProvider.processSchemaRegistry(null);
        DefaultYangSchemaRegistry schemaRegistry =
                testYangSchemaNodeProvider.getDefaultYangSchemaRegistry();

        YangCodecRegistry.initializeDefaultCodec();
        DefaultYangCodecHandler defaultYangCodecHandler =
                new DefaultYangCodecHandler(schemaRegistry);

        StringBuilder sb = new StringBuilder();
        String sCurrentLine;

        try (BufferedReader br = new BufferedReader(new FileReader(path))) {

            while ((sCurrentLine = br.readLine()) != null) {
                sb.append(sCurrentLine);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Verify the received object list
        List<Object> objectList =
                defaultYangCodecHandler.decode(sb.toString(),
                                               XML, QUERY_REQUEST);
        processPurchasingSpecObj(objectList);
    }

    /**
     * Unit test case in which verifying object for xml string with get-config
     * and filter as root name.
     */
    @Test
    public void proceessCodecDecodeFunctionForGetConfig() {
        String path = "src/test/resources/ychTestResourceFiles/getconfigrootname.xml";
        testYangSchemaNodeProvider.processSchemaRegistry(null);
        DefaultYangSchemaRegistry schemaRegistry =
                testYangSchemaNodeProvider.getDefaultYangSchemaRegistry();

        YangCodecRegistry.initializeDefaultCodec();
        DefaultYangCodecHandler defaultYangCodecHandler =
                new DefaultYangCodecHandler(schemaRegistry);

        StringBuilder sb = new StringBuilder();
        String sCurrentLine;

        try (BufferedReader br = new BufferedReader(new FileReader(path))) {

            while ((sCurrentLine = br.readLine()) != null) {
                sb.append(sCurrentLine);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Verify the received object list
        List<Object> objectList = defaultYangCodecHandler.decode(
                sb.toString(),
                XML, QUERY_CONFIG_REQUEST);
        processMerchandiserObj(objectList);
    }

    /**
     * Unit test case in which verifying object for xml string with data as
     * root name.
     */
    @Test
    public void proceessCodecDecodeFunctionForGetData() {
        String path = "src/test/resources/ychTestResourceFiles/getReply.xml";
        testYangSchemaNodeProvider.processSchemaRegistry(null);
        DefaultYangSchemaRegistry schemaRegistry =
                testYangSchemaNodeProvider.getDefaultYangSchemaRegistry();

        YangCodecRegistry.initializeDefaultCodec();
        DefaultYangCodecHandler defaultYangCodecHandler =
                new DefaultYangCodecHandler(schemaRegistry);

        StringBuilder sb = new StringBuilder();
        String sCurrentLine;

        try (BufferedReader br = new BufferedReader(new FileReader(path))) {

            while ((sCurrentLine = br.readLine()) != null) {
                sb.append(sCurrentLine);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Verify the received object list
        List<Object> objectList = defaultYangCodecHandler.decode(
                sb.toString(),
                XML, QUERY_CONFIG_REQUEST);
        processPurchasingSpecObj(objectList);
    }

    /**
     * Unit test case in which verifying object for xml string with rpc-reply
     * and data as root name .
     */
    @Test
    public void proceessCodecDecodeFunctionForGetConfigData() {
        String path = "src/test/resources/ychTestResourceFiles/getconfigReply.xml";
        testYangSchemaNodeProvider.processSchemaRegistry(null);
        DefaultYangSchemaRegistry schemaRegistry =
                testYangSchemaNodeProvider.getDefaultYangSchemaRegistry();

        YangCodecRegistry.initializeDefaultCodec();
        DefaultYangCodecHandler defaultYangCodecHandler =
                new DefaultYangCodecHandler(schemaRegistry);

        StringBuilder sb = new StringBuilder();
        String sCurrentLine;

        try (BufferedReader br = new BufferedReader(new FileReader(path))) {

            while ((sCurrentLine = br.readLine()) != null) {
                sb.append(sCurrentLine);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Verify the received object list
        List<Object> objectList = defaultYangCodecHandler.decode(sb.toString(),
                                                                 XML, null);
        processMerchandiserObj(objectList);
    }

    @Test
    public void proceessCodecDecodeFunctionForPresenceContainer() {
        testYangSchemaNodeProvider.processSchemaRegistry(null);
        DefaultYangSchemaRegistry schemaRegistry =
                testYangSchemaNodeProvider.getDefaultYangSchemaRegistry();

        YangCodecRegistry.initializeDefaultCodec();
        DefaultYangCodecHandler defaultYangCodecHandler =
                new DefaultYangCodecHandler(schemaRegistry);

        // Verify the received object list
        List<Object> objectList = defaultYangCodecHandler.decode(purchaseXml(),
                                                                 XML, null);
        assertNotNull(objectList);
        Iterator<Object> iterator = objectList.iterator();
        while (iterator.hasNext()) {
            Object object = iterator.next();
            if (object.getClass().getSimpleName().equals(PURCH_MOD)) {
                YchPurchasingsupervisorOpParam purchasingsupervisorOpParam =
                        (YchPurchasingsupervisorOpParam) object;
                assertEquals(AM_OBJ + "purchasing-specialist: leaf value", "purchasingSpecialist",
                             purchasingsupervisorOpParam.ychPurchasingSupervisor().ychPurchasingSpecialist());
                assertEquals(AM_OBJ + "purchasing-support: leaf value", "support",
                        purchasingsupervisorOpParam.ychPurchasingSupervisor().ychPurchasingSupport());
                assertNotNull(AM_OBJ + "purchasing-manager: leaf value",
                        purchasingsupervisorOpParam.ychPurchasingSupervisor().ychIsManager());
            } else {
                assertEquals(AM_OBJ, PURCH_MOD, object.getClass().getSimpleName());
            }
        }
    }

    @Test
    public void proceessCodecDecodeFunctionForSelectionNode() {
        testYangSchemaNodeProvider.processSchemaRegistry(null);
        DefaultYangSchemaRegistry schemaRegistry =
                testYangSchemaNodeProvider.getDefaultYangSchemaRegistry();

        YangCodecRegistry.initializeDefaultCodec();
        DefaultYangCodecHandler defaultYangCodecHandler =
                new DefaultYangCodecHandler(schemaRegistry);

        // Verify the received object list
        List<Object> objectList = defaultYangCodecHandler.decode(
                purchaseXmlEmptySelectionNode(), XML, null);
        assertNotNull(objectList);
        Iterator<Object> iterator = objectList.iterator();
        while (iterator.hasNext()) {
            Object object = iterator.next();
            if (object.getClass().getSimpleName().equals(PURCH_MOD)) {
                YchPurchasingsupervisorOpParam purchasingsupervisorOpParam =
                        (YchPurchasingsupervisorOpParam) object;
                assertNull(AM_OBJ + "purchasing-specialist: leaf value not empty",
                         purchasingsupervisorOpParam.
                         ychPurchasingSupervisor().ychPurchasingSpecialist());
            } else {
                assertEquals(AM_OBJ, PURCH_MOD, object.getClass().getSimpleName());
            }
        }
    }

    @Test
    public void proceessCodecDecodeFunctionForBitmaskContainer() {
        testYangSchemaNodeProvider.processSchemaRegistry(null);
        DefaultYangSchemaRegistry schemaRegistry =
                testYangSchemaNodeProvider.getDefaultYangSchemaRegistry();

        YangCodecRegistry.initializeDefaultCodec();
        DefaultYangCodecHandler defaultYangCodecHandler =
                new DefaultYangCodecHandler(schemaRegistry);

        BitSet purchaseOptions = new BitSet(4);
        purchaseOptions.set(1); //option1
        purchaseOptions.set(3); //option3

        // Verify the received object list
        List<Object> objectList = defaultYangCodecHandler.decode(
                purchaseXmlOptions(purchaseOptions), XML, null);
        assertNotNull(objectList);
        Iterator<Object> iterator = objectList.iterator();
        while (iterator.hasNext()) {
            Object object = iterator.next();
            if (object.getClass().getSimpleName().equals(PURCH_MOD)) {
                YchPurchasingsupervisorOpParam purchasingsupervisorOpParam =
                        (YchPurchasingsupervisorOpParam) object;
                assertEquals(AM_OBJ + "purchasing-support: leaf value", "support",
                        purchasingsupervisorOpParam.ychPurchasingSupervisor().ychPurchasingSupport());
                assertEquals(AM_OBJ + "ych-puchasing-options: leaf value",
                        purchaseOptions,
                        purchasingsupervisorOpParam.ychPurchasingSupervisor().ychPurchasingOptions());
            } else {
                assertEquals(AM_OBJ, PURCH_MOD, object.getClass().getSimpleName());
            }
        }
    }

    @Test
    public void proceessCodecDecodeFunctionForEmptyBitmask() {
        testYangSchemaNodeProvider.processSchemaRegistry(null);
        DefaultYangSchemaRegistry schemaRegistry =
                testYangSchemaNodeProvider.getDefaultYangSchemaRegistry();

        YangCodecRegistry.initializeDefaultCodec();
        DefaultYangCodecHandler defaultYangCodecHandler =
                new DefaultYangCodecHandler(schemaRegistry);

        // Verify the received object list
        List<Object> objectList = defaultYangCodecHandler.decode(
                purchaseXmlOptions(null), XML, null);
        assertNotNull(objectList);
        Iterator<Object> iterator = objectList.iterator();
        while (iterator.hasNext()) {
            Object object = iterator.next();
            if (object.getClass().getSimpleName().equals(PURCH_MOD)) {
                YchPurchasingsupervisorOpParam purchasingsupervisorOpParam =
                        (YchPurchasingsupervisorOpParam) object;
                assertEquals(AM_OBJ + "purchasing-support: leaf value", "support",
                        purchasingsupervisorOpParam.ychPurchasingSupervisor().ychPurchasingSupport());
                assertNull(AM_OBJ + "ych-puchasing-options: leaf value empty",
                        purchasingsupervisorOpParam.ychPurchasingSupervisor().ychPurchasingOptions());
            } else {
                assertEquals(AM_OBJ, PURCH_MOD, object.getClass().getSimpleName());
            }
        }
    }
}