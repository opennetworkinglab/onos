/*
 * Copyright 2015-present Open Networking Foundation
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

import org.onosproject.rest.AbstractWebResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.FileInputStream;
import java.io.IOException;

import static com.google.common.io.ByteStreams.toByteArray;

/**
 * Provides stream of diagnostic information.
 */
@Path("diagnostics")
public class DiagnosticsWebResource extends AbstractWebResource {

    private static final String COMMAND = "../bin/onos-node-diagnostics";
    private static final String DIAGS = "/tmp/onos-node-diags.tar.gz";

    private final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * Get tar.gz stream of node diagnostic information.
     *
     * @return 200 OK with a tar.gz stream of diagnostic data
     */
    @GET
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response getDiagnostics() {
        try {
            execute(COMMAND);
            return ok(new FileInputStream(DIAGS)).build();
        } catch (IOException e) {
            return Response.serverError().build();
        }
    }

    // Executes the given command arguments as a system command.
    private void execute(String command) throws IOException {
        try {
            Process process = Runtime.getRuntime().exec(command);
            byte[] output = toByteArray(process.getInputStream());
            byte[] error = toByteArray(process.getErrorStream());
            int code = process.waitFor();
            if (code != 0) {
                log.info("Command failed: status={}, output={}, error={}",
                         code, new String(output), new String(error));
            }
        } catch (InterruptedException e) {
            log.error("Interrupted executing command {}", command, e);
            Thread.currentThread().interrupt();
        }
    }
}
