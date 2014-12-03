/*
 * Copyright 2014 Open Networking Laboratory
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
package org.onosproject.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onlab.util.ItemNotFoundException;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.CodecService;
import org.onosproject.codec.JsonCodec;
import org.onlab.rest.BaseResource;

/**
 * Abstract REST resource.
 */
public class AbstractWebResource extends BaseResource implements CodecContext {

    @Override
    public ObjectMapper mapper() {
        return new ObjectMapper();
    }

    /**
     * Returns the JSON codec for the specified entity class.
     *
     * @param entityClass entity class
     * @param <T>         entity type
     * @return JSON codec
     */
    public <T> JsonCodec<T> codec(Class<T> entityClass) {
        return get(CodecService.class).getCodec(entityClass);
    }

    /**
     * Returns JSON object wrapping the array encoding of the specified
     * collection of items.
     *
     * @param codecClass codec item class
     * @param field      field holding the array
     * @param items      collection of items to be encoded into array
     * @param <T>        item type
     * @return JSON object
     */
    protected <T> ObjectNode encodeArray(Class<T> codecClass, String field,
                                         Iterable<T> items) {
        ObjectNode result = mapper().createObjectNode();
        result.set(field, codec(codecClass).encode(items, this));
        return result;
    }

    /**
     * Returns the specified item if that items is null; otherwise throws
     * not found exception.
     *
     * @param item    item to check
     * @param message not found message
     * @param <T>     item type
     * @return item if not null
     * @throws org.onlab.util.ItemNotFoundException if item is null
     */
    protected <T> T nullIsNotFound(T item, String message) {
        if (item == null) {
            throw new ItemNotFoundException(message);
        }
        return item;
    }

}
