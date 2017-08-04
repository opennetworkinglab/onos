/*
 * Copyright 2015-present Open Networking Foundation
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

package org.onosproject.security.store;

import org.onosproject.core.ApplicationId;
import org.onosproject.event.AbstractEvent;

/**
 * Security-Mode ONOS notifications.
 */
public class SecurityModeEvent extends AbstractEvent<SecurityModeEvent.Type, ApplicationId> {

    protected SecurityModeEvent(Type type, ApplicationId subject) {
        super(type, subject);
    }

    public enum Type {

        /**
         * Signifies that security policy has been accepted.
         */
        POLICY_ACCEPTED,

        /**
         * Signifies that security policy has been reviewed.
         */
        POLICY_REVIEWED,

        /**
         * Signifies that application has violated security policy.
         */
        POLICY_VIOLATED,
    }
}
