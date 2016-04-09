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
package org.onosproject.net;

import java.util.Objects;

import com.google.common.base.MoreObjects;

/*
 * Representation of NSH context header value
 */
public final class NshContextHeader {

    private final int nshContextHeader;

    /**
     * Default constructor.
     *
     * @param nshContextHeader nsh context header value.
     */
    private NshContextHeader(int nshContextHeader) {
        this.nshContextHeader = nshContextHeader;
    }

    /**
     * Returns the NshContextHeader by setting its value.
     *
     * @param nshContextHeader nsh context header value.
     * @return NshContextHeader
     */
    public static NshContextHeader of(int nshContextHeader) {
        return new NshContextHeader(nshContextHeader);
    }


    /**
     * Returns nsh context header value.
     *
     * @return the nsh context header
     */
    public int nshContextHeader() {
        return nshContextHeader;
    }


    @Override
    public int hashCode() {
        return Objects.hash(nshContextHeader);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof NshContextHeader)) {
            return false;
        }
        final NshContextHeader other = (NshContextHeader) obj;
        return   Objects.equals(this.nshContextHeader, other.nshContextHeader);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("nshContextHeader", nshContextHeader)
                .toString();
    }
}

