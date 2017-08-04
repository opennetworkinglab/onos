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
package org.onosproject.ovsdb.rfc.jsonrpc;

import java.util.List;

import org.onosproject.ovsdb.rfc.message.UpdateNotification;

/**
 * The callback function interface will be used when the server send to the
 * client report changes.
 */
public interface Callback {
    /**
     * The "update" notification is sent by the server to the client to report
     * changes in tables that are being monitored following a "*monitor"
     * request.
     * @param updateNotification the information of the update
     */
    void update(UpdateNotification updateNotification);

    /**
     * The "locked" notification is provided to notify a client that it has been
     * granted a lock that it had previously requested with the "lock" method.
     * @param ids the locked ids
     */
    void locked(List<String> ids);

    /**
     * The "stolen" notification is provided to notify a client, which had
     * previously obtained a lock, that another client has stolen ownership of
     * that lock.
     * @param ids the stolen ids
     */
    void stolen(List<String> ids);

}
