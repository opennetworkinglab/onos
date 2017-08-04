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
package org.onosproject.openflow.controller.driver;

/**
 * When we remove a pending role request we use this enum to indicate how we
 * arrived at the decision. When we send a role request to the switch, we
 * also use  this enum to indicate what we expect back from the switch, so the
 * role changer can match the reply to our expectation.
 */
public enum RoleRecvStatus {
    /** The switch returned an error indicating that roles are not.
     * supported*/
    UNSUPPORTED,
    /** The request timed out. */
    NO_REPLY,
    /** The reply was old, there is a newer request pending. */
    OLD_REPLY,
    /**
     *  The reply's role matched the role that this controller set in the
     *  request message - invoked either initially at startup or to reassert
     *  current role.
     */
    MATCHED_CURRENT_ROLE,
    /**
     *  The reply's role matched the role that this controller set in the
     *  request message - this is the result of a callback from the
     *  global registry, followed by a role request sent to the switch.
     */
    MATCHED_SET_ROLE,
    /**
     * The reply's role was a response to the query made by this controller.
     */
    REPLY_QUERY,
    /** We received a role reply message from the switch
     *  but the expectation was unclear, or there was no expectation.
     */
    OTHER_EXPECTATION,
}
