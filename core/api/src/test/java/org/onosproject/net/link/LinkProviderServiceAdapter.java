/*
 * Copyright 2016-present Open Networking Foundation
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
package org.onosproject.net.link;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.LinkKey;
import org.onosproject.net.provider.AbstractProviderService;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class LinkProviderServiceAdapter
        extends AbstractProviderService<LinkProvider>
        implements LinkProviderService {

    List<DeviceId> vanishedDpid = Lists.newLinkedList();
    List<Long> vanishedPort = Lists.newLinkedList();
    Map<DeviceId, DeviceId> discoveredLinks = Maps.newHashMap();
    Map<LinkKey, LinkDescription> discoveredLinkDescriptions = new HashMap<>();

    protected LinkProviderServiceAdapter(LinkProvider provider) {
        super(provider);
    }

    @Override
    public void linkDetected(LinkDescription linkDescription) {
        LinkKey key = LinkKey.linkKey(linkDescription.src(), linkDescription.dst());
        discoveredLinkDescriptions.put(key, linkDescription);
        DeviceId sDid = linkDescription.src().deviceId();
        DeviceId dDid = linkDescription.dst().deviceId();
        discoveredLinks.put(sDid, dDid);
    }

    @Override
    public void linkVanished(LinkDescription linkDescription) {
        LinkKey key = LinkKey.linkKey(linkDescription.src(), linkDescription.dst());
        discoveredLinkDescriptions.remove(key);
    }

    @Override
    public void linksVanished(ConnectPoint connectPoint) {
        vanishedPort.add(connectPoint.port().toLong());

    }

    @Override
    public void linksVanished(DeviceId deviceId) {
        vanishedDpid.add(deviceId);
    }

    public List<DeviceId> vanishedDpid() {
        return vanishedDpid;
    }

    public List<Long> vanishedPort() {
        return vanishedPort;
    }

    public Map<DeviceId, DeviceId> discoveredLinks() {
        return discoveredLinks;
    }

    public Map<LinkKey, LinkDescription> discoveredLinkDescriptions() {
        return discoveredLinkDescriptions;
    }
}
