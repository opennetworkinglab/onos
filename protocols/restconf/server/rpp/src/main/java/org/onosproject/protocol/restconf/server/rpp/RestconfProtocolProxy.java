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
package org.onosproject.protocol.restconf.server.rpp;

import org.onlab.rest.AbstractWebApplication;
import org.onosproject.restconf.api.RestconfJsonBodyWriter;

import java.util.Set;

/**
 * RESTCONF Server front-end application.
 */
public class RestconfProtocolProxy extends AbstractWebApplication {

    @Override
    public Set<Class<?>> getClasses() {
        return getClasses(
                RestconfWebResource.class,
                RestconfJsonBodyWriter.class
        );
    }
}
