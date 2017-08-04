/*
 * Copyright 2016-present Open Networking Foundation
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
package org.onosproject.cpman.impl;

import org.onosproject.cpman.SystemInfo;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * A factory class which instantiates a system info object.
 */
public final class SystemInfoFactory {

    private final Logger log = getLogger(getClass());

    private SystemInfo systemInfo;

    // non-instantiable (except for our Singleton)
    private SystemInfoFactory() {
    }

    /**
     * Returns system information.
     *
     * @return reference object of system info
     */
    public SystemInfo getSystemInfo() {
        synchronized (systemInfo) {
            return this.systemInfo;
        }
    }

    /**
     * Set system information only if it is empty.
     *
     * @param systemInfo reference object of system info
     */
    public void setSystemInfo(SystemInfo systemInfo) {
        synchronized (systemInfo) {
            if (this.systemInfo == null) {
                this.systemInfo = systemInfo;
            } else {
                log.warn("System information has already been set");
            }
        }
    }

    /**
     * Returns an instance of system info factory.
     *
     * @return instance of system info factory
     */
    public static SystemInfoFactory getInstance() {
        return SingletonHelper.INSTANCE;
    }

    private static class SingletonHelper {
        private static final SystemInfoFactory INSTANCE = new SystemInfoFactory();
    }
}
