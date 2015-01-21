/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.net.flowext;

import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * Experimental extension to the flow rule subsystem; still under development.
 * Represents a generic abstraction of the service data. User app can customize whatever it needs to install on devices.
 */
public class DownStreamFlowEntry implements FlowEntryExtension {

    /**
     * temporarily only have byte stream, but it will be extract more abstract information from it later.
     */
    private final ByteBuffer payload;

    public DownStreamFlowEntry(ByteBuffer data) {
        this.payload = data;
    }

    /**
     * Get the payload of flowExtension.
     *
     * @return the byte steam value of payload.
     */
//   @Override
//   public ByteBuffer getPayload() {
    // TODO Auto-generated method stub
//       return payload;
//   }

    /**
     * Returns a hash code value for the object.
     * It use payload as parameter to hash.
     *
     * @return a hash code value for this object.
     */
    @Override
    public int hashCode() {
        return Objects.hash(payload);
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     *
     * @param obj the reference object with which to compare.
     * @return {@code true} if this object is the same as the obj
     * argument; {@code false} otherwise.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DownStreamFlowEntry) {
            DownStreamFlowEntry packet = (DownStreamFlowEntry) obj;
            return Objects.equals(this.payload, packet.payload);
        } else {
            return false;
        }
    }

    /**
     * Returns a string representation of the object.
     *
     * @return a string representation of the object.
     */
    @Override
    public String toString() {
        String obj = new String(payload.array());
        return obj;
    }
}
