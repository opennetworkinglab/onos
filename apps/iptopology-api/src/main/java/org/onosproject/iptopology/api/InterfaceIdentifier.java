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
 * This class provides information on Local Interface Identifier and Remote
 * Interface Identifier of the link.
 */
public class InterfaceIdentifier {
    private final Integer identifier;

    /**
     * Constructor to initialize identifier.
     *
     * @param identifier local/remote interface identifier
     */
    public InterfaceIdentifier(Integer identifier) {
        this.identifier = identifier;
    }

    /**
     * Provides the local/remote interface identifier of the link.
     *
     * @return interface identifier
     */
    public Integer identifier() {
        return identifier;
    }

    @Override
    public int hashCode() {
        return Objects.hash(identifier);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof InterfaceIdentifier) {
            InterfaceIdentifier other = (InterfaceIdentifier) obj;
            return Objects.equals(identifier, other.identifier);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("identifier", identifier)
                .toString();
    }
}
