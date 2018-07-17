/*
 * Copyright 2018-present Open Networking Foundation
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

package org.onlab.rest;

import com.google.common.collect.ImmutableSet;

import javax.ws.rs.ForbiddenException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import java.io.IOException;
import java.util.Set;

/**
 * Filter that performs authorization checks on all incoming REST API requests.
 * Methods with modify semantics require 'admin' role; all others require 'viewer' role.
 */
public class AuthorizationFilter implements ContainerRequestFilter {

    private static final String ADMIN = "admin";
    private static final String VIEWER = "viewer";

    private static final String FORBIDDEN_MSG =
            "User has insufficient privilege for this request";

    private static final Set<String> PRIVILEGED_METHODS =
            ImmutableSet.of("POST", "PUT", "DELETE", "PATCH");

    private static boolean disableForTests = false;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        if (disableForTests) {
            return;
        }
        if ((PRIVILEGED_METHODS.contains(requestContext.getMethod()) &&
                !requestContext.getSecurityContext().isUserInRole(ADMIN)) ||
                !requestContext.getSecurityContext().isUserInRole(VIEWER)) {
            throw new ForbiddenException(FORBIDDEN_MSG);
        }
    }

    public static void disableForTests() {
        disableForTests = true;
    }
}
