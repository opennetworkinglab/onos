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
package org.onosproject.netconf;

import java.util.List;

import com.google.common.annotations.Beta;
import com.google.common.collect.ImmutableList;

/**
 * Represents class of errors related to NETCONF rpc messaging.
 */
@Beta
public class NetconfRpcException extends RuntimeException {

    private static final long serialVersionUID = -5947975698207522820L;

    private final List<NetconfRpcError> error;

    /**
     * @param errors RPC error message body
     */
    public NetconfRpcException(List<NetconfRpcError> errors) {
        super(getErrorMsg(errors));
        this.error = ImmutableList.copyOf(errors);
    }

    /**
     * @param errors RPC error message body
     * @param message describing the error
     */
    public NetconfRpcException(List<NetconfRpcError> errors, String message) {
        super(message);
        this.error = ImmutableList.copyOf(errors);
    }

    /**
     * @param errors RPC error message body
     * @param cause of this exception
     */
    public NetconfRpcException(List<NetconfRpcError> errors, Throwable cause) {
        super(getErrorMsg(errors), cause);
        this.error = ImmutableList.copyOf(errors);
    }

    /**
     * @param errors RPC error message body
     * @param message describing the error
     * @param cause of this exception
     */
    public NetconfRpcException(List<NetconfRpcError> errors, String message, Throwable cause) {
        super(message, cause);
        this.error = ImmutableList.copyOf(errors);
    }

    /**
     * Retrieves rpc-error details.
     *
     * @return the rpc-errors
     */
    public List<NetconfRpcError> rpcErrors() {
        return error;
    }


    @Override
    public String toString() {
        return super.toString() + " " + error;
    }

    /**
     * Gets the first non-empty error message or {@code ""}.
     * @param errors to fetch message from
     * @return first non-empty error message or {@code ""}
     */
    private static String getErrorMsg(List<NetconfRpcError> errors) {
        return errors.stream()
            .map(NetconfRpcError::message)
            .filter(s -> !s.isEmpty())
            .findFirst().orElse("");
    }

}
