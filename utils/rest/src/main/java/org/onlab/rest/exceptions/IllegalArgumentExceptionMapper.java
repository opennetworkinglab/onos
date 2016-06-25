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

package org.onlab.rest.exceptions;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

/**
 * Mapper for illegal argument exceptions to the BAD_REQUEST response code.
 */
@Provider
public class IllegalArgumentExceptionMapper extends AbstractMapper<IllegalArgumentException> {
    @Override
    protected Response.Status responseStatus() {
        return Response.Status.BAD_REQUEST;
    }
}

