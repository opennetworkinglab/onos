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
package org.onosproject.mapping.codec;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.codec.impl.CodecManager;
import org.onosproject.mapping.actions.MappingAction;
import org.onosproject.mapping.actions.MappingActions;
import org.onosproject.mapping.actions.NoMappingAction;
import org.onosproject.mapping.actions.DropMappingAction;
import org.onosproject.mapping.actions.ForwardMappingAction;
import org.onosproject.mapping.actions.NativeForwardMappingAction;
import org.onosproject.mapping.MappingCodecRegistrator;

import java.io.IOException;
import java.io.InputStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.onosproject.mapping.codec.MappingActionJsonMatcher.matchesAction;

/**
 * Unit tests for MappingActionCodec.
 */
public class MappingActionCodecTest {

    private static final String NO_ACTION_STRING = "NO_ACTION";

    private CodecContext context;
    private JsonCodec<MappingAction> actionCodec;
    private MappingCodecRegistrator registrator;

    /**
     * Sets up for each test.
     * Creates a context and fetches the mapping action codec.
     */
    @Before
    public void setUp() {
        CodecManager manager = new CodecManager();
        registrator = new MappingCodecRegistrator();
        registrator.codecService = manager;
        registrator.activate();

        context = new MappingCodecContextAdapter(registrator.codecService);
        actionCodec = context.codec(MappingAction.class);
        assertThat(actionCodec, notNullValue());
    }

    /**
     * Deactivates the codec registrator.
     */
    @After
    public void tearDown() {
        registrator.deactivate();
    }

    /**
     * Tests the encoding of no mapping action.
     */
    @Test
    public void noActionTest() {
        final NoMappingAction action = MappingActions.noAction();
        final ObjectNode actionJson = actionCodec.encode(action, context);
        assertThat(actionJson, matchesAction(action));
    }

    /**
     * Tests the encoding of drop mapping action.
     */
    @Test
    public void dropActionTest() {
        final DropMappingAction action = MappingActions.drop();
        final ObjectNode actionJson = actionCodec.encode(action, context);
        assertThat(actionJson, matchesAction(action));
    }

    /**
     * Tests the encoding of forward mapping action.
     */
    @Test
    public void forwardActionTest() {
        final ForwardMappingAction action = MappingActions.forward();
        final ObjectNode actionJson = actionCodec.encode(action, context);
        assertThat(actionJson, matchesAction(action));
    }

    /**
     * Tests the encoding of native forwarding mapping action.
     */
    @Test
    public void nativeForwardActionTest() {
        final NativeForwardMappingAction action = MappingActions.nativeForward();
        final ObjectNode actionJson = actionCodec.encode(action, context);
        assertThat(actionJson, matchesAction(action));
    }

    /**
     * Tests decoding of a mapping key JSON object.
     *
     * @throws IOException if processing the resource fails
     */
    @Test
    public void testMappingActionDecode() throws IOException {
        MappingAction action = getAction("MappingAction.json");
        assertThat(action.toString(), is(NO_ACTION_STRING));
    }

    /**
     * Reads in a mapping action from the given resource and decodes it.
     *
     * @param resourceName resource to use to read the JSON for the rule
     * @return decoded mappingAction
     * @throws IOException if processing the resource fails
     */
    private MappingAction getAction(String resourceName) throws IOException {
        InputStream jsonStream = MappingActionCodecTest.class.getResourceAsStream(resourceName);
        JsonNode json = context.mapper().readTree(jsonStream);
        assertThat(json, notNullValue());
        MappingAction action = actionCodec.decode((ObjectNode) json, context);
        assertThat(action, notNullValue());
        return action;
    }
 }
