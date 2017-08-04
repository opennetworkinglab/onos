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
package org.onosproject.fwd;

import org.onlab.packet.MacAddress;
import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Sample reactive forwarding application.
 */
public class ReactiveForwardMetrics {
    private Long replyPacket = null;
    private Long inPacket = null;
    private Long droppedPacket = null;
    private Long forwardedPacket = null;
    private MacAddress macAddress;

    ReactiveForwardMetrics(Long replyPacket, Long inPacket, Long droppedPacket,
                           Long forwardedPacket, MacAddress macAddress) {
        this.replyPacket = replyPacket;
        this.inPacket = inPacket;
        this.droppedPacket = droppedPacket;
        this.forwardedPacket = forwardedPacket;
        this.macAddress = macAddress;
    }

    public void incremnetReplyPacket() {
        replyPacket++;

    }

    public void incrementInPacket() {
        inPacket++;
    }

    public void incrementDroppedPacket() {
        droppedPacket++;
    }

    public void incrementForwardedPacket() {
        forwardedPacket++;
    }

    public MacAddress getMacAddress() {
        return macAddress;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
            .add("inpktCounter ", inPacket)
            .add("replypktCounter ", replyPacket)
            .add("forwardpktCounter ", forwardedPacket)
            .add("droppktCounter ", droppedPacket).toString();
    }
}
