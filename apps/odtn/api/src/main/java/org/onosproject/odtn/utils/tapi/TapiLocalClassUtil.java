/*
 * Copyright 2018-present Open Networking Foundation
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

package org.onosproject.odtn.utils.tapi;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Map.Entry;
import org.onosproject.yang.gen.v1.tapicommon.rev20181210.tapicommon.globalclass.DefaultName;
import org.onosproject.yang.gen.v1.tapicommon.rev20181210.tapicommon.globalclass.Name;
import org.onosproject.yang.model.ModelObject;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Utility methods dealing with TAPI modelObject which includes local class grouping.
 * <p>
 * tapi-common@2018-03-07.yang
 * grouping local-class {
 * leaf local-id {
 * type string;
 * }
 * list name {
 * key 'value-name';
 * uses name-and-value;
 * }
 * }
 * </p>
 * <p>
 * grouping name-and-value {
 * leaf value-name {
 * type string;
 * description "The name of the value. The value need not have a name.";
 * }
 * leaf value {
 * type string;
 * description "The value";
 * }
 * description "A scoped name-value pair";
 * }
 * </p>
 */
public final class TapiLocalClassUtil {

    private static final Logger log = getLogger(TapiLocalClassUtil.class);

    private TapiLocalClassUtil() {
    }

    /**
     * Set local-id for the ModelObject.
     *
     * @param obj     ModelObject
     * @param localId LocalId
     * @param <T>     Type of ModelObject
     */
    public static <T extends ModelObject> void setLocalId(T obj, String localId) {
        @SuppressWarnings("unchecked")
        Class<T> cls = (Class<T>) obj.getClass();
        try {
            Method method = cls.getMethod("localId", String.class);
            method.invoke(obj, localId);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            log.error("Exception thrown", e);
        }
    }

    /**
     * Get local-id for the ModelObject.
     *
     * @param obj ModelObject
     * @param <T> Type of ModelObject
     * @return Local-id
     */
    public static <T extends ModelObject> String getLocalId(T obj) {
        String localId = null;
        @SuppressWarnings("unchecked")
        Class<T> cls = (Class<T>) obj.getClass();
        try {
            Method method = cls.getMethod("localId");
            localId = (String) method.invoke(obj);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            log.error("Exception thrown", e);
        }

        return localId;
    }

    /**
     * Add key-value to the ModelObject as "name-and-value" list.
     *
     * @param obj ModelObject
     * @param kvs Key-value map
     * @param <T> Type of ModelObject
     */
    public static <T extends ModelObject> void addNameList(T obj, Map<String, String> kvs) {

        @SuppressWarnings("unchecked")
        Class<T> cls = (Class<T>) obj.getClass();
        try {
            Method method = cls.getMethod("addToName", Name.class);

            for (Entry<String, String> kv : kvs.entrySet()) {
                DefaultName prop = new DefaultName();
                prop.valueName(kv.getKey());
                prop.value(kv.getValue());
                method.invoke(obj, prop);
            }
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            log.error("Exception thrown", e);
        }
    }
}
