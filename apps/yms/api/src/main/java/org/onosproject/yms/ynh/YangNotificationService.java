/*
 * Copyright 2016-present Open Networking Laboratory
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

package org.onosproject.yms.ynh;

/**
 * Abstraction of an entity which provides interfaces to YANG notification
 * service. YNH handles notification from the application/core and provide
 * it to the protocols.
 * <p>
 * NBI Protocols which can support notification delivery for application(s)
 * needs to add themselves as a listeners with YANG notification service.
 * Protocols can use YANG notification service to check if a received
 * notification should be filtered against any of their protocol specific
 * filtering mechanism.
 */
public interface YangNotificationService {
    //TODO
}
