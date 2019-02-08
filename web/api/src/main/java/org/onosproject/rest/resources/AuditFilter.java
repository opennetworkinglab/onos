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

package org.onosproject.rest.resources;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.onlab.osgi.DefaultServiceDirectory;
import org.onlab.osgi.ServiceDirectory;
import org.onosproject.security.AuditService;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import java.io.IOException;

import static org.onlab.util.Tools.readTreeFromStream;

/**
 * HTTP Filter for auditing REST API requests.
 */
public class AuditFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private ObjectMapper mapper = new ObjectMapper();
    private final String separator = "\", \"";
    private final String logCompSeperator = "\" : \"";

    private static boolean disableForTests = false;
    private static ServiceDirectory services = new DefaultServiceDirectory();

    /**
     * Disables functionality for unit tests.
     */
    public static void disableForTests() {
        disableForTests = true;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        if (auditService() != null) {
            String requestBody = (requestContext.hasEntity() ?
                    (readTreeFromStream(mapper, requestContext.getEntityStream()).toString()) : "");
            requestContext.setProperty("requestBody", requestBody);
            // FIXME: audit message should be better structured
            requestContext.setProperty("auditMessage", "{\"Path" + logCompSeperator
                    + requestContext.getUriInfo().getPath() + separator + "Method"
                    + logCompSeperator + requestContext.getMethod() + separator
                    + (requestContext.getMethod().equals("PUT") ?
                    // FIXME: is there really a need to differentiate based on method?
                    ("Path_Parameters" + logCompSeperator + requestContext.getUriInfo().getPathParameters().toString()
                            + separator + "Query_Parameters" + logCompSeperator
                            + requestContext.getUriInfo().getQueryParameters().toString()
                            + separator + "Request_Body" + logCompSeperator + requestBody) : ""));
            requestContext.setEntityStream(IOUtils.toInputStream(requestBody));
        }
    }

    @Override
    public void filter(ContainerRequestContext containerRequestContext,
                       ContainerResponseContext containerResponseContext) throws IOException {
        AuditService auditService = auditService();
        if (auditService != null) {
            containerRequestContext.setProperty("auditMessage", containerRequestContext.getProperty("auditMessage")
                    + separator + "Status" + logCompSeperator + containerResponseContext.getStatusInfo().toString()
                    + "\"}");
            // FIXME: Audit record should indicate who did it, not just what was done and when
            String user = containerRequestContext.getSecurityContext().getUserPrincipal().getName();
            String action = containerRequestContext.getProperty("auditMessage").toString();
            auditService.logUserAction(user, action);
        }
    }

    private AuditService auditService() {
        AuditService auditService = null;
        try {
            auditService = disableForTests ? null : services.get(AuditService.class);
        } catch (org.onlab.osgi.ServiceNotFoundException e) {
            return null;
        }
        return auditService != null && auditService.isAuditing() ? auditService : null;
    }
}
