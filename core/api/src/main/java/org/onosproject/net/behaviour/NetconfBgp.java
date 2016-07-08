/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.net.behaviour;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Objects;

/**
 * Represent the object for the xml element of bgp.
 */
public class NetconfBgp {
    private final String contentVersion;
    private final String formatVersion;
    private final NetconfBgpcomm bgpcomm;

    /**
     * NetconfBgp constructor.
     * 
     * @param contentVersion content version
     * @param formatVersion format version
     * @param bgpcomm NetconfBgpcomm
     */
    public NetconfBgp(String contentVersion, String formatVersion,
                      NetconfBgpcomm bgpcomm) {
        checkNotNull(contentVersion, "contentVersion cannot be null");
        checkNotNull(formatVersion, "formatVersion cannot be null");
        checkNotNull(bgpcomm, "bgpcomm cannot be null");
        this.contentVersion = contentVersion;
        this.formatVersion = formatVersion;
        this.bgpcomm = bgpcomm;
    }

    /**
     * Returns contentVersion.
     * 
     * @return contentVersion
     */
    public String contentVersion() {
        return contentVersion;
    }

    /**
     * Returns formatVersion.
     * 
     * @return formatVersion
     */
    public String formatVersion() {
        return formatVersion;
    }

    /**
     * Returns bgpcomm.
     * 
     * @return bgpcomm
     */
    public NetconfBgpcomm bgpcomm() {
        return bgpcomm;
    }

    @Override
    public int hashCode() {
        return Objects.hash(contentVersion, formatVersion, bgpcomm);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof NetconfBgp) {
            final NetconfBgp other = (NetconfBgp) obj;
            return Objects.equals(this.contentVersion, other.contentVersion)
                    && Objects.equals(this.formatVersion, other.formatVersion)
                    && Objects.equals(this.bgpcomm, other.bgpcomm);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("contentVersion", contentVersion)
                .add("formatVersion", formatVersion).add("bgpcomm", bgpcomm)
                .toString();
    }
}
