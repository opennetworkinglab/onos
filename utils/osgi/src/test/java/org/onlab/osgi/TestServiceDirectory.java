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
package org.onlab.osgi;

import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.MutableClassToInstanceMap;

/**
 * Service directory implementation suitable for testing.
 */
@SuppressWarnings("unchecked")
public class TestServiceDirectory implements ServiceDirectory {

    private ClassToInstanceMap<Object> services = MutableClassToInstanceMap.create();

    @Override
    public <T> T get(Class<T> serviceClass) {
        return services.getInstance(serviceClass);
    }

    /**
     * Adds a new service to the directory.
     *
     * @param serviceClass service class
     * @param service service instance
     * @return self
     */
    public TestServiceDirectory add(Class serviceClass, Object service) {
        services.putInstance(serviceClass, service);
        return this;
    }

}
