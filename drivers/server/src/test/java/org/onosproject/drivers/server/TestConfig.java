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
package org.onosproject.drivers.server;

import org.onlab.packet.IpAddress;
import org.onosproject.net.DeviceId;
import org.onosproject.net.behaviour.ControllerInfo;
import org.onosproject.protocol.rest.DefaultRestSBDevice;
import org.onosproject.protocol.rest.RestSBDevice;
import java.util.Arrays;
import java.util.List;

/**
 * Common configuration for testing.
 */
public class TestConfig {

    /**
     * No need for a constructor, methods are static.
     */
    protected TestConfig() {}

    /**
     * Device information used during the tests.
     */
    public static final String REST_SCHEME = "rest";
    public static final String REST_DEV_TEST_IP_1 = "10.0.0.1";
    public static final int    REST_TEST_PORT = 80;

    public static final DeviceId REST_DEV_ID1 = DeviceId.deviceId(
        REST_SCHEME + ":" + REST_DEV_TEST_IP_1 + ":" + REST_TEST_PORT
    );
    public static final RestSBDevice REST_DEV1 = new DefaultRestSBDevice(
        IpAddress.valueOf(REST_DEV_TEST_IP_1),
        REST_TEST_PORT,
        "foo", "bar", "http", null, true
    );

    /**
     * Controller information used during the tests.
     */
    public static final String REST_CTRL_TEST_IP_1 = "10.0.0.253";
    public static final String REST_CTRL_TEST_IP_2 = "10.0.0.254";
    public static final String REST_CTRL_TEST_TYPE = "tcp";

    public static final List<ControllerInfo> CONTROLLERS = Arrays.asList(
        new ControllerInfo(
            IpAddress.valueOf(REST_CTRL_TEST_IP_1),
            REST_TEST_PORT,
            REST_CTRL_TEST_TYPE
        ),
        new ControllerInfo(
            IpAddress.valueOf(REST_CTRL_TEST_IP_2),
            REST_TEST_PORT,
            REST_CTRL_TEST_TYPE
        )
    );

}
