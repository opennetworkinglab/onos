/*
 * Copyright 2017-present Open Networking Laboratory
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
package org.onosproject.mapping;

import org.slf4j.Logger;

import static com.google.common.base.MoreObjects.toStringHelper;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Default implementation of MappingEntry.
 */
public class DefaultMappingEntry extends DefaultMapping implements StoredMappingEntry {

    private static final Logger log = getLogger(DefaultMappingEntry.class);

    private MappingEntryState state;

    /**
     * Creates a mapping entry specified with the mapping, state information.
     *
     * @param mapping mapping
     * @param state   mapping state
     */
    public DefaultMappingEntry(Mapping mapping, MappingEntryState state) {
        super(mapping);
        this.state = state;
    }

    /**
     * Creates a mapping entry specified with the mapping.
     *
     * @param mapping mapping
     */
    public DefaultMappingEntry(Mapping mapping) {
        this(mapping, MappingEntryState.PENDING_ADD);
    }

    @Override
    public MappingEntryState state() {
        return state;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("mapping", super.toString())
                .add("state", state)
                .toString();
    }

    @Override
    public void setState(MappingEntryState newState) {
        this.state = newState;
    }
}
