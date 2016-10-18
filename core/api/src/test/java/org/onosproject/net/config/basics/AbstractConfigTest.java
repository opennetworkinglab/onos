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

package org.onosproject.net.config.basics;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.net.DeviceId;
import org.onosproject.net.config.ConfigApplyDelegate;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.fail;

/**
 * Abstract superclass for config tests.
 */
abstract class AbstractConfigTest {

    private static final String D_PREFIX = "of:00000000000000";

    static final String BASIC = "basic";
    static final String JSON_LOADED = "%nJSON loaded: %s";
    static final String CHECKING_S = "   checking: %s";

    // Shared object mapper.
    final ObjectMapper mapper = new ObjectMapper();

    // Shared null-delegate.
    final ConfigApplyDelegate delegate = config -> {
    };

    /**
     * Prints the given format string with parameter replacement to stdout.
     *
     * @param fmt    format string
     * @param params parameters
     * @see String#format(String, Object...)
     */
    static void print(String fmt, Object... params) {
        System.out.println(String.format(fmt, params));
    }

    /**
     * Prints the given object's string representation to stdout.
     *
     * @param o the object to print
     */
    static void print(Object o) {
        print("%s", o);
    }

    /**
     * Reads in and parses the specified JSON file, returning a JSON node
     * representation of the data. Note that if an error occurs while
     * attempting to read the file, {@link org.junit.Assert#fail()} will be
     * called.
     *
     * @param path JSON file path
     * @return data represented as a JSON node
     */
    JsonNode getTestJson(String path) {
        try {
            InputStream is = AbstractConfigTest.class.getResourceAsStream(path);
            return mapper.readTree(is);

        } catch (IOException e) {
            fail("Could not read json from: " + path + ": " + e.getMessage());
        }
        return null;
    }

    /**
     * Returns a device identifier from a given string suffix.
     *
     * @param suffix two character suffix
     * @return device identifier
     */
    static DeviceId dstr(String suffix) {
        return DeviceId.deviceId(D_PREFIX + suffix);
    }

    /**
     * Utility class to build quick JSON structures.
     */
    final class TmpJson {

        final ObjectNode root = mapper.createObjectNode();

        TmpJson props(String... keys) {
            for (String k : keys) {
                root.put(k, k);
            }
            return this;
        }

        TmpJson arrays(String... keys) {
            for (String k : keys) {
                root.set(k, mapper.createArrayNode());
            }
            return this;
        }

        TmpJson objects(String... keys) {
            for (String k : keys) {
                root.set(k, mapper.createObjectNode());
            }
            return this;
        }

        ObjectNode node() {
            return root;
        }
    }
}
