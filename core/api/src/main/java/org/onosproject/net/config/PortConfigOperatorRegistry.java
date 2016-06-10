/*
 * Copyright 2016-present Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.onosproject.net.config;

import org.onosproject.net.ConnectPoint;

import com.google.common.annotations.Beta;

/**
 * Abstraction of a port operator registry.
 */
@Beta
public interface PortConfigOperatorRegistry {

    /**
     * Registers {@link PortConfigOperator} instance.
     *
     * @param portOp {@link PortConfigOperator} instance.
     * @param configs {@link Config} class for a Port referred by {@code portOp}
     */
    void registerPortConfigOperator(PortConfigOperator portOp,
                                    Class<? extends Config<ConnectPoint>>... configs);

    /**
     * Unregisters {@link PortConfigOperator} instance.
     *
     * @param portOp {@link PortConfigOperator} instance.
     */
    void unregisterPortConfigOperator(PortConfigOperator portOp);

}
