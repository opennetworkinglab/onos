/*
 * Copyright 2014-present Open Networking Foundation
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

import org.onlab.osgi.DefaultServiceDirectory;
import org.onlab.osgi.ServiceDirectory;

import javax.ws.rs.core.Response;

/**
 * Base abstraction of a JAX-RS resource.
 */
public abstract class BaseResource {

    private static ServiceDirectory services = new DefaultServiceDirectory();

    /**
     * Returns reference to the specified service implementation.
     *
     * @param service service class
     * @param <T>     type of service
     * @return service implementation
     */
    public <T> T get(Class<T> service) {
        return services.get(service);
    }

    protected static Response.ResponseBuilder ok(Object obj) {
        return Response.ok(obj);
    }

}
