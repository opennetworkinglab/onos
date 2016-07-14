/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.codec.impl;

import com.fasterxml.jackson.databind.JsonNode;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.onosproject.net.meter.Band;

/**
 * Hamcrest matcher for bands.
 */
public final class MeterBandJsonMatcher extends TypeSafeDiagnosingMatcher<JsonNode> {

    private final Band band;

    private MeterBandJsonMatcher(Band band) {
        this.band = band;
    }

    /**
     * Matches the contents of a meter band.
     *
     * @param bandJson JSON representation of band to match
     * @param description Description object used for recording errors
     * @return true if contents match, false otherwise
     */
    @Override
    protected boolean matchesSafely(JsonNode bandJson, Description description) {
        // check type
        final String jsonType = bandJson.get("type").textValue();
        if (!band.type().name().equals(jsonType)) {
            description.appendText("type was " + jsonType);
            return false;
        }

        // check rate
        final long jsonRate = bandJson.get("rate").longValue();
        if (band.rate() != jsonRate) {
            description.appendText("rate was " + jsonRate);
            return false;
        }

        // check burst size
        final long jsonBurstSize = bandJson.get("burstSize").longValue();
        if (band.burst() != jsonBurstSize) {
            description.appendText("burst size was " + jsonBurstSize);
            return false;
        }

        // check precedence
        final JsonNode jsonNodePrec = bandJson.get("prec");
        if (jsonNodePrec != null) {
            if (band.dropPrecedence() != jsonNodePrec.shortValue()) {
                description.appendText("drop precedence was " + jsonNodePrec.shortValue());
                return false;
            }
        }

        // check packets
        final JsonNode jsonNodePackets = bandJson.get("packets");
        if (jsonNodePackets != null) {
            if (band.packets() != jsonNodePackets.asLong()) {
                description.appendText("packets was " + jsonNodePackets.asLong());
                return false;
            }
        }

        final JsonNode jsonNodeBytes = bandJson.get("bytes");
        if (jsonNodeBytes != null) {
            if (band.bytes() != jsonNodeBytes.asLong()) {
                description.appendText("bytes was " + jsonNodeBytes.asLong());
                return false;
            }
        }

        return true;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText(band.toString());
    }

    /**
     * Factory to allocate a band matcher.
     *
     * @param band band object we are looking for
     * @return matcher
     */
    public static MeterBandJsonMatcher matchesMeterBand(Band band) {
        return new MeterBandJsonMatcher(band);
    }
}
