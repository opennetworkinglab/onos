/*
 * Copyright 2016-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.onosproject.net.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.onlab.osgi.DefaultServiceDirectory;
import org.onlab.osgi.ServiceDirectory;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.CodecService;
import org.onosproject.codec.JsonCodec;
import org.slf4j.Logger;

import java.io.IOException;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * {@link Config} with access to {@link CodecService}.
 *
 * @param <S> type of subject
 */
public abstract class BaseConfig<S>
    extends Config<S>
    implements CodecContext {

    private static ServiceDirectory services = new DefaultServiceDirectory();
    private static final Logger log = getLogger(BaseConfig.class);

    @Override
    public <T> JsonCodec<T> codec(Class<T> entityClass) {
        return getService(CodecService.class).getCodec(entityClass);
    }

    @Override
    public <T> T getService(Class<T> serviceClass) {
        return services.get(serviceClass);
    }

    @Override
    public ObjectMapper mapper() {
        return mapper;
    }

    /**
     * Decodes the specified entity from JSON using codec
     * registered to this context.
     *
     * @param json    JSON String to decode
     * @param entityClass entity class
     * @param <T> entity class type
     * @return decoded entity
     */
    protected <T> T decode(String json, Class<T> entityClass) {
        try {
            return decode(mapper().readTree(json), entityClass);
        } catch (IOException e) {
            log.error("Exception caught.", e);
            throw new IllegalArgumentException(e);
        }
    }

}
