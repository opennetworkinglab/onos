/*
 * Copyright 2017-present Open Networking Foundation
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
package org.onosproject.lisp.ctl.impl.tree;

import com.google.common.collect.Lists;
import com.googlecode.concurrenttrees.radix.ConcurrentRadixTree;
import com.googlecode.concurrenttrees.radix.RadixTree;
import com.googlecode.concurrenttrees.radix.node.concrete.DefaultCharArrayNodeFactory;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpAddress.Version;
import org.onlab.packet.IpPrefix;

import java.util.List;

import static org.onosproject.lisp.ctl.impl.util.LispMapUtil.getParentPrefix;
import static org.onosproject.lisp.ctl.impl.util.LispMapUtil.getPrefixString;

/**
 * Implements current radix tree that stores IP address as a key.
 */
public class IpConcurrentRadixTree<V> implements IpRadixTree<V> {

    private RadixTree<V> ipv4Tree =
            new ConcurrentRadixTree<>(new DefaultCharArrayNodeFactory());
    private RadixTree<V> ipv6Tree =
            new ConcurrentRadixTree<>(new DefaultCharArrayNodeFactory());

    @Override
    public V put(IpPrefix prefix, V value) {

        String prefixString = getPrefixString(prefix);

        if (prefix.isIp4()) {
            return ipv4Tree.put(prefixString, value);
        }
        if (prefix.isIp6()) {
            return ipv6Tree.put(prefixString, value);
        }

        return null;
    }

    @Override
    public V putIfAbsent(IpPrefix prefix, V value) {

        String prefixString = getPrefixString(prefix);

        if (prefix.isIp4()) {
            return ipv4Tree.put(prefixString, value);
        }

        if (prefix.isIp6()) {
            return ipv6Tree.put(prefixString, value);
        }

        return null;
    }

    @Override
    public boolean remove(IpPrefix prefix) {

        String prefixString = getPrefixString(prefix);

        if (prefix.isIp4()) {
            return ipv4Tree.remove(prefixString);
        }

        if (prefix.isIp6()) {
            return ipv6Tree.remove(prefixString);
        }

        return false;
    }

    @Override
    public V getValueForExactAddress(IpPrefix prefix) {

        String prefixString = getPrefixString(prefix);

        if (prefix.isIp4()) {
            return ipv4Tree.getValueForExactKey(prefixString);
        }

        if (prefix.isIp6()) {
            return ipv6Tree.getValueForExactKey(prefixString);
        }

        return null;
    }

    @Override
    public V getValueForClosestParentAddress(IpPrefix prefix) {

        if (prefix.isIp4()) {
            return getValueForClosestParentAddress(prefix, ipv4Tree);
        }

        if (prefix.isIp6()) {
            return getValueForClosestParentAddress(prefix, ipv6Tree);
        }

        return null;
    }

    @Override
    public List<IpAddress> getAddressesStartingWith(IpPrefix prefix) {

        // TODO: implement later

        return null;
    }

    @Override
    public List<V> getValuesForAddressesStartingWith(IpPrefix prefix) {

        String prefixString = getPrefixString(prefix);

        if (prefix.isIp4()) {
            return Lists.newArrayList(ipv4Tree.getValuesForKeysStartingWith(prefixString));
        }

        if (prefix.isIp6()) {
            return Lists.newArrayList(ipv6Tree.getValuesForKeysStartingWith(prefixString));
        }

        return null;
    }

    @Override
    public int size(Version version) {

        if (version == Version.INET) {
            return ipv4Tree.size();
        }

        if (version == Version.INET6) {
            return ipv6Tree.size();
        }

        return 0;
    }

    @Override
    public void clear() {
        ipv4Tree = new ConcurrentRadixTree<>(new DefaultCharArrayNodeFactory());
        ipv6Tree = new ConcurrentRadixTree<>(new DefaultCharArrayNodeFactory());
    }

    /**
     * Returns the value associated with the closest parent address from a
     * given radix tree, or returns null if no such value is associated
     * with the address.
     *
     * @param prefix IP prefix
     * @param tree a radix tree
     * @return A value associated with the closest parent address, or
     * null if no value was associated with the address
     */
    private V getValueForClosestParentAddress(IpPrefix prefix, RadixTree<V> tree) {

        while (prefix != null && prefix.prefixLength() > 0) {
            V value = tree.getValueForExactKey(getPrefixString(prefix));
            if (value != null) {
                return value;
            }
            prefix = getParentPrefix(prefix);
        }

        return null;
    }
}