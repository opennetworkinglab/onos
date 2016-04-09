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
package org.onosproject.openflow;

import org.projectfloodlight.openflow.protocol.OFPortDesc;
import org.projectfloodlight.openflow.protocol.OFPortReason;
import org.projectfloodlight.openflow.protocol.OFPortStatus;
import org.projectfloodlight.openflow.protocol.OFType;

/**
 * Mocked open flow port status message.
 */
public class MockOfPortStatus extends OfMessageAdapter implements OFPortStatus {
    public MockOfPortStatus() {
        super(OFType.PORT_STATUS);
    }

    @Override
    public OFPortReason getReason() {
        return null;
    }

    @Override
    public OFPortDesc getDesc() {
        return null;
    }

    @Override
    public OFPortStatus.Builder createBuilder() {
        return null;
    }
}
