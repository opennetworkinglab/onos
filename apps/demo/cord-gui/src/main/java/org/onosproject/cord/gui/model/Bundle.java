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

import java.util.HashSet;
import java.util.Set;

/**
 * Encapsulates a bundle, including current state.
 */
public class Bundle {
    private final BundleDescriptor bundleDescriptor;
    private final Set<XosFunction> functions;

    /**
     * Constructs a new bundle instance.
     *
     * @param bundleDescriptor the descriptor
     */
    public Bundle(BundleDescriptor bundleDescriptor) {
        this.bundleDescriptor = bundleDescriptor;
        this.functions = initFunctions();
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
        return ImmutableSet.copyOf(functions);
    }

    /**
     * Creates an initial set of function instances.
     *
     * @return initial function instances
     */
    private Set<XosFunction> initFunctions() {
        Set<XosFunction> funcs = new HashSet<XosFunction>();
        for (XosFunctionDescriptor xfd: bundleDescriptor.functions()) {
            funcs.add(createFunction(xfd));
        }
        return funcs;
    }

    private XosFunction createFunction(XosFunctionDescriptor xfd) {
        XosFunction func;
        switch (xfd) {
            case URL_FILTER:
                func = new UrlFilterFunction(xfd);
                break;

            default:
                func = new DefaultXosFunction(xfd);
                break;
        }
        return func;
    }
}
