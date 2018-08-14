/*
 * Copyright 2014-present Open Networking Foundation
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
package org.onosproject.net.provider;

import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * External identity of a {@link org.onosproject.net.provider.Provider} family.
 * It also carriers two designations of external characteristics, the URI
 * scheme and primary/ancillary indicator.
 * <p>
 * The device URI scheme is used to determine applicability of a provider to
 * operations on a specific device. The ancillary indicator serves to designate
 * a provider as a primary or ancillary.
 * </p>
 * <p>
 * A {@link org.onosproject.net.provider.ProviderRegistry} uses this designation
 * to permit only one primary provider per device URI scheme. Multiple
 * ancillary providers can register with the same device URI scheme however.
 * </p>
 */
public class ProviderId {

    /**
     * Represents no provider ID.
     */
    public static final ProviderId NONE = new ProviderId("none", "none");

    private final String scheme;
    private final String id;
    private final boolean ancillary;

    // For serialization
    private ProviderId() {
        scheme = null;
        id = null;
        ancillary = false;
    }

    /**
     * Creates a new primary provider identifier from the specified string.
     * The providers are expected to follow the reverse DNS convention, e.g.
     * {@code org.onosproject.provider.of.device}
     *
     * @param scheme device URI scheme to which this provider is bound, e.g. "of", "snmp"
     * @param id     string identifier
     */
    public ProviderId(String scheme, String id) {
        this(scheme, id, false);
    }

    /**
     * Creates a new provider identifier from the specified string.
     * The providers are expected to follow the reverse DNS convention, e.g.
     * {@code org.onosproject.provider.of.device}
     *
     * @param scheme    device URI scheme to which this provider is bound, e.g. "of", "snmp"
     * @param id        string identifier
     * @param ancillary ancillary provider indicator
     */
    public ProviderId(String scheme, String id, boolean ancillary) {
        this.scheme = checkNotNull(scheme, "Scheme cannot be null").toLowerCase();
        this.id = checkNotNull(id, "ID cannot be null");
        this.ancillary = ancillary;
    }

    /**
     * Returns the device URI scheme to which this provider is bound.
     *
     * @return device URI scheme
     */
    public String scheme() {
        return scheme;
    }

    /**
     * Returns the device URI scheme specific id portion.
     *
     * @return id
     */
    public String id() {
        return id;
    }

    /**
     * Indicates whether this identifier designates an ancillary providers.
     *
     * @return true if the provider is ancillary; false if primary
     */
    public boolean isAncillary() {
        return ancillary;
    }

    @Override
    public int hashCode() {
        return Objects.hash(scheme, id);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof ProviderId) {
            final ProviderId other = (ProviderId) obj;
            return Objects.equals(this.scheme, other.scheme) &&
                    Objects.equals(this.id, other.id) &&
                    this.ancillary == other.ancillary;
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("scheme", scheme).add("id", id)
                .add("ancillary", ancillary).toString();
    }

}
