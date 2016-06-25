/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.incubator.rpc;

import org.onosproject.net.provider.ProviderService;

import com.google.common.annotations.Beta;

// Not completely sure if we will make use of this at the moment
// added to follow existing  {@link ProviderRegistry} pattern
@Beta
public interface RemoteServiceContextProviderService
        extends ProviderService<RemoteServiceContextProvider> {

}
