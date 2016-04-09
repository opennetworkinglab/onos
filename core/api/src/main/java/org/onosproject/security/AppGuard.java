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

import java.security.AccessController;
import java.security.AccessControlContext;
import com.google.common.annotations.Beta;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
/**
 * Aids SM-ONOS to perform API-level permission checking.
 */
@Beta
public final class AppGuard {
    private AppGuard() {

    }

    /**
     * Checks if the caller has the required permission only when security-mode is enabled.
     *
     * @param permission permission to be checked
     */
    public static void checkPermission(AppPermission.Type permission) {
        SecurityManager sm = System.getSecurityManager();
        if (sm == null) {
            return;
        }
        AccessControlContext context = AccessController.getContext();
        if (context == null) {
            sm.checkPermission(new AppPermission((permission)));
        } else {
            int contextHash = context.hashCode() ^ permission.hashCode();
            PermissionCheckCache.getInstance().checkCache(contextHash, new AppPermission(permission));
        }
    }

    private static final class PermissionCheckCache {

        private static final Cache<Integer, Boolean> CACHE = CacheBuilder.newBuilder()
                .maximumSize(1000)
                .expireAfterAccess(10, TimeUnit.MINUTES)
                .build();

        private PermissionCheckCache() {
        }

        private static class SingletonHelper {
            private static final PermissionCheckCache INSTANCE = new PermissionCheckCache();
        }

        public static PermissionCheckCache getInstance() {
            return SingletonHelper.INSTANCE;
        }

        public static void checkCache(int key, AppPermission perm) {
            try {
                CACHE.get(key, () -> {
                    System.getSecurityManager().checkPermission(perm);
                    return true;
                });
            } catch (ExecutionException e) {
                System.getSecurityManager().checkPermission(perm);
            }
        }
    }
}
