/*
 * Copyright 2016-present Open Networking Foundation
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
package org.onosproject.net.optical.device.port;

import java.util.Optional;

import org.onosproject.net.Port;

import com.google.common.annotations.Beta;

/**
 * Abstraction of a class capable of  translating generic-Port object
 * as another domain-specific Port of type {@code P}.
 *
 * @param <P> Port type to map generic Port to
 */
@Beta
public interface PortMapper<P extends Port> {

    /**
     * Returns true if this port is capable of being projected as {@code <P>}.
     *
     * @param port port
     * @return true if this port can be projected as the given type
     */
    boolean is(Port port);

    /**
     * Returns {@code port} mapped to {@code <P>}.
     *
     * @param port Port to map
     * @return {@code port} mapped to {@code <P>}
     */
    Optional<P> as(Port port);
}
