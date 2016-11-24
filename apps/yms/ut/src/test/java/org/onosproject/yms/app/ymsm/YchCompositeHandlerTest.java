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

package org.onosproject.yms.app.ymsm;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.onosproject.yang.gen.v1.ydt.customs.supervisor.rev20160524.CustomssupervisorOpParam;
import org.onosproject.yms.app.ych.defaultcodecs.YangCodecRegistry;
import org.onosproject.yms.app.ysr.TestYangSchemaNodeProvider;
import org.onosproject.yms.ych.YangCodecHandler;
import org.onosproject.yms.ych.YangCompositeEncoding;
import org.onosproject.yms.ych.YangDataTreeCodec;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static junit.framework.TestCase.assertNotNull;
import static org.onosproject.yms.ych.YangProtocolEncodingFormat.XML;

/**
 * Unit test case for YCH composite codec handler.
 */
public class YchCompositeHandlerTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private static final String REG_PREFIX = "RegisteredDataTreeCodec ";
    private static final String REG_ENCODE = REG_PREFIX +
            "encodeYdtToProtocolFormat Called.";
    private static final String REG_DECODE = REG_PREFIX +
            "decodeProtocolDataToYdt Called.";
    private static final String REG_COMPO_ENCODE = REG_PREFIX +
            "encodeYdtToCompositeProtocolFormat Called.";
    private static final String REG_COMPO_DECODE = REG_PREFIX +
            "decodeCompositeProtocolDataToYdt Called.";
    private static final String OVERRIDE_PREFIX = "OverriddenDataTreeCodec ";
    private static final String OVERRIDE_ENCODE = OVERRIDE_PREFIX +
            "encodeYdtToProtocolFormat Called.";
    private static final String OVERRIDE_DECODE = OVERRIDE_PREFIX +
            "decodeProtocolDataToYdt Called.";
    private static final String OVERRIDE_COMPO_ENCODE = OVERRIDE_PREFIX +
            "encodeYdtToCompositeProtocolFormat Called.";
    private static final String OVERRIDE_COMPO_DECODE = OVERRIDE_PREFIX +
            "decodeCompositeProtocolDataToYdt Called.";

    private TestYangSchemaNodeProvider provider =
            new TestYangSchemaNodeProvider();

    /**
     * Unit test case in which verifying codec handler is null or not.
     */
    @Test
    public void checkForCodecHandler() {
        YmsManager ymsManager = new YmsManager();
        ymsManager.coreService = new MockCoreService();
        ymsManager.activate();
        YangCodecHandler yangCodecHandler = ymsManager.getYangCodecHandler();
        assertNotNull("Codec handler is null", yangCodecHandler);
    }

    /**
     * Unit test case in which verifying registered codec handler for encode is
     * null or not.
     */
    @Test
    public void checkForRegisterDefaultCodecEncode() {
        thrown.expectMessage(REG_ENCODE);
        YangDataTreeCodec yangDataTreeCodec = new MockRegisteredDataTreeCodec();
        YmsManager ymsManager = new YmsManager();
        YangCodecRegistry.initializeDefaultCodec();
        ymsManager.coreService = new MockCoreService();
        ymsManager.activate();
        ymsManager.registerDefaultCodec(yangDataTreeCodec, XML);
        YangCodecHandler yangCodecHandler = ymsManager.getYangCodecHandler();
        assertNotNull("Codec handler is null", yangCodecHandler);

        provider.processSchemaRegistry(null);
        List<Object> yangModuleList = new ArrayList<>();

        // Creating the object
        Object object = CustomssupervisorOpParam.builder()
                .supervisor("Customssupervisor").build();
        yangModuleList.add(object);

        // Get the xml string and compare
        Map<String, String> tagAttr = new HashMap<String, String>();
        tagAttr.put("type", "subtree");

        yangCodecHandler.encodeOperation("filter", "ydt.filter-type",
                                         tagAttr, yangModuleList,
                                         XML, null);
    }

    /**
     * Unit test case in which verifying registered codec handler for decode is
     * null or not.
     */
    @Test
    public void checkForRegisterDefaultCodecDecode() {
        thrown.expectMessage(REG_DECODE);
        YangDataTreeCodec yangDataTreeCodec = new MockRegisteredDataTreeCodec();
        YmsManager ymsManager = new YmsManager();
        YangCodecRegistry.initializeDefaultCodec();
        ymsManager.coreService = new MockCoreService();
        ymsManager.activate();
        ymsManager.registerDefaultCodec(yangDataTreeCodec, XML);
        YangCodecHandler yangCodecHandler = ymsManager.getYangCodecHandler();
        assertNotNull("Codec handler is null", yangCodecHandler);

        provider.processSchemaRegistry(null);
        yangCodecHandler.decode("XML String", XML, null);
    }

    /**
     * Unit test case in which verifying registered codec handler for
     * composite encode is null or not.
     */
    @Test
    public void checkForRegisterDefaultCodecCompEncode() {
        thrown.expectMessage(REG_COMPO_ENCODE);
        YangDataTreeCodec yangDataTreeCodec = new MockRegisteredDataTreeCodec();
        YmsManager ymsManager = new YmsManager();
        YangCodecRegistry.initializeDefaultCodec();
        ymsManager.coreService = new MockCoreService();
        ymsManager.activate();
        ymsManager.registerDefaultCodec(yangDataTreeCodec, XML);
        YangCodecHandler yangCodecHandler = ymsManager.getYangCodecHandler();
        assertNotNull("Codec handler is null", yangCodecHandler);

        provider.processSchemaRegistry(null);
        // Creating the object
        Object object = CustomssupervisorOpParam.builder()
                .supervisor("Customssupervisor").build();

        yangCodecHandler.encodeCompositeOperation("filter",
                                                  "ydt.filter-type", object,
                                                  XML, null);
    }

    /**
     * Unit test case in which verifying registered codec handler for
     * composite decode is null or not.
     */
    @Test
    public void checkForRegisterDefaultCodecCompDecode() {
        thrown.expectMessage(REG_COMPO_DECODE);
        YangDataTreeCodec yangDataTreeCodec = new MockRegisteredDataTreeCodec();
        YmsManager ymsManager = new YmsManager();
        YangCodecRegistry.initializeDefaultCodec();
        ymsManager.coreService = new MockCoreService();
        ymsManager.activate();
        ymsManager.registerDefaultCodec(yangDataTreeCodec, XML);
        YangCodecHandler yangCodecHandler = ymsManager.getYangCodecHandler();
        assertNotNull("Codec handler is null", yangCodecHandler);

        provider.processSchemaRegistry(null);
        // Creating the object
        YangCompositeEncoding yangCompositeEncoding =
                new MockYangCompositeEncoding();
        yangCodecHandler.decode(yangCompositeEncoding, XML, null);
    }

    /**
     * Unit test case in which verifying overridden codec handler for encode is
     * null or not.
     */
    @Test
    public void checkForOverriddenDataTreeCodecEncode() {
        thrown.expectMessage(OVERRIDE_ENCODE);
        YangDataTreeCodec yangDataTreeCodec = new MockRegisteredDataTreeCodec();
        YmsManager ymsManager = new YmsManager();
        ymsManager.coreService = new MockCoreService();
        ymsManager.activate();
        ymsManager.registerDefaultCodec(yangDataTreeCodec, XML);
        YangCodecHandler yangCodecHandler = ymsManager.getYangCodecHandler();
        assertNotNull("Codec handler is null", yangCodecHandler);

        YangDataTreeCodec overriddenCodec = new MockOverriddenDataTreeCodec();
        yangCodecHandler.registerOverriddenCodec(overriddenCodec, XML);

        provider.processSchemaRegistry(null);
        List<Object> yangModuleList = new ArrayList<>();

        // Creating the object
        Object object = CustomssupervisorOpParam.builder()
                .supervisor("Customssupervisor").build();
        yangModuleList.add(object);

        // Get the xml string and compare
        Map<String, String> tagAttr = new HashMap<String, String>();
        tagAttr.put("type", "subtree");
        yangCodecHandler.encodeOperation("filter", "ydt.filter-type",
                                         tagAttr, yangModuleList,
                                         XML, null);
    }

    /**
     * Unit test case in which verifying overridden codec handler for decode is
     * null or not.
     */
    @Test
    public void checkForOverriddenDataTreeCodecDecode() {
        thrown.expectMessage(OVERRIDE_DECODE);
        YangDataTreeCodec yangDataTreeCodec = new MockRegisteredDataTreeCodec();
        YmsManager ymsManager = new YmsManager();
        ymsManager.coreService = new MockCoreService();
        ymsManager.activate();
        ymsManager.registerDefaultCodec(yangDataTreeCodec, XML);
        YangCodecHandler yangCodecHandler = ymsManager.getYangCodecHandler();
        assertNotNull("Codec handler is null", yangCodecHandler);

        YangDataTreeCodec overriddenCodec = new MockOverriddenDataTreeCodec();
        yangCodecHandler.registerOverriddenCodec(overriddenCodec, XML);

        provider.processSchemaRegistry(null);
        yangCodecHandler.decode("XML String", XML, null);
    }

    /**
     * Unit test case in which verifying overridden codec handler for
     * composite encode is null or not.
     */
    @Test
    public void checkForOverriddenDataTreeCodecCompoEncode() {
        thrown.expectMessage(OVERRIDE_COMPO_ENCODE);
        YangDataTreeCodec yangDataTreeCodec = new MockRegisteredDataTreeCodec();
        YmsManager ymsManager = new YmsManager();
        ymsManager.coreService = new MockCoreService();
        ymsManager.activate();
        ymsManager.registerDefaultCodec(yangDataTreeCodec, XML);
        YangCodecHandler yangCodecHandler = ymsManager.getYangCodecHandler();
        assertNotNull("Codec handler is null", yangCodecHandler);

        YangDataTreeCodec overriddenCodec = new MockOverriddenDataTreeCodec();
        yangCodecHandler.registerOverriddenCodec(overriddenCodec, XML);

        provider.processSchemaRegistry(null);
        // Creating the object
        Object object = CustomssupervisorOpParam.builder()
                .supervisor("Customssupervisor").build();
        yangCodecHandler.encodeCompositeOperation("filter",
                                                  "ydt.filter-type",
                                                  object,
                                                  XML, null);
    }

    /**
     * Unit test case in which verifying overridden codec handler for
     * composite decode is null or not.
     */
    @Test
    public void checkForOverriddenDataTreeCodecCompoDecode() {
        thrown.expectMessage(OVERRIDE_COMPO_DECODE);
        YangDataTreeCodec yangDataTreeCodec = new MockRegisteredDataTreeCodec();
        YmsManager ymsManager = new YmsManager();
        ymsManager.coreService = new MockCoreService();
        ymsManager.activate();
        ymsManager.registerDefaultCodec(yangDataTreeCodec, XML);
        YangCodecHandler yangCodecHandler = ymsManager.getYangCodecHandler();
        assertNotNull("Codec handler is null", yangCodecHandler);

        YangDataTreeCodec overriddenCodec = new MockOverriddenDataTreeCodec();
        yangCodecHandler.registerOverriddenCodec(overriddenCodec, XML);

        provider.processSchemaRegistry(null);
        // Verify the received object list
        YangCompositeEncoding yangCompositeEncoding =
                new MockYangCompositeEncoding();
        yangCodecHandler.decode(yangCompositeEncoding, XML, null);
    }
}
