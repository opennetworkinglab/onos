/*
 * Copyright 2014-present Open Networking Laboratory
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
package org.onosproject.net.provider;

import static com.google.common.base.Preconditions.checkState;

/**
 * Base implementation of a provider service, which tracks the provider to
 * which it is issued and can be invalidated.
 *
 * @param <P> type of the information provider
 */
public abstract class AbstractProviderService<P extends Provider> implements ProviderService<P> {

    private boolean isValid = true;
    private final P provider;

    /**
     * Creates a provider service on behalf of the specified provider.
     *
     * @param provider provider to which this service is being issued
     */
    protected AbstractProviderService(P provider) {
        this.provider = provider;
    }

    /**
     * Invalidates this provider service.
     */
    public void invalidate() {
        isValid = false;
    }

    /**
     * Checks the validity of this provider service.
     *
     * @throws java.lang.IllegalStateException if the service is no longer valid
     */
    public void checkValidity() {
        checkState(isValid, "Provider service is no longer valid");
    }

    @Override
    public P provider() {
        return provider;
    }

}
