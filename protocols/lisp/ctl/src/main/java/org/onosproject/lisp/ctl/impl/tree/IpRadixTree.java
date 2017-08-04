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

import org.onlab.packet.IpAddress;
import org.onlab.packet.IpAddress.Version;
import org.onlab.packet.IpPrefix;

import java.util.List;

/**
 * A radix tree that stores IP address as a key.
 */
public interface IpRadixTree<V> {

    /**
     * Associates the specified value with the specified IP address in this
     * radix tree. If the radix tree previously contained a mapping for the
     * address, the old value is replaced by the specified value.
     *
     * @param prefix prefix with which the specified value is to be associated
     * @param value   value to be associated with the specified IP address
     * @return The previous value associated with the key,
     * if there was one, otherwise null
     */
    V put(IpPrefix prefix, V value);

    /**
     * If a value is not already associated with the given address in the radix
     * tree, associates the given value with the address; otherwise if an
     * existing value is already associated, returns the existing value and
     * does not overwrite it.
     *
     * This operation is performed atomically.
     *
     * @param prefix prefix with which the specified value is to be associated
     * @param value   value to be associated with the specified IP address
     * @return The existing value associated with the address, if there was one;
     * otherwise null in which case the new value was successfully associated
     */
    V putIfAbsent(IpPrefix prefix, V value);

    /**
     * Removes the mapping for a address from this radix tree if it is present
     * (optional operation). More formally, if this radix tree contains a
     * mapping from address <tt>address</tt> to value <tt>v</tt> such that
     * <code>(address==null ?  address==null : address.equals(k))</code>,
     * that mapping is removed. (The radix tree can contain at most one
     * such mapping.)
     *
     * @param prefix prefix whose mapping is to be removed from the radix tree
     * @return True if a value was removed (and therefore was associated with
     * the address), false if no value was associated/removed
     */
    boolean remove(IpPrefix prefix);

    /**
     * Returns the value associated with the given address (exact match),
     * or returns null if no such value is associated with the address.
     *
     * @param prefix The prefix with which a sought value might be associated
     * @return A value associated with the given address (exact match), or null
     * if no value was associated with the address
     */
    V getValueForExactAddress(IpPrefix prefix);

    /**
     * Returns the value associated with the closest parent address,
     * or returns null if no such value is associated with the address.
     * This method simply tries the exact match at the first time, if there is
     * no associated value, it tries the exact match with the parent IP prefix.
     *
     * @param prefix The prefix with which a sought value might be associated
     * @return A value associated with the closest parent address, or
     * null if no value was associated with the address
     */
    V getValueForClosestParentAddress(IpPrefix prefix);

    /**
     * Returns a lazy collection which returns the set of addresses in the radix
     * tree which start with the given prefix.
     *
     * This is <i>inclusive</i> - if the given prefix is an exact match for a
     * address in the tree, that address is also returned.
     *
     * @param prefix A prefix of sought addresses in the tree
     * @return A set of addresses in the tree which start with the given prefix
     */
    List<IpAddress> getAddressesStartingWith(IpPrefix prefix);

    /**
     * Returns a lazy collection which returns the set of values associated with
     * addresses in the radix tree which start with the given prefix.
     *
     * This is <i>inclusive</i> - if the given prefix is an exact match for a
     * address in the tree, the value associated with that address is
     * also returned.
     *
     * Note that although the same value might originally have been associated
     * with multiple addresses, the set returned does not contain duplicates
     * (as determined by the value objects' implementation of
     * {@link Object#equals(Object)}).
     *
     * @param prefix A prefix of addresses in the tree for which associated
     *               values are sought
     * @return The set of values associated with addresses in the tree which
     * start with the given prefix
     */
    List<V> getValuesForAddressesStartingWith(IpPrefix prefix);

    /**
     * Counts the number of addresses/values stored in the radix tree.
     *
     * In the current implementation, <b>this is an expensive operation</b>,
     * having O(n) time complexity.
     *
     * @param version The version of IP address
     * @return The number of addresses/values stored in the radix tree
     */
    int size(Version version);

    /**
     * Removes all of the mappings from this radix tree (optional operation).
     * The radix tree will be empty after this call returns.
     */
    void clear();
}
