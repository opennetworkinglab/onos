/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onosproject.dhcprelay.config;

import org.onosproject.core.ApplicationId;
import org.onosproject.net.config.Config;


/**
 * Dhcp Fpm Config.
 */
public class EnableDhcpFpmConfig extends Config<ApplicationId> {
    public static final String KEY = "dhcpFpm";
    private static final String DHCP_FPM_ENABLE = "enabled";

    @Override
    public boolean isValid() {
      if (!hasFields(DHCP_FPM_ENABLE)) {
          return false;
      }
      return isBoolean(DHCP_FPM_ENABLE, FieldPresence.OPTIONAL);
    }

    /**
     * Returns whether Dhcp Fpm is enabled.
     *
     * @return true if enabled, otherwise false
     */
    public boolean getDhcpFpmEnable() {
        return object.path(DHCP_FPM_ENABLE).asBoolean(false);
    }
}
