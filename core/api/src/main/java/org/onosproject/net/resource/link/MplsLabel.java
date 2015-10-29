/*
 * Copyright 2014-2015 Open Networking Laboratory
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

package org.onosproject.net.resource.link;

import java.util.Objects;

/**
 * Representation of MPLS label resource.
 *
 * @deprecated in Emu Release
 */
@Deprecated
public final class MplsLabel implements LinkResource {

    private final org.onlab.packet.MplsLabel mplsLabel;


    /**
     * Creates a new instance with given MPLS label.
     *
     * @param mplsLabel MPLS Label value to be assigned
     */
    public MplsLabel(int mplsLabel) {
        this.mplsLabel =  org.onlab.packet.MplsLabel.mplsLabel(mplsLabel);
    }

    /**
     * Creates a new instance with given MPLS label.
     *
     * @param mplsLabel mplsLabel value to be assigned
     * @return {@link MplsLabel} instance with given bandwidth
     */
    public static MplsLabel valueOf(int mplsLabel) {
        return new MplsLabel(mplsLabel);
    }

    /**
     * Returns MPLS Label as an MPLS Label Object.
     *
     * @return MPLS label as an MPLS Label Object.
     */
    public org.onlab.packet.MplsLabel label() {
        return mplsLabel;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof MplsLabel) {
            MplsLabel that = (MplsLabel) obj;
            return Objects.equals(this.mplsLabel, that.mplsLabel);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.mplsLabel);
    }

    @Override
    public String toString() {
        return String.valueOf(this.mplsLabel);
    }
}
