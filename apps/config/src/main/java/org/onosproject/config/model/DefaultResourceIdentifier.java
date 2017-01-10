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
package org.onosproject.config.model;

/**
 * Created by sdn on 12/15/16.
 */
public class DefaultResourceIdentifier<V> implements ResourceIdentifier     {
    NodeKey key;
    ResourceIdentifier next;

    public DefaultResourceIdentifier(String nm, String nmspc) {
        this.key = new NodeKey(nm, nmspc);
        this.next = null;
    }

    public DefaultResourceIdentifier(ResourceIdentifier parent, NodeKey ckey) {
        this.key = parent.nodeKey();
        //new NodeKey(parent.nodeKey().schemaId.name, parent.nodeKey().schemaId.nameSpace);
        this.next = new DefaultResourceIdentifier(ckey);
    }

    public DefaultResourceIdentifier(ResourceIdentifier parent, ResourceIdentifier child) {
        this.key = parent.nodeKey();
        this.next = child;
    }

    public DefaultResourceIdentifier(NodeKey nkey) {
        this.key = nkey;
        this.next = null;
    }

    /*public void setChild(NodeKey ckey) {
        this.next = new DefaultResourceIdentifier(ckey);
    }*/

    @Override
    public NodeKey nodeKey() {
        return this.key;
    }

    @Override
    public ResourceIdentifier descendentIdentifier() {
        return this.next;
    }

    @Override
    public String getBase() {
        return this.key.schemaId.name.concat("#").concat(this.key.schemaId.nameSpace);
    }

    @Override
    public String asString() {
        String base = getBase();
        ResourceIdentifier desc = next;
        while (desc != null) {
            base.concat(".").concat(desc.getBase());
            desc = desc.descendentIdentifier();
        }
        return base;
    }
}