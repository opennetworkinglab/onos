/*
 * Copyright 2015 Open Networking Laboratory
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
 *
 */

package org.onosproject.cord.gui.model;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Default XOS function implementation, that does not have any parameters
 * to tweak.
 */
public class DefaultXosFunction implements XosFunction {

    protected static final ObjectMapper MAPPER = new ObjectMapper();

    private final XosFunctionDescriptor xfd;

    public DefaultXosFunction(XosFunctionDescriptor xfd) {
        this.xfd = xfd;
    }

    public XosFunctionDescriptor descriptor() {
        return xfd;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This default implementation throws an exception.
     *
     * @param user user to apply the change to
     * @param param parameter name
     * @param value new parameter value
     * @throws UnsupportedOperationException if invoked
     */
    public void applyParam(SubscriberUser user, String param, String value) {
        throw new UnsupportedOperationException();
    }

    public Memento createMemento() {
        return null;
    }

    public String xosUrlApply(SubscriberUser user) {
        return null;
    }

    @Override
    public String toString() {
        return "{XosFunction: " + xfd + "}";
    }
}
