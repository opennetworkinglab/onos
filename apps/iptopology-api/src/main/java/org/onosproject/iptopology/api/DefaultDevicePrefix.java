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

import org.onosproject.net.AbstractAnnotated;
import org.onosproject.net.Annotations;
import org.onosproject.net.Element;

import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Default Device prefix implementation.
 */
public class DefaultDevicePrefix extends AbstractAnnotated implements DevicePrefix {

    private final Element element;
    private final PrefixIdentifier prefixIdentifier;
    private final PrefixTed prefixTed;

    /**
     * Creates a network device prefix attributed to the specified element.
     *
     * @param element           parent network element
     * @param prefixIdentifier  prefix identifier
     * @param prefixTed         prefid traffic engineering parameters
     * @param annotations       optional key/value annotations
     */
    public DefaultDevicePrefix(Element element, PrefixIdentifier prefixIdentifier,
                               PrefixTed prefixTed, Annotations... annotations) {
        super(annotations);
        this.element = element;
        this.prefixIdentifier = prefixIdentifier;
        this.prefixTed = prefixTed;
    }

    @Override
    public Element element() {
        return element;
    }

    @Override
    public PrefixIdentifier prefixIdentifier() {
        return prefixIdentifier;
    }

    @Override
    public PrefixTed prefixTed() {
        return prefixTed;
    }

    @Override
    public int hashCode() {
        return Objects.hash(element, prefixIdentifier, prefixTed);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof DefaultDevicePrefix) {
            final DefaultDevicePrefix other = (DefaultDevicePrefix) obj;
            return Objects.equals(this.element.id(), other.element.id()) &&
                    Objects.equals(this.prefixIdentifier, other.prefixIdentifier) &&
                    Objects.equals(this.prefixTed, other.prefixTed) &&
                    Objects.equals(this.annotations(), other.annotations());
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .omitNullValues()
                .add("element", element.id())
                .add("prefixIdentifier", prefixIdentifier)
                .add("prefixTed", prefixTed)
                .toString();
    }
}