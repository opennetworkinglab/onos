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
 *
 */

package org.onosproject.ui;

import java.util.Collection;

/**
 * Abstraction of an entity capable of producing one or more topology-2
 * overlay handlers specific to a given user interface connection.
 */
public interface UiTopo2OverlayFactory {

    /**
     * Produces a collection of new overlay handlers for topology-2 view.
     *
     * @return collection of new overlay handlers
     */
    Collection<UiTopo2Overlay> newOverlays();
}
