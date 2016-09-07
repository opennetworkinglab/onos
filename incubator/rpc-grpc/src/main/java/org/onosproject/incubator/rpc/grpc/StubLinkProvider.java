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
package org.onosproject.incubator.rpc.grpc;

import org.onosproject.net.link.LinkProvider;
import org.onosproject.net.provider.AbstractProvider;
import org.onosproject.net.provider.ProviderId;

import com.google.common.annotations.Beta;
import com.google.common.base.MoreObjects;

/**
 * Stub LinkProvider to be registered on Server-side.
 */
@Beta
final class StubLinkProvider extends AbstractProvider implements LinkProvider {
    protected StubLinkProvider(String scheme) {
        super(new ProviderId(scheme, GrpcRemoteServiceServer.RPC_PROVIDER_NAME));
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id())
                .toString();
    }
}
