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
package org.onosproject.iptopology.api;

import static com.google.common.base.MoreObjects.toStringHelper;

import java.util.Objects;

/**
 * Represents signaling protocols that are enabled.
 */
public class Signalling {
    private final Boolean ldp;
    private final Boolean rsvpte;

    /**
     * Constructor to initialize the values.
     *
     * @param ldp Label Distribution Protocol whether enabled or not
     * @param rsvpte RSVP TE whether enabled or not
     */
    public Signalling(Boolean ldp, Boolean rsvpte) {
        this.ldp = ldp;
        this.rsvpte = rsvpte;
    }

    /**
     * Obtains whether LDP signalling protocol is enabled or not.
     *
     * @return LDP signalling protocol is enabled or not
     */
    public Boolean ldp() {
        return ldp;
    }

    /**
     * Obtains whether rsvp-te signalling protocol is enabled or not.
     *
     * @return rsvp-te signalling protocol is enabled or not
     */
    public Boolean rsvpte() {
        return rsvpte;
    }

    @Override
    public int hashCode() {
        return Objects.hash(ldp, rsvpte);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof Signalling) {
            Signalling other = (Signalling) obj;
            return Objects.equals(ldp, other.ldp) && Objects.equals(rsvpte, other.rsvpte);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("ldp", ldp)
                .add("rsvpte", rsvpte)
                .toString();
    }
}