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
package org.onosproject.restconf.api;

import com.fasterxml.jackson.databind.node.ObjectNode;

import javax.ws.rs.core.Response;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Output of a RESTCONF RPC.
 */
public class RestconfRpcOutput {
    private Response.Status status;
    private ObjectNode output;
    private String reasonPhrase;


    /**
     * Default constructor.
     */
    public RestconfRpcOutput() {

    }

    /**
     * Instantiates a new RPC output object.
     *
     * @param s RPC execution status
     * @param d RPC output in JSON format
     */
    public RestconfRpcOutput(Response.Status s, ObjectNode d) {
        this.status = s;
        this.output = d;
    }

    /**
     * Sets the RPC execution status.
     *
     * @param s status
     */
    public void status(Response.Status s) {
        this.status = s;
    }

    /**
     * Sets the failure reason message for the RPC execution.
     *
     * @param s failure reason
     */
    public void reason(String s) {
        this.reasonPhrase = s;
    }

    /**
     * Returns the failure reason.
     *
     * @return failure reason
     */
    public String reason() {
        return this.reasonPhrase;
    }

    /**
     * Sets the RPC output.
     *
     * @param d RPC output in JSON format
     */
    public void output(ObjectNode d) {
        this.output = d;
    }

    /**
     * Returns the RPC execution status.
     *
     * @return status
     */
    public Response.Status status() {
        return this.status;
    }

    /**
     * Returns the RPC output in JSON format.
     *
     * @return output
     */
    public ObjectNode output() {
        return this.output;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("status", status)
                .add("reason", reasonPhrase)
                .add("output", output)
                .toString();
    }
}
