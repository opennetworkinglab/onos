/*
 * Copyright 2017-present Open Networking Foundation
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
import org.hamcrest.Description;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.IpPrefix;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.codec.impl.CodecManager;
import org.onosproject.mapping.DefaultMappingKey;
import org.onosproject.mapping.MappingKey;
import org.onosproject.mapping.addresses.MappingAddress;
import org.onosproject.mapping.addresses.MappingAddresses;
import org.onosproject.mapping.MappingCodecRegistrator;

import java.io.IOException;
import java.io.InputStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Unit tests for MappingKeyCodec.
 */
public class MappingKeyCodecTest {

    private static final String IPV4_STRING = "1.2.3.4";
    private static final String PORT_STRING = "32";
    private static final IpPrefix IPV4_PREFIX =
                         IpPrefix.valueOf(IPV4_STRING + "/" + PORT_STRING);

    private CodecContext context;
    private JsonCodec<MappingKey> keyCodec;
    private MappingCodecRegistrator registrator;

    /**
     * Sets up for each test.
     * Creates a context and fetches the mapping key codec.
     */
    @Before
    public void setUp() {
        CodecManager manager = new CodecManager();
        registrator = new MappingCodecRegistrator();
        registrator.codecService = manager;
        registrator.activate();

        context = new MappingCodecContextAdapter(registrator.codecService);
        keyCodec = context.codec(MappingKey.class);
        assertThat(keyCodec, notNullValue());
    }

    /**
     * Deactivates the codec registrator.
     */
    @After
    public void tearDown() {
        registrator.deactivate();
    }

    /**
     * Tests encoding of a mapping key object.
     */
    @Test
    public void testMappingKeyEncode() {

        MappingAddress address = MappingAddresses.ipv4MappingAddress(IPV4_PREFIX);

        MappingKey key = DefaultMappingKey.builder()
                                .withAddress(address)
                                .build();

        ObjectNode keyJson = keyCodec.encode(key, context);
        assertThat(keyJson, MappingKeyJsonMatcher.matchesMappingKey(key));
    }

    /**
     * Tests decoding of a mapping key JSON object.
     */
    @Test
    public void testMappingKeyDecode() throws IOException {
        MappingKey key = getKey("MappingKey.json");
        assertThat(key.address().toString(),
                            is("IPV4:" + IPV4_STRING + "/" + PORT_STRING));
    }

    /**
     * Hamcrest matcher for mapping key.
     */
    public static final class MappingKeyJsonMatcher
            extends TypeSafeDiagnosingMatcher<JsonNode> {

        private final MappingKey mappingKey;

        /**
         * A default constructor.
         *
         * @param mappingKey mapping key
         */
        private MappingKeyJsonMatcher(MappingKey mappingKey) {
            this.mappingKey = mappingKey;
        }

        @Override
        protected boolean matchesSafely(JsonNode jsonNode, Description description) {

            // check address
            final JsonNode jsonAddressNode = jsonNode.get(MappingKeyCodec.ADDRESS);

            assertThat(jsonAddressNode,
                    MappingAddressJsonMatcher.matchesMappingAddress(mappingKey.address()));

            return true;
        }

        @Override
        public void describeTo(Description description) {
            description.appendText(mappingKey.toString());
        }

        /**
         * Factory to allocate a mapping treatment.
         *
         * @param mappingKey mapping treatment object we are looking for
         * @return matcher
         */
        static MappingKeyJsonMatcher matchesMappingKey(MappingKey mappingKey) {
            return new MappingKeyJsonMatcher(mappingKey);
        }
    }

    /**
     * Reads in a mapping key from the given resource and decodes it.
     *
     * @param resourceName resource to use to read the JSON for the rule
     * @return decoded mappingKey
     * @throws IOException if processing the resource fails
     */
    private MappingKey getKey(String resourceName) throws IOException {
        InputStream jsonStream = MappingKeyCodecTest.class.getResourceAsStream(resourceName);
        JsonNode json = context.mapper().readTree(jsonStream);
        assertThat(json, notNullValue());
        MappingKey key = keyCodec.decode((ObjectNode) json, context);
        assertThat(key, notNullValue());
        return key;
    }
}
