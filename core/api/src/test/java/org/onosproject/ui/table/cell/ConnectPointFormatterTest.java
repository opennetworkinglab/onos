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

package org.onosproject.ui.table.cell;

import org.junit.Test;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.ui.table.CellFormatter;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for {@link ConnectPointFormatter}.
 */
public class ConnectPointFormatterTest {

    private static final DeviceId DEVID = DeviceId.deviceId("foobar");
    private static final PortNumber PORT = PortNumber.portNumber(42);

    private static final ConnectPoint CP = new ConnectPoint(DEVID, PORT);

    private CellFormatter fmt = ConnectPointFormatter.INSTANCE;

    @Test
    public void basic() {
        assertEquals("wrong format", "foobar/42", fmt.format(CP));
    }

}
