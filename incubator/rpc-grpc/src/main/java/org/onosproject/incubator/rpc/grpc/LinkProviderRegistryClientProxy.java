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

import static com.google.common.base.Preconditions.checkNotNull;

import org.onosproject.net.link.LinkProvider;
import org.onosproject.net.link.LinkProviderRegistry;
import org.onosproject.net.link.LinkProviderService;
import org.onosproject.net.provider.AbstractProviderRegistry;

import com.google.common.annotations.Beta;

import io.grpc.Channel;

/**
 * Proxy object to handle LinkProviderRegistry calls.
 */
@Beta
public class LinkProviderRegistryClientProxy
    extends AbstractProviderRegistry<LinkProvider, LinkProviderService>
    implements LinkProviderRegistry {

    private final Channel channel;

    public LinkProviderRegistryClientProxy(Channel channel) {
        this.channel = checkNotNull(channel);
    }

    @Override
    protected LinkProviderService createProviderService(LinkProvider provider) {
        return new LinkProviderServiceClientProxy(provider, channel);
    }

}
