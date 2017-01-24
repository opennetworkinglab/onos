/*
 * Copyright 2017-present Open Networking Laboratory
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

import java.security.Permission;
import java.util.List;
import java.util.Map;

import org.onosproject.core.ApplicationId;

/**
 * Test adapter for SecurityAdminService.
 */
public class SecurityAdminServiceAdapter implements SecurityAdminService {

    @Override
    public boolean isSecured(ApplicationId appId) {
        return false;
    }

    @Override
    public void review(ApplicationId appId) {
    }

    @Override
    public void acceptPolicy(ApplicationId appId) {
    }

    @Override
    public void register(ApplicationId appId) {
    }

    @Override
    public Map<Integer, List<Permission>> getPrintableSpecifiedPermissions(ApplicationId appId) {
        return null;
    }

    @Override
    public Map<Integer, List<Permission>> getPrintableGrantedPermissions(ApplicationId appId) {
        return null;
    }

    @Override
    public Map<Integer, List<Permission>> getPrintableRequestedPermissions(ApplicationId appId) {
        return null;
    }

}
