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
package org.onlab.osgi;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentInstance;

import java.util.Dictionary;

/**
 * Adapter implementation of OSGI component context.
 */
public class ComponentContextAdapter implements ComponentContext {
    @Override
    public Dictionary getProperties() {
        return null;
    }

    @Override
    public Object locateService(String name) {
        return null;
    }

    @Override
    public Object locateService(String name, ServiceReference reference) {
        return null;
    }

    @Override
    public Object[] locateServices(String name) {
        return new Object[0];
    }

    @Override
    public BundleContext getBundleContext() {
        return null;
    }

    @Override
    public Bundle getUsingBundle() {
        return null;
    }

    @Override
    public ComponentInstance getComponentInstance() {
        return null;
    }

    @Override
    public void enableComponent(String name) {

    }

    @Override
    public void disableComponent(String name) {

    }

    @Override
    public ServiceReference getServiceReference() {
        return null;
    }
}
