/*
 * Copyright 2017-present Open Networking Laboratory
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
import org.apache.commons.lang3.StringUtils;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpAddress.Version;
import org.onlab.packet.IpPrefix;

import java.util.List;

/**
 * Implements current radix tree that stores IP address as a key.
 */
public class IpConcurrentRadixTree<V> implements IpRadixTree<V> {

    private static final int IPV4_BLOCK_LENGTH = 8;
    private static final int IPV6_BLOCK_LENGTH = 16;

    private static final String IPV4_DELIMITER = ".";
    private static final String IPV6_DELIMITER = ":";

    private static final String IPV4_ZERO = "0";
    private static final String IPV6_SUFFIX = "::";

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
     * Obtains the string formatted IP prefix.
     * For example, if the IP address is 10.1.1.1 and has 16 prefix length,
     * the resulting string is 10.1
     *
     * @param prefix IP prefix
     * @return string formatted IP prefix
     */
    private String getPrefixString(IpPrefix prefix) {
        String addressString = prefix.address().toString();
        StringBuilder sb = new StringBuilder();
        String delimiter = "";
        int numOfBlock = 0;

        if (prefix.isIp4()) {
            delimiter = IPV4_DELIMITER;
            numOfBlock = prefix.prefixLength() / IPV4_BLOCK_LENGTH;
        }

        if (prefix.isIp6()) {
            delimiter = IPV6_DELIMITER;
            numOfBlock = prefix.prefixLength() / IPV6_BLOCK_LENGTH;
        }

        String[] octets = StringUtils.split(addressString, delimiter);

        for (int i = 0; i < numOfBlock; i++) {
            sb.append(octets[i]);

            if (i < numOfBlock - 1) {
                sb.append(delimiter);
            }
        }

        return sb.toString();
    }

    /**
     * Obtains the parent IP prefix of the given IP prefix.
     * For example, if the given IP prefix is 10.1.1.1, the parent IP prefix
     * will be 10.1.1
     *
     * @param prefix IP prefix
     * @return parent IP prefix
     */
    private IpPrefix getParentPrefix(IpPrefix prefix) {
        String addressString = prefix.address().toString();
        int prefixLength = prefix.prefixLength();
        StringBuilder sb = new StringBuilder();
        String delimiter = "";
        String zero = "";
        int blockLength = 0;

        if (prefix.isIp4()) {
            delimiter = IPV4_DELIMITER;
            blockLength = IPV4_BLOCK_LENGTH;
            zero = IPV4_ZERO;
        }

        if (prefix.isIp6()) {
            delimiter = IPV6_DELIMITER;
            blockLength = IPV6_BLOCK_LENGTH;
        }

        String[] octets = StringUtils.split(addressString, delimiter);
        String parentAddressString;
        if (octets.length == 1) {
            return prefix;
        } else {
            prefixLength = prefixLength - blockLength;
            int blockIdx = prefixLength / blockLength;
            for (int i = 0; i < octets.length; i++) {
                if (i < blockIdx) {
                    sb.append(octets[i]);
                    sb.append(delimiter);
                } else {
                    sb.append(zero);
                    if (prefix.isIp4()) {
                        sb.append(delimiter);
                    }
                }
            }

            // ipv6 address prefix typically ends with ::
            if (prefix.isIp6()) {
                sb.append(IPV6_SUFFIX);
            }
            parentAddressString = StringUtils.substring(sb.toString(),
                    0, sb.toString().length() - 1);
            return IpPrefix.valueOf(parentAddressString + "/" + prefixLength);
        }
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

        while (prefix != null) {
            V value = tree.getValueForExactKey(getPrefixString(prefix));
            if (value != null) {
                return value;
            }
            prefix = getParentPrefix(prefix);
        }

        return null;
    }
}