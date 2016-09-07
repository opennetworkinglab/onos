/**
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
package org.onosproject.kafkaintegration.api.dto;

import static com.google.common.base.MoreObjects.toStringHelper;

import java.util.Objects;

/**
 * DTO to hold Registration Response for requests from external apps.
 */
public final class RegistrationResponse {

    private EventSubscriberGroupId groupId;

    private String ipAddress;

    private String port;

    public RegistrationResponse(EventSubscriberGroupId groupId,
                                String ipAddress, String port) {
        this.groupId = groupId;
        this.ipAddress = ipAddress;
        this.port = port;
    }

    public final EventSubscriberGroupId getGroupId() {
        return groupId;
    }

    public final String getIpAddress() {
        return ipAddress;
    }

    public final String getPort() {
        return port;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof RegistrationResponse) {
            RegistrationResponse sub = (RegistrationResponse) o;
            if (sub.groupId.equals(groupId) && sub.ipAddress.equals(ipAddress)
                    && sub.port.equals(port)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupId, ipAddress, port);
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("subscriberGroupId", groupId)
                .add("ipAddress", ipAddress).add("port", port).toString();
    }
}
