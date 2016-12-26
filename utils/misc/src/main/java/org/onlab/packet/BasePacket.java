/*
 * Copyright 2014-present Open Networking Laboratory
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

package org.onlab.packet;

/**
 * Base packet class.
 */
public abstract class BasePacket implements IPacket {

    protected IPacket parent;
    protected IPacket payload;

    @Override
    public IPacket getParent() {
        return this.parent;
    }

    @Override
    public IPacket setParent(final IPacket parent) {
        this.parent = parent;
        return this;
    }

    @Override
    public IPacket getPayload() {
        return this.payload;
    }

    @Override
    public IPacket setPayload(final IPacket payload) {
        this.payload = payload;
        return this;
    }

    @Override
    public void resetChecksum() {
        if (this.parent != null) {
            this.parent.resetChecksum();
        }
    }

    @Override
    public int hashCode() {
        final int prime = 6733;
        int result = 1;
        result = prime * result
                + (this.payload == null ? 0 : this.payload.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof BasePacket)) {
            return false;
        }
        final BasePacket other = (BasePacket) obj;
        if (this.payload == null) {
            if (other.payload != null) {
                return false;
            }
        } else if (!this.payload.equals(other.payload)) {
            return false;
        }
        return true;
    }

    @Override
    public Object clone() {
        IPacket pkt;
        try {
            pkt = this.getClass().newInstance();
        } catch (final Exception e) {
            throw new RuntimeException("Could not clone packet");
        }

        final byte[] data = this.serialize();
        pkt.deserialize(data, 0, data.length);
        pkt.setParent(this.parent);
        return pkt;
    }
}
