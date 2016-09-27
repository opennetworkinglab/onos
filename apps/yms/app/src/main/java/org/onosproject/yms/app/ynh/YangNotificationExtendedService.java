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

package org.onosproject.yms.app.ynh;

import org.onosproject.event.ListenerService;
import org.onosproject.yms.ynh.YangNotificationService;

/**
 * Abstraction of an entity which provides interfaces to YANG extended notification
 * service. It provides extended interfaces required by YMS internal modules.
 * Application registers their schema with YMSM, YMSM delegates the registration
 * request to YSR. YSR then looks for the presence of notification in application
 * schema, presence of notification will trigger YSR to ask YANG extended notification
 * service to register it as a listener to that application events.
 */
public interface YangNotificationExtendedService extends YangNotificationService {

    /**
     * Registers as listener with application. This is called by YSR when it
     * detects notification presence in application YANG file at the time when
     * application registers it's schema with YMS.
     *
     * @param appObject application object
     */
    void registerAsListener(ListenerService appObject);

    // TODO handle scenario when multiple services are implemented by single manager.
}
