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
package org.onosproject.drivers.lisp.extensions.codec;

import com.fasterxml.jackson.databind.JsonNode;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.onosproject.drivers.lisp.extensions.LispTeAddress;
import org.onosproject.mapping.codec.MappingAddressJsonMatcher;

/**
 * Hamcrest matcher for TeRecord.
 */
public final class LispTeRecordJsonMatcher
        extends TypeSafeDiagnosingMatcher<JsonNode> {

    private final LispTeAddress.TeRecord record;

    /**
     * Default constructor.
     *
     * @param record TeRecord object
     */
    private LispTeRecordJsonMatcher(LispTeAddress.TeRecord record) {
        this.record = record;
    }

    @Override
    protected boolean matchesSafely(JsonNode jsonNode, Description description) {

        // check isLookup
        boolean jsonLookup = jsonNode.get(LispTeRecordCodec.LOOKUP).asBoolean();
        boolean lookup = record.isLookup();
        if (jsonLookup != lookup) {
            description.appendText("IsLookup was " + jsonLookup);
            return false;
        }

        // check isRlocProbe
        boolean jsonRlocProbe = jsonNode.get(LispTeRecordCodec.RLOC_PROBE).asBoolean();
        boolean rlocProbe = record.isRlocProbe();
        if (jsonRlocProbe != rlocProbe) {
            description.appendText("IsRlocProbe was " + jsonRlocProbe);
            return false;
        }

        // check isStrict
        boolean jsonStrict = jsonNode.get(LispTeRecordCodec.STRICT).asBoolean();
        boolean strict = record.isStrict();
        if (jsonStrict != strict) {
            description.appendText("IsStrict was " + jsonStrict);
            return false;
        }

        // check address
        MappingAddressJsonMatcher addressMatcher =
                MappingAddressJsonMatcher.matchesMappingAddress(record.getAddress());

        return addressMatcher.matches(jsonNode.get(LispTeRecordCodec.ADDRESS));
    }

    @Override
    public void describeTo(Description description) {
        description.appendText(record.toString());
    }

    /**
     * Factory to allocate a TeRecord matcher.
     *
     * @param record TeRecord object we are looking for
     * @return matcher
     */
    public static LispTeRecordJsonMatcher matchesTeRecord(LispTeAddress.TeRecord record) {
        return new LispTeRecordJsonMatcher(record);
    }
}
