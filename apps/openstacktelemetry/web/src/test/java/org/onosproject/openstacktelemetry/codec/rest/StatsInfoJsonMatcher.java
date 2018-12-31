/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.openstacktelemetry.codec.rest;

import com.fasterxml.jackson.databind.JsonNode;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.onosproject.openstacktelemetry.api.StatsInfo;

/**
 * Hamcrest matcher for StatsInfoJsonCodec.
 */
public final class StatsInfoJsonMatcher extends TypeSafeDiagnosingMatcher<JsonNode> {

    private final StatsInfo statsInfo;

    private static final String STARTUP_TIME = "startupTime";
    private static final String FST_PKT_ARR_TIME = "fstPktArrTime";
    private static final String LST_PKT_OFFSET = "lstPktOffset";
    private static final String PREV_ACC_BYTES = "prevAccBytes";
    private static final String PREV_ACC_PKTS = "prevAccPkts";
    private static final String CURR_ACC_BYTES = "currAccBytes";
    private static final String CURR_ACC_PKTS = "currAccPkts";
    private static final String ERROR_PKTS = "errorPkts";
    private static final String DROP_PKTS = "dropPkts";

    private StatsInfoJsonMatcher(StatsInfo statsInfo) {
        this.statsInfo = statsInfo;
    }

    @Override
    protected boolean matchesSafely(JsonNode jsonNode, Description description) {

        // check startup time
        long jsonStartupTime = jsonNode.get(STARTUP_TIME).asLong();
        long startupTime = statsInfo.startupTime();
        if (jsonStartupTime != startupTime) {
            description.appendText("startup time was " + jsonStartupTime);
            return false;
        }

        // check first packet arrival time
        long jsonFstPktArrTime = jsonNode.get(FST_PKT_ARR_TIME).asLong();
        long fstPktArrTime = statsInfo.fstPktArrTime();
        if (jsonFstPktArrTime != fstPktArrTime) {
            description.appendText("first packet arrival time was " + jsonFstPktArrTime);
            return false;
        }

        // check last packet offset
        int jsonLstPktOffset = jsonNode.get(LST_PKT_OFFSET).asInt();
        int lstPktOffset = statsInfo.lstPktOffset();
        if (jsonLstPktOffset != lstPktOffset) {
            description.appendText("last packet offset was " + jsonLstPktOffset);
            return false;
        }

        // check previous accumulated bytes
        long jsonPrevAccBytes = jsonNode.get(PREV_ACC_BYTES).asLong();
        long preAccBytes = statsInfo.prevAccBytes();
        if (jsonPrevAccBytes != preAccBytes) {
            description.appendText("previous accumulated bytes was " + jsonPrevAccBytes);
            return false;
        }

        // check previous accumulated packets
        int jsonPreAccPkts = jsonNode.get(PREV_ACC_PKTS).asInt();
        int preAccPkts = statsInfo.prevAccPkts();
        if (jsonPreAccPkts != preAccPkts) {
            description.appendText("previous accumulated packets was " + jsonPreAccPkts);
            return false;
        }

        // check current accumulated bytes
        long jsonCurrAccBytes = jsonNode.get(CURR_ACC_BYTES).asLong();
        long currAccBytes = statsInfo.currAccBytes();
        if (jsonCurrAccBytes != currAccBytes) {
            description.appendText("current accumulated bytes was " + jsonCurrAccBytes);
            return false;
        }

        // check current accumulated packets
        int jsonCurrAccPkts = jsonNode.get(CURR_ACC_PKTS).asInt();
        int currAccPkts = statsInfo.currAccPkts();
        if (jsonCurrAccPkts != currAccPkts) {
            description.appendText("current accumulated packets was " + jsonCurrAccPkts);
            return false;
        }

        // check error packets
        short jsonErrorPkts = (short) jsonNode.get(ERROR_PKTS).asInt();
        short errorPkts = statsInfo.errorPkts();
        if (jsonErrorPkts != errorPkts) {
            description.appendText("error packets was " + jsonErrorPkts);
            return false;
        }

        // check drop packets
        short jsonDropPkts = (short) jsonNode.get(DROP_PKTS).asInt();
        short dropPkts = statsInfo.dropPkts();
        if (jsonDropPkts != dropPkts) {
            description.appendText("drop packets was " + jsonDropPkts);
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
    public static StatsInfoJsonMatcher matchStatsInfo(StatsInfo statsInfo) {
        return new StatsInfoJsonMatcher(statsInfo);
    }
}
