/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.store.service;

/**
 * Distributed primitive revision types.
 * <p>
 * Revision types dictate the semantics of new revisions of a distributed primitive. They indicate how the new
 * revision is initialized and how it relates to prior revisions.
 */
public enum RevisionType {

    /**
     * A distributed primitive that is initialized with an empty state.
     */
    NONE,

    /**
     * A distributed primitive that is initialized from the previous revision and sets the previous revision to
     * read-only mode.
     */
    VERSION,

    /**
     * A distributed primitive that is initialized from the state of the previous revision and to which changes to
     * the previous revision are propagated.
     */
    PROPAGATE,

    /**
     * A distributed primitive that is initialized from the state of the previous revision and thereafter diverges.
     */
    ISOLATE,

}
