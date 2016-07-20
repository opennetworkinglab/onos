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

import java.net.URI;

import com.google.common.annotations.Beta;

// This is actually the RPC Service, where consumers get
// RemoteSericeContext (~= RPC Session) for given URI
// expected to be implemented by some Manager class on Lower-side ONOS
/**
 * Service for retrieving RPC session handler ({@link RemoteServiceContext}).
 */
@Beta
public interface RemoteServiceDirectory {

    /**
     * Returns remote service context.
     *
     * @param uri URI representing remote end point. e.g., (grpc://hostname:port)
     * @return remote service context
     * @throws UnsupportedOperationException if URI scheme was not supported.
     */
    RemoteServiceContext get(URI uri);

}
