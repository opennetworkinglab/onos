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
import org.onosproject.net.meter.Meter;

/**
 * Hamcrest matcher for meters.
 */
public final class MeterJsonMatcher extends TypeSafeDiagnosingMatcher<JsonNode> {

    private final Meter meter;

    private MeterJsonMatcher(Meter meter) {
        this.meter = meter;
    }

    @Override
    protected boolean matchesSafely(JsonNode jsonMeter, Description description) {
        // check id
        String jsonMeterId = jsonMeter.get("id").asText();
        String meterId = meter.id().toString();
        if (!jsonMeterId.equals(meterId)) {
            description.appendText("meter id was " + jsonMeterId);
            return false;
        }

        // check unit
        String jsonUnit = jsonMeter.get("unit").asText();
        String unit = meter.unit().toString();
        if (!jsonUnit.equals(unit)) {
            description.appendText("unit was " + jsonUnit);
            return false;
        }

        // check burst
        boolean jsonBurst = jsonMeter.get("burst").asBoolean();
        boolean burst = meter.isBurst();
        if (jsonBurst != burst) {
            description.appendText("isBurst was " + jsonBurst);
            return false;
        }

        // check state
        JsonNode jsonNodeState = jsonMeter.get("state");
        if (jsonNodeState != null) {
            String state = meter.state().toString();
            if (!jsonNodeState.asText().equals(state)) {
                description.appendText("state was " + jsonNodeState.asText());
                return false;
            }
        }

        // check life
        JsonNode jsonNodeLife = jsonMeter.get("life");
        if (jsonNodeLife != null) {
            long life = meter.life();
            if (jsonNodeLife.asLong() != life) {
                description.appendText("life was " + jsonNodeLife.asLong());
                return false;
            }
        }

        // check bytes
        JsonNode jsonNodeBytes = jsonMeter.get("bytes");
        if (jsonNodeBytes != null) {
            long bytes = meter.bytesSeen();
            if (jsonNodeBytes.asLong() != bytes) {
                description.appendText("bytes was " + jsonNodeBytes.asLong());
                return false;
            }
        }

        // check packets
        JsonNode jsonNodePackets = jsonMeter.get("packets");
        if (jsonNodePackets != null) {
            long packets = meter.packetsSeen();
            if (jsonNodePackets.asLong() != packets) {
                description.appendText("packets was " + jsonNodePackets.asLong());
                return false;
            }
        }

        // check size of band array
        JsonNode jsonBands = jsonMeter.get("bands");
        if (jsonBands.size() != meter.bands().size()) {
            description.appendText("bands size was " + jsonBands.size());
            return false;
        }

        // check bands
        for (Band band : meter.bands()) {
            boolean bandFound = false;
            for (int bandIndex = 0; bandIndex < jsonBands.size(); bandIndex++) {
                MeterBandJsonMatcher bandMatcher = MeterBandJsonMatcher.matchesMeterBand(band);
                if (bandMatcher.matches(jsonBands.get(bandIndex))) {
                    bandFound = true;
                    break;
                }
            }
            if (!bandFound) {
                description.appendText("band not found " + band.toString());
                return false;
            }
        }

        return true;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText(meter.toString());
    }

    /**
     * Factory to allocate a meter matcher.
     *
     * @param meter meter object we are looking for
     * @return matcher
     */
    public static MeterJsonMatcher matchesMeter(Meter meter) {
        return new MeterJsonMatcher(meter);
    }
}
