/*
 * Copyright 2021-present Open Networking Foundation
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
package org.onosproject.kubevirtnetworking.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.onlab.osgi.DefaultServiceDirectory;
import org.onlab.osgi.ServiceDirectory;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.CodecService;
import org.onosproject.codec.JsonCodec;

/**
 * Abstract watcher.
 */
public class AbstractWatcher implements CodecContext {

    private static ServiceDirectory services = new DefaultServiceDirectory();
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public ObjectMapper mapper() {
        return mapper;
    }

    @Override
    public <T> JsonCodec<T> codec(Class<T> entityClass) {
        return services.get(CodecService.class).getCodec(entityClass);
    }

    @Override
    public <T> T getService(Class<T> serviceClass) {
        return services.get(serviceClass);
    }
}
