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
package org.onlab.osgi;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

/**
 * Default implementation of the service directory using OSGi framework utilities.
 */
public class DefaultServiceDirectory implements ServiceDirectory {

    /**
     * Returns the reference to the implementation of the specified service.
     *
     * @param serviceClass service class
     * @param <T>          type of service
     * @return service implementation
     */
    public static <T> T getService(Class<T> serviceClass) {
        Bundle bundle = FrameworkUtil.getBundle(serviceClass);
        if (bundle != null) {
            BundleContext bc = bundle.getBundleContext();
            if (bc != null) {
                ServiceReference<T> reference = bc.getServiceReference(serviceClass);
                if (reference != null) {
                    T impl = bc.getService(reference);
                    if (impl != null) {
                        return impl;
                    }
                }
            }
        }
        throw new ServiceNotFoundException("Service " + serviceClass.getName() + " not found");
    }

    @Override
    public <T> T get(Class<T> serviceClass) {
        return getService(serviceClass);
    }

}
