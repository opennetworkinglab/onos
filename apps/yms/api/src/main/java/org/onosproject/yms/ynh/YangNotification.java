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

import org.onosproject.yms.ydt.YdtContext;

/**
 * Represents YANG notification which is a subject of YANG based event.
 *
 * YMS add themselves as a listener to applications. Application sends
 * their notification in YANG utils generated notification. YMS obtains
 * the module/sub-module schema tree in which this notification is contained
 * and then convert this data to an abstract tree notation (YDT) with root
 * node as the logical node with name "yangnotification" it contains
 * module/sub-module node in which notification is contained.
 * It sends the same to all the protocol who has added them as a listener
 * as a YANG notification.
 *
 * This class represents YANG notification which contains the
 * notification context in abstract tree notation.
 */
public class YangNotification {

    /**
     * YANG notification in form of abstract tree notation (YDT)
     * Root node of notification root context will be logical node with
     * name as "yangnotification", it contains module/sub-module node
     * in which notification is contained.
     */
    YdtContext notificationRootContext;

    /**
     * Creates an instance of YANG notification subject.
     *
     * @param notificationContext logical root node with name as
     *                            "yangnotification"
     */
    public YangNotification(YdtContext notificationContext) {
        this.notificationRootContext = notificationContext;
    }

    /**
     * Assign the YANG modeled notification data.
     *
     * @param notificationRootContext YANG data tree containing the data for
     *                                the notification
     */
    public void setNotificationRootContext(YdtContext notificationRootContext) {
        this.notificationRootContext = notificationRootContext;
    }

    /**
     * Returns YANG notification data.
     *
     * @return YANG data tree containing the data for the notification
     */
    public YdtContext getNotificationRootContext() {
        return notificationRootContext;
    }
}
