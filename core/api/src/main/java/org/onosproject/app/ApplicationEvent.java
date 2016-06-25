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
package org.onosproject.app;

import org.onosproject.core.Application;
import org.onosproject.event.AbstractEvent;

/**
 * Describes application lifecycle event.
 */
public class ApplicationEvent extends AbstractEvent<ApplicationEvent.Type, Application> {

    public enum Type {
        /**
         * Signifies that an application has been installed.
         */
        APP_INSTALLED,

        /**
         * Signifies that an application has been activated.
         */
        APP_ACTIVATED,

        /**
         * Signifies that an application has been deactivated.
         */
        APP_DEACTIVATED,

        /**
         * Signifies that an application has been uninstalled.
         */
        APP_UNINSTALLED,

        /**
         * Signifies that application granted permissions have changed.
         */
        APP_PERMISSIONS_CHANGED
    }

    /**
     * Creates an event of a given type and for the specified app and the
     * current time.
     *
     * @param type app event type
     * @param app  event app subject
     */
    public ApplicationEvent(Type type, Application app) {
        super(type, app);
    }

    /**
     * Creates an event of a given type and for the specified app and time.
     *
     * @param type app event type
     * @param app  event app subject
     * @param time occurrence time
     */
    public ApplicationEvent(Type type, Application app, long time) {
        super(type, app, time);
    }

}
