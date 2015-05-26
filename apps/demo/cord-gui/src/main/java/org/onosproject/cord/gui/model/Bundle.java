/*
 * Copyright 2015 Open Networking Laboratory
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
 *
 */

package org.onosproject.cord.gui.model;

import com.google.common.collect.ImmutableSet;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Encapsulates a bundle, including current state.
 */
public class Bundle {
    private final BundleDescriptor bundleDescriptor;
    private final Map<XosFunctionDescriptor, XosFunction> functionMap =
        new HashMap<XosFunctionDescriptor, XosFunction>();

    /**
     * Constructs a new bundle instance.
     *
     * @param bundleDescriptor the descriptor
     */
    public Bundle(BundleDescriptor bundleDescriptor) {
        this.bundleDescriptor = bundleDescriptor;
        initFunctions();
    }

    /**
     * Returns the bundle descriptor.
     *
     * @return the descriptor
     */
    public BundleDescriptor descriptor() {
        return bundleDescriptor;
    }

    /**
     * Returns the set of function instances for this bundle.
     *
     * @return the functions
     */
    public Set<XosFunction> functions() {
        return ImmutableSet.copyOf(functionMap.values());
    }

    /**
     * Creates an initial set of function instances.
     */
    private void initFunctions() {
        for (XosFunctionDescriptor xfd: bundleDescriptor.functions()) {
            functionMap.put(xfd, createFunction(xfd));
        }
    }

    private XosFunction createFunction(XosFunctionDescriptor xfd) {
        XosFunction func;
        switch (xfd) {
            case URL_FILTER:
                func = new UrlFilterFunction();
                break;

            default:
                func = new DefaultXosFunction(xfd);
                break;
        }
        return func;
    }

    /**
     * Returns the function instance for the specified descriptor, or returns
     * null if function is not part of this bundle.
     *
     * @param xfd function descrriptor
     * @return function instance
     */
    public XosFunction findFunction(XosFunctionDescriptor xfd) {
        return functionMap.get(xfd);
    }
}
