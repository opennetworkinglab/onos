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

import org.hamcrest.Description;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.onosproject.net.ConnectPoint;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Hamcrest matcher for connect points.
 */

public final class ConnectPointJsonMatcher extends TypeSafeDiagnosingMatcher<JsonNode> {

    private final ConnectPoint connectPoint;

    private ConnectPointJsonMatcher(ConnectPoint connectPointValue) {
        connectPoint = connectPointValue;
    }

    @Override
    public boolean matchesSafely(JsonNode jsonConnectPoint, Description description) {
        // check device
        final String jsonDevice = jsonConnectPoint.get("device").asText();
        final String device = connectPoint.deviceId().toString();
        if (!jsonDevice.equals(device)) {
            description.appendText("device was " + jsonDevice);
            return false;
        }

        // check port
        final String jsonPort = jsonConnectPoint.get("port").asText();
        final String port = connectPoint.port().toString();
        if (!jsonPort.equals(port)) {
            description.appendText("port was " + jsonPort);
            return false;
        }

        return true;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText(connectPoint.toString());
    }

    /**
     * Factory to allocate an connect point matcher.
     *
     * @param connectPoint connect point object we are looking for
     * @return matcher
     */
    public static ConnectPointJsonMatcher matchesConnectPoint(ConnectPoint connectPoint) {
        return new ConnectPointJsonMatcher(connectPoint);
    }
}
