/*
 * Copyright 2019-present Open Networking Foundation
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
package org.onosproject.openstacktelemetry.codec.json;

import com.fasterxml.jackson.databind.JsonNode;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.onosproject.openstacktelemetry.api.LinkStatsInfo;

/**
 * Hamcrest matcher for ThreeDVLinkStatsInfoJsonCodec.
 */
public final class ThreeDVLinkStatsInfoJsonMatcher
        extends TypeSafeDiagnosingMatcher<JsonNode> {

    private final LinkStatsInfo statsInfo;

    private static final String TX_PACKET = "txPacket";
    private static final String RX_PACKET = "rxPacket";
    private static final String TX_BYTE = "txByte";
    private static final String RX_BYTE = "rxByte";
    private static final String TX_DROP = "txDrop";
    private static final String RX_DROP = "rxDrop";
    private static final String TIMESTAMP = "timestamp";

    private ThreeDVLinkStatsInfoJsonMatcher(LinkStatsInfo statsInfo) {
        this.statsInfo = statsInfo;
    }

    @Override
    protected boolean matchesSafely(JsonNode jsonNode, Description description) {

        // check TX packet count
        long jsonTxPacket = jsonNode.get(TX_PACKET).asLong();
        long txPacket = statsInfo.getTxPacket();

        if (jsonTxPacket != txPacket) {
            description.appendText("TX packet was " + jsonTxPacket);
            return false;
        }

        // check RX packet count
        long jsonRxPacket = jsonNode.get(RX_PACKET).asLong();
        long rxPacket = statsInfo.getRxPacket();

        if (jsonRxPacket != rxPacket) {
            description.appendText("RX packet was " + jsonRxPacket);
            return false;
        }

        // check TX byte count
        long jsonTxByte = jsonNode.get(TX_BYTE).asLong();
        long txByte = statsInfo.getTxByte();

        if (jsonTxByte != txByte) {
            description.appendText("TX byte was " + jsonTxByte);
            return false;
        }

        // check RX byte count
        long jsonRxByte = jsonNode.get(RX_BYTE).asLong();
        long rxByte = statsInfo.getRxByte();

        if (jsonRxByte != rxByte) {
            description.appendText("RX byte was " + jsonRxByte);
            return false;
        }

        // check TX drop count
        long jsonTxDrop = jsonNode.get(TX_DROP).asLong();
        long txDrop = statsInfo.getTxDrop();

        if (jsonTxDrop != txDrop) {
            description.appendText("TX drop was " + jsonTxDrop);
            return false;
        }

        // check RX drop count
        long jsonRxDrop = jsonNode.get(RX_DROP).asLong();
        long rxDrop = statsInfo.getRxDrop();

        if (jsonRxDrop != rxDrop) {
            description.appendText("RX drop was " + jsonRxDrop);
            return false;
        }

        // check timestamp
        long jsonTimestamp = jsonNode.get(TIMESTAMP).asLong();
        long timestamp = statsInfo.getTimestamp();

        if (jsonTimestamp != timestamp) {
            description.appendText("Timestamp was " + jsonTimestamp);
            return false;
        }

        return true;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText(statsInfo.toString());
    }

    /**
     * Factory to allocate a stats info matcher.
     *
     * @param statsInfo stats info object we are looking for
     * @return matcher
     */
    public static ThreeDVLinkStatsInfoJsonMatcher matchesLinkStatsInfo(LinkStatsInfo statsInfo) {
        return new ThreeDVLinkStatsInfoJsonMatcher(statsInfo);
    }
}
