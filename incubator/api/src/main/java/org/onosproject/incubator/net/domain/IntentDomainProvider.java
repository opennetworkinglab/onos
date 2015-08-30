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
 */
package org.onosproject.incubator.net.domain;

import com.google.common.annotations.Beta;

import java.util.List;
import java.util.Set;

/**
 * FIXME.
 */
@Beta
public interface IntentDomainProvider {

    /**
     * Requests that the provider attempt to satisfy the intent primitive.
     * The application must apply the context before the intent resource
     * can be used. Request contexts can be explictly cancelled, or they will
     * eventually time out so that resources can be reused.
     *
     * @param domain intent domain for the request
     * @param primitive intent primitive
     * @return request contexts that contain resources to satisfy the intent
     */
    //TODO Consider an iterable and/or holds (only hold one or two reservation(s) at a time)
    List<RequestContext> request(IntentDomain domain, IntentPrimitive primitive);

    /**
     * Request that the provider attempt to modify an existing resource to satisfy
     * a new intent primitive. The application must apply the context before
     * the intent resource can be used.
     *
     * @param resource existing resource
     * @param newPrimitive intent primitive
     * @return request contexts that contain resources to satisfy the intent
     */
    List<RequestContext> modify(IntentResource resource, IntentPrimitive newPrimitive);

    /**
     * Requests that the provider release an intent resource.
     *
     * @param resource intent resource
     */
    void release(IntentResource resource);

    /**
     * Requests that the provider apply the intent resource in the request context.
     *
     * @param context request context
     * @return intent resource that satisfies the intent
     */
    IntentResource apply(RequestContext context);

    /**
     * Requests that the provider cancel the request. Requests that are not applied
     * will be eventually timed out by the provider.
     *
     * @param context request context
     */
    void cancel(RequestContext context);

    /**
     * Returns all intent resources held by the provider.
     *
     * @return set of intent resources
     */
    Set<IntentResource> getResources();
}


