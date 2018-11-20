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

package org.onlab.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import java.io.IOException;

import static org.onlab.util.Tools.readTreeFromStream;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * FIlter for logging all REST Api http requests and details of request and response.
 */

public class AuditFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static Logger log = getLogger(AuditFilter.class);
    private ObjectMapper mapper = new ObjectMapper();
    private final String separator = "  |  ";

    private static boolean disableForTests = false;
    private static String auditFile = "all";
    private static boolean auditEnabled = false;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        if (disableForTests) {
            return;
        }
        if (isEnabled()) {
            String requestBody = (requestContext.hasEntity() ?
                    (readTreeFromStream(mapper, requestContext.getEntityStream()).toString()) : "");
            requestContext.setProperty("requestBody", requestBody);
            requestContext.setProperty("auditLog", "Path: " + requestContext.getUriInfo().getPath() + separator
                    + "Method: " + requestContext.getMethod() + separator
                    + (requestContext.getMethod().equals("PUT") ?
                    ("Path_Parameters: " + requestContext.getUriInfo().getPathParameters().toString() + separator
                            + "Query_Parameters: " + requestContext.getUriInfo().getQueryParameters().toString()
                            + separator + "Request_Body: " + requestBody) : ""));
            requestContext.setEntityStream(IOUtils.toInputStream(requestBody));
        }
    }

    @Override
    public void filter(ContainerRequestContext containerRequestContext,
                       ContainerResponseContext containerResponseContext) throws IOException {
        if (disableForTests) {
            return;
        }
        if (isEnabled()) {
            containerRequestContext.setProperty("auditLog", containerRequestContext.getProperty("auditLog") + separator
                    + "Status: " + containerResponseContext.getStatusInfo().toString());
            saveAuditLog(containerRequestContext.getProperty("auditLog").toString());
        }
    }

    /**
     * To disable unit testing for this class.
     */
    public static void disableForTests() {
        disableForTests = true;
    }

    /**
     * To save audit logs into the log file.
     *
     * @param msg audit message.
     */
    private void saveAuditLog(String msg) {
        if (isEnabled()) {
            if (auditFile.equals("all")) {
                log.info(msg);
                log.info("AuditLog : " + msg);
            } else if (auditFile.equals("karaf")) {
                log.info(msg);
            } else if (auditFile.equals("audit")) {
                log.info("AuditLog : " + msg);
            }
        }
    }

    /**
     * To check if REST API Audit is enabled.
     *
     * @return true if the REST API Audit is enabled.
     */
    private static boolean isEnabled() {
        return auditEnabled;
    }

    /**
     * To enable REST API Audit.
     */
    public static void enableAudit() {
        auditEnabled = true;
    }

    /**
     * To disable REST API Audit.
     */
    public static void disableAudit() {
        auditEnabled = false;
    }

    /**
     * To set audit file type which REST API Audit logs must be saved.
     *
     * @param auditFile file that REST API Audit logs are saved.
     */
    public static void setAuditFile(String auditFile) {
        AuditFilter.auditFile = auditFile;
    }


}
