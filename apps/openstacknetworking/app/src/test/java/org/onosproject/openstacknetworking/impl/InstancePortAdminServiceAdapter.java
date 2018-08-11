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
package org.onosproject.openstacknetworking.impl;

import org.onosproject.openstacknetworking.api.InstancePort;
import org.onosproject.openstacknetworking.api.InstancePortAdminService;

/**
 * Test adapter for instance port admin service.
 */
public class InstancePortAdminServiceAdapter
        extends InstancePortServiceAdapter implements InstancePortAdminService {
    @Override
    public void createInstancePort(InstancePort instancePort) {

    }

    @Override
    public void updateInstancePort(InstancePort instancePort) {

    }

    @Override
    public void removeInstancePort(String portId) {

    }

    @Override
    public void clear() {

    }
}
