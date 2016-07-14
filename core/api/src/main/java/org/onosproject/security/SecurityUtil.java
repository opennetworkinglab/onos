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

package org.onosproject.security;

import com.google.common.annotations.Beta;
import org.onlab.osgi.DefaultServiceDirectory;
import org.onlab.osgi.ServiceDirectory;
import org.onlab.osgi.ServiceNotFoundException;
import org.onosproject.core.ApplicationId;

/**
 * Utility class to aid Security-Mode ONOS.
 */
@Beta
public final class SecurityUtil {

    protected static ServiceDirectory serviceDirectory = new DefaultServiceDirectory();

    private SecurityUtil() {
    }

    public static boolean isSecurityModeEnabled() {
        if (System.getSecurityManager() != null) {
            try {
                SecurityAdminService securityService = serviceDirectory.get(SecurityAdminService.class);
                if (securityService != null) {
                    return true;
                }
            } catch (ServiceNotFoundException e) {
                return false;
            }
        }
        return false;
    }

    public static SecurityAdminService getSecurityService() {
        if (System.getSecurityManager() != null) {
            try {
                SecurityAdminService securityService = serviceDirectory.get(SecurityAdminService.class);
                if (securityService != null) {
                    return securityService;
                }
            } catch (ServiceNotFoundException e) {
                return null;
            }
        }
        return null;
    }

    public static boolean isAppSecured(ApplicationId appId) {
        SecurityAdminService service = getSecurityService();
        if (service != null) {
            if (!service.isSecured(appId)) {
                System.out.println("\n*******************************");
                System.out.println("      SM-ONOS APP WARNING      ");
                System.out.println("*******************************");
                System.out.println(appId.name() + " has not been secured.");
                System.out.println("Please review before activating.");
                return false;
            }
        }
        return true;
    }
    public static void register(ApplicationId appId) {
        SecurityAdminService service = getSecurityService();
        if (service != null) {
            service.register(appId);
        }
    }
}
