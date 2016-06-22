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
package org.onosproject.routing;

import java.util.Collection;

/**
 * A component that is able to process Forwarding Information Base (FIB) updates.
 *
 * @deprecated use RouteService instead
 */
@Deprecated
public interface FibListener {

    /**
     * Signals the FIB component of changes to the FIB.
     *
     * @param updates FIB updates of the UDPATE type
     * @param withdraws FIB updates of the WITHDRAW type
     */
    // TODO this interface should use only one collection when we have the new
    // intent key API
    void update(Collection<FibUpdate> updates, Collection<FibUpdate> withdraws);

}
