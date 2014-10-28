/*
 * Copyright 2014 Open Networking Laboratory
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
package org.onlab.onos.net;

import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Default port implementation.
 */
public class DefaultPort extends AbstractAnnotated implements Port {

    private final Element element;
    private final PortNumber number;
    private final boolean isEnabled;

    /**
     * Creates a network element attributed to the specified provider.
     *
     * @param element     parent network element
     * @param number      port number
     * @param isEnabled   indicator whether the port is up and active
     * @param annotations optional key/value annotations
     */
    public DefaultPort(Element element, PortNumber number,
                       boolean isEnabled, Annotations... annotations) {
        super(annotations);
        this.element = element;
        this.number = number;
        this.isEnabled = isEnabled;
    }

    @Override
    public int hashCode() {
        return Objects.hash(number, isEnabled);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof DefaultPort) {
            final DefaultPort other = (DefaultPort) obj;
            return Objects.equals(this.element.id(), other.element.id()) &&
                    Objects.equals(this.number, other.number) &&
                    Objects.equals(this.isEnabled, other.isEnabled);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("element", element.id())
                .add("number", number)
                .add("isEnabled", isEnabled)
                .toString();
    }

    @Override
    public PortNumber number() {
        return number;
    }

    @Override
    public boolean isEnabled() {
        return isEnabled;
    }

    @Override
    public Element element() {
        return element;
    }

}
