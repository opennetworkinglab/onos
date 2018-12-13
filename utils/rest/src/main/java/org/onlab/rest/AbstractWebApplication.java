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

package org.onlab.rest;

import com.google.common.collect.ImmutableSet;
import org.onlab.rest.exceptions.BadRequestMapper;
import org.onlab.rest.exceptions.EntityNotFoundMapper;
import org.onlab.rest.exceptions.ForbiddenMapper;
import org.onlab.rest.exceptions.IllegalArgumentExceptionMapper;
import org.onlab.rest.exceptions.IllegalStateExceptionMapper;
import org.onlab.rest.exceptions.NotFoundMapper;
import org.onlab.rest.exceptions.ServerErrorMapper;
import org.onlab.rest.exceptions.ServiceNotFoundMapper;
import org.onlab.rest.exceptions.WebApplicationExceptionMapper;

import javax.ws.rs.core.Application;
import java.util.Set;

/**
 * Base web application.
 */
public abstract class AbstractWebApplication extends Application {

    /**
     * Returns the aggregate set of resources, writers and mappers combined
     * with a default set of such web entities.
     *
     * @param classes set of resources, writers and mappers
     * @return combined set of web entities
     */
    protected Set<Class<?>> getClasses(Class<?>... classes) {
        ImmutableSet.Builder<Class<?>> builder = ImmutableSet.builder();
        builder.add(AuthorizationFilter.class,
                    ForbiddenMapper.class,
                    ServiceNotFoundMapper.class,
                    EntityNotFoundMapper.class,
                    NotFoundMapper.class,
                    ServerErrorMapper.class,
                    BadRequestMapper.class,
                    WebApplicationExceptionMapper.class,
                    IllegalArgumentExceptionMapper.class,
                    IllegalStateExceptionMapper.class,
                    JsonBodyWriter.class);
        builder.add(classes);
        return builder.build();
    }

    @Override
    public abstract Set<Class<?>> getClasses();

}
