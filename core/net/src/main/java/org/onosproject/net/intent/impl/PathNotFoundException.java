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
package org.onosproject.net.intent.impl;

import com.google.common.base.MoreObjects;
import org.onosproject.net.ElementId;
import org.onosproject.net.intent.IntentException;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An exception thrown when a path is not found.
 */
public class PathNotFoundException extends IntentException {
    private static final long serialVersionUID = -2087045731049914733L;

    private final ElementId source;
    private final ElementId destination;

    public PathNotFoundException(ElementId source, ElementId destination) {
        super(String.format("No path from %s to %s", source, destination));
        this.source = checkNotNull(source);
        this.destination = checkNotNull(destination);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("source", source)
                .add("destination", destination)
                .toString();
    }
}
