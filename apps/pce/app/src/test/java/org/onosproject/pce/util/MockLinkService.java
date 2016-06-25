/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.pce.util;

import org.onosproject.net.Link;
import org.onosproject.net.link.LinkServiceAdapter;
import org.onosproject.net.link.LinkListener;

import java.util.ArrayList;
import java.util.List;

/**
 * Test fixture for the link service.
 */
public class MockLinkService extends LinkServiceAdapter {
    List<Link> links = new ArrayList<>();
    LinkListener listener;

    @Override
    public int getLinkCount() {
        return links.size();
    }

    @Override
    public Iterable<Link> getLinks() {
        return links;
    }

    @Override
    public void addListener(LinkListener listener) {
        this.listener = listener;
    }

    /**
     * Get listener.
     */
    public LinkListener getListener() {
        return listener;
    }

    /**
     * Add link.
     *
     * @param link needs to be added to list
     */
    public void addLink(Link link) {
        links.add(link);
    }

    /**
     * Delete link.
     *
     * @param link needs to be deleted from list
     */
    public void removeLink(Link link) {
        links.remove(link);
    }
}
