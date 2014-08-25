/**
 *    Copyright (c) 2008 The Board of Trustees of The Leland Stanford Junior
 *    University
 *
 *    Licensed under the Apache License, Version 2.0 (the "License"); you may
 *    not use this file except in compliance with the License. You may obtain
 *    a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *    WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *    License for the specific language governing permissions and limitations
 *    under the License.
 **/

package org.projectfloodlight.openflow.util;

import java.util.LinkedHashMap;

public class LRULinkedHashMap<K, V> extends LinkedHashMap<K, V> {
    private static final long serialVersionUID = -2964986094089626647L;
    protected int maximumCapacity;

    public LRULinkedHashMap(final int initialCapacity, final int maximumCapacity) {
        super(initialCapacity, 0.75f, true);
        this.maximumCapacity = maximumCapacity;
    }

    public LRULinkedHashMap(final int maximumCapacity) {
        super(16, 0.75f, true);
        this.maximumCapacity = maximumCapacity;
    }

    @Override
    protected boolean removeEldestEntry(final java.util.Map.Entry<K, V> eldest) {
        if (this.size() > maximumCapacity)
            return true;
        return false;
    }
}
