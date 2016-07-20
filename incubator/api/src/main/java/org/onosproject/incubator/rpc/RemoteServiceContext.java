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

package org.onosproject.incubator.rpc;

import com.google.common.annotations.Beta;

// Implementation is expected to be a handler for RPC channel
// and shim-layer to convert Java Service interface calls to/from RPC call
/**
 * Context for Remote service.
 */
@Beta
public interface RemoteServiceContext {

    // we may need a method to check connection state?

    /**
     * Returns implementation of the specified service class.
     *
     * @param serviceClass service class
     * @param <T>          type of service
     * @return implementation class
     * @throws UnsupportedOperationException if this context does not support it.
     */
    <T> T get(Class<T> serviceClass);
}
