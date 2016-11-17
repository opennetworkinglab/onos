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

import org.onlab.util.Identifier;

/**
 * Representation of a MPLS label.
 */
public final class MplsLabel extends Identifier<Integer> {

    // An MPLS Label maximum 20 bits.
    public static final int MAX_MPLS = 0xFFFFF;

    protected MplsLabel(int value) {
        super(value);
    }

    public static MplsLabel mplsLabel(int value) {

        if (value < 0 || value > MAX_MPLS) {
            String errorMsg = "MPLS label value " + value +
                " is not in the interval [0, 0xFFFFF]";
            throw new IllegalArgumentException(errorMsg);
        }
        return new MplsLabel(value);
    }

    /**
     * Creates a MplsLabel object using the supplied decimal string.
     *
     * @param value the MPLS identifier expressed as string
     * @return Mplslabel object created from the string
     */
    public static MplsLabel mplsLabel(String value) {
        try {
            return MplsLabel.mplsLabel(Integer.parseInt(value));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public int toInt() {
        return this.id();
    }

    @Override
    public String toString() {
        return String.valueOf(this.identifier);
    }

}
