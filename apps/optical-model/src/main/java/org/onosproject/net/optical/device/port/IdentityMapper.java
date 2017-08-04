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
 * {@link PortMapper} which simply return given input.
 */
@Beta
public class IdentityMapper implements PortMapper<Port> {

    @Override
    public boolean is(Port port) {
        return true;
    }

    @Override
    public Optional<Port> as(Port port) {
        return Optional.of(port);
    }
}
