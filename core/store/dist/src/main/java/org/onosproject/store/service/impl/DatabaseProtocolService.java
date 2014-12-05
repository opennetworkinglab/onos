/*
 * Copyright 2014 Open Networking Laboratory
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
package org.onosproject.store.service.impl;

import net.kuujo.copycat.cluster.TcpMember;
import net.kuujo.copycat.spi.protocol.Protocol;

// interface required for connecting DatabaseManager + ClusterMessagingProtocol
// TODO: Consider changing ClusterMessagingProtocol to non-Service class
public interface DatabaseProtocolService extends Protocol<TcpMember> {

}
