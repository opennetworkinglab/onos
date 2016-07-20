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

import org.onlab.util.ItemNotFoundException;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

/**
 * Mapper for service not found exceptions to the NOT_FOUND response code.
 */
@Provider
public class EntityNotFoundMapper extends AbstractMapper<ItemNotFoundException> {
    @Override
    protected Response.Status responseStatus() {
        return Response.Status.NOT_FOUND;
    }
}
