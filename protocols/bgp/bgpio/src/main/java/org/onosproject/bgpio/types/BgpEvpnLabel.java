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

package org.onosproject.bgpio.types;

import java.util.Arrays;
import org.jboss.netty.buffer.ChannelBuffer;
import com.google.common.base.MoreObjects;

public class BgpEvpnLabel implements Comparable<BgpEvpnLabel> {

    public static final int MPLS_LABEL_LENGTH = 3;
    private byte[] mplsLabel;

    /**
     * Resets fields.
     */
    public BgpEvpnLabel() {
        this.mplsLabel = null;
    }

    /**
     * Constructor to initialize parameters.
     *
     * @param bgpEvpnLabel mpls label
     */
    public BgpEvpnLabel(byte[] bgpEvpnLabel) {
        this.mplsLabel = bgpEvpnLabel;
    }

    /**
     * Reads mpls label from channelBuffer.
     *
     * @param cb channelBuffer
     * @return object of mpls label
     */
    public static BgpEvpnLabel read(ChannelBuffer cb) {
        return new BgpEvpnLabel(cb.readBytes(3).array());
    }

    /**
     * writes mpls label into channelBuffer.
     *
     * @param cb channelBuffer
     * @return length length of written data
     */
    public int write(ChannelBuffer cb) {
        int iLenStartIndex = cb.writerIndex();
        cb.writeBytes(mplsLabel);
        return cb.writerIndex() - iLenStartIndex;
    }

    /**
     * Returns mpls label.
     *
     * @return mpls label
     */
    public byte[] getMplsLabel() {
        return this.mplsLabel;
    }

    @Override
    public int compareTo(BgpEvpnLabel mplsLabel) {
        return 0;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof BgpEvpnLabel) {

            BgpEvpnLabel that = (BgpEvpnLabel) obj;

            return Arrays.equals(this.mplsLabel, that.mplsLabel);
        }

        return false;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(mplsLabel);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("mplsLabel", mplsLabel).toString();
    }
}
