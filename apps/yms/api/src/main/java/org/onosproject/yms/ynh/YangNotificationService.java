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

import org.onosproject.event.ListenerService;

/**
 * Abstraction of an entity which provides interfaces to YANG notification
 * service. YANG notification handler receives the event notifications from
 * application/core and provide it to the protocols.
 *
 * NBI Protocols which can support notification delivery for application(s)
 * needs to add themselves as a listeners with YANG notification service.
 * Also based on registered schema YMS add themselves as a listener to
 * applications. Application sends their notification in YANG utils generated
 * notification. YMS obtains the module/sub-module schema tree in which this
 * notification is contained and then convert this data to an abstract tree
 * notation (YDT) with root node as the module/sub-module node in which
 * notification is contained. It sends the same to all the protocol who has
 * added them as a listener as a YANG notification.
 *
 * Also Protocols can use YANG notification service to check if a received
 * notification should be filtered against any of their protocol specific
 * filtering mechanism.
 */
public interface YangNotificationService
        extends ListenerService<YangNotificationEvent,
        YangNotificationListener> {

   /*
    *      Example of a use case of notification filtering.
    *      The following example illustrates how to select fault events which
    *      have severities of critical, major, or minor.  The filtering criteria
    *      evaluation is as follows:
    *
    *      ((fault & severity=critical) | (fault & severity=major) | (fault &
    *      severity=minor))
    *
    *           <netconf:rpc netconf:message-id="101"
    *                   xmlns:netconf="urn:ietf:params:xml:ns:netconf:base:1.0">
    *             <create-subscription
    *                 xmlns="urn:ietf:params:xml:ns:netconf:notification:1.0">
    *               <filter netconf:type="subtree">
    *                 <event xmlns="http://example.com/event/1.0">
    *                   <eventClass>fault</eventClass>
    *                   <severity>critical</severity>
    *                 </event>
    *                 <event xmlns="http://example.com/event/1.0">
    *                   <eventClass>fault</eventClass>
    *                   <severity>major</severity>
    *                 </event>
    *                 <event xmlns="http://example.com/event/1.0">
    *                   <eventClass>fault</eventClass>
    *                   <severity>minor</severity>
    *                 </event>
    *               </filter>
    *             </create-subscription>
    *           </netconf:rpc>
    */

    /**
     * Protocols have their own mechanism to support notification filtering
     * or notification subscription. Depending on the protocol specification,
     * the filtering is implemented in the protocol.
     * The Protocol implementations are abstracted of the Schema, there are
     * scenarios in which  they need to check if the received notification
     * is of interest as per the schema filtering / subscription.
     * In such scenario, protocols can create a filtering / subscription YANG
     * data tree and use the notification service to filter the notification
     * subject against their filter.
     *
     * Filters the notification subject YANG data tree, with the specified
     * filter of the NBI protocol. If the filter does not match for the
     * passed notification subject, null will be returned.
     * Otherwise, the part of the subject matching the filter will be returned.
     *
     * @param notificationSubject YANG notification subject reported by YANG
     *                            notification service.
     * @param notificationFilter  Protocols data model specific notification
     *                            filter represented in YANG data tree.
     * @return filtered notification which passes the data model specific
     * notification filter.
     */
    YangNotification getFilteredSubject(YangNotification notificationSubject,
                                        YangNotification notificationFilter);

}
