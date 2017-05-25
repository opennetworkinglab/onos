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

package org.onosproject.net.resource.impl;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.lang.math.RandomUtils;
import org.onlab.packet.MplsLabel;
import org.onlab.packet.VlanId;
import org.onlab.util.Identifier;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.EncapsulationType;
import org.onosproject.net.Link;
import org.onosproject.net.LinkKey;
import org.onosproject.net.resource.Resource;
import org.onosproject.net.resource.ResourceAllocation;
import org.onosproject.net.resource.ResourceConsumer;
import org.onosproject.net.resource.ResourceService;
import org.onosproject.net.resource.Resources;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Helper class which interacts with the ResourceService and provides
 * a unified API to allocate MPLS labels and VLAN Ids.
 */
public final class LabelAllocator {

    private enum Behavior {
        /**
         * Random selection.
         */
        RANDOM,
        /**
         * First fit selection.
         */
        FIRST_FIT
    }

    private static final Behavior[] BEHAVIORS = Behavior.values();

    private ResourceService resourceService;
    private LabelSelection labelSelection;

    /**
     * Creates a new label allocator. Random is the
     * default behavior.
     *
     * @param rs the resource service
     */
    public LabelAllocator(ResourceService rs) {
        this.resourceService = checkNotNull(rs);
        this.labelSelection = this.getLabelSelection(Behavior.RANDOM);
    }

    /**
     * Checks if a given string is a valid Behavior.
     *
     * @param value the string to check
     * @return true if value is a valid Behavior, false otherwise
     */
    public static boolean isInEnum(String value) {
        for (Behavior b : BEHAVIORS) {
            if (b.name().equals(value)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Changes the selection behavior.
     *
     * @param type the behavior type
     */
    public void setLabelSelection(String type) {
        if (isInEnum(type)) {
            this.labelSelection = this.getLabelSelection(type);
        }
    }

    /**
     * Retrieves the label selection behavior.
     *
     * @return the label selection behavior in use
     */
    public LabelSelection getLabelSelection() {
        return this.labelSelection;
    }

    /**
     * Returns the label selection behavior, given a behavior type.
     *
     * @param type the behavior type
     * @return the label selection behavior in use
     */
    private LabelSelection getLabelSelection(String type) {
        Behavior behavior = Behavior.valueOf(type);
        return this.getLabelSelection(behavior);
    }

    /**
     * Creates a new LabelSelection. Random is
     * the default label selection behavior.
     *
     * @param type the behavior type
     * @return the object implementing the behavior
     */
    private LabelSelection getLabelSelection(Behavior type) {
        LabelSelection selection = null;
        switch (type) {
            case FIRST_FIT:
                selection = new FirstFitSelection();
                break;
            case RANDOM:
            default:
                selection = new RandomSelection();
                break;
        }
        return selection;
    }

    /**
     * Looks for available Ids.
     *
     * @param links the links where to look for Ids
     * @param  type the encapsulation type
     * @return the mappings between key and id
     */
    private Map<LinkKey, Identifier<?>> findAvailableIDs(Set<LinkKey> links, EncapsulationType type) {

        Map<LinkKey, Identifier<?>> ids = Maps.newHashMap();
        for (LinkKey link : links) {
            Set<Identifier<?>> availableIDsatSrc = getAvailableIDs(link.src(), type);
            Set<Identifier<?>> availableIDsatDst = getAvailableIDs(link.dst(), type);
            Set<Identifier<?>> common = Sets.intersection(availableIDsatSrc, availableIDsatDst);
            if (common.isEmpty()) {
                continue;
            }
            Identifier<?> selected = labelSelection.select(common);
            if (selected == null) {
                continue;
            }
            ids.put(link, selected);
        }
        return ids;
    }

    /**
     * Looks for available Ids associated to the given connection point.
     *
     * @param cp the connection point
     * @param type the type of Id
     * @return the set of available Ids
     */
    private Set<Identifier<?>> getAvailableIDs(ConnectPoint cp, EncapsulationType type) {
        return resourceService.getAvailableResourceValues(
                Resources.discrete(cp.deviceId(), cp.port()).id(), getEncapsulationClass(type)
        );
    }

    /**
     * Method to map the encapsulation type to identifier class.
     * VLAN is the default encapsulation.
     *
     * @param type the type of encapsulation
     * @return the id class
     */
    private Class getEncapsulationClass(EncapsulationType type) {
        Class idType;
        switch (type) {
            case MPLS:
                idType = MplsLabel.class;
                break;
            case VLAN:
            default:
                idType = VlanId.class;
        }
        return idType;
    }

    /**
     * Allocates labels and associates them to links.
     *
     * @param links the links where labels will be allocated
     * @param resourceConsumer the resource consumer
     * @param type the encapsulation type
     * @return the list of links and associated labels
     */
    public Map<LinkKey, Identifier<?>> assignLabelToLinks(Set<Link> links,
                                                          ResourceConsumer resourceConsumer,
                                                          EncapsulationType type) {
        Set<LinkKey> linkRequest = links.stream()
                .map(LinkKey::linkKey)
                .collect(Collectors.toSet());

        Map<LinkKey, Identifier<?>> availableIds = findAvailableIDs(linkRequest, type);
        if (availableIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Set<Resource> resources = availableIds.entrySet().stream()
                .flatMap(x -> Stream.of(
                        Resources.discrete(
                                x.getKey().src().deviceId(),
                                x.getKey().src().port(),
                                x.getValue()
                        ).resource(),
                        Resources.discrete(
                                x.getKey().dst().deviceId(),
                                x.getKey().dst().port(),
                                x.getValue()
                        ).resource()
                ))
                .collect(Collectors.toSet());

        List<ResourceAllocation> allocations = resourceService.allocate(resourceConsumer,
                                                                        ImmutableList.copyOf(resources));

        if (allocations.isEmpty()) {
            return Collections.emptyMap();
        }

        return ImmutableMap.copyOf(availableIds);
    }

    /**
     * Allocates labels and associates them to source
     * and destination ports of a link.
     *
     * @param links the links on which labels will be reserved
     * @param resourceConsumer the resource consumer
     * @param type the encapsulation type
     * @return the list of ports and associated labels
     */
    public Map<ConnectPoint, Identifier<?>> assignLabelToPorts(Set<Link> links,
                                                               ResourceConsumer resourceConsumer,
                                                               EncapsulationType type) {
        Map<LinkKey, Identifier<?>> allocation = this.assignLabelToLinks(links,
                                                                         resourceConsumer,
                                                                         type);
        if (allocation.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<ConnectPoint, Identifier<?>> finalAllocation = Maps.newHashMap();
        allocation.forEach((link, value) -> {
            finalAllocation.putIfAbsent(link.src(), value);
            finalAllocation.putIfAbsent(link.dst(), value);
        });
        return ImmutableMap.copyOf(finalAllocation);
    }

    /**
     * Interface for selection algorithms of the labels.
     */
    public interface LabelSelection {

        /**
         * Picks an element from values using a particular algorithm.
         *
         * @param values the values to select from
         * @return the selected identifier if values are present, null otherwise
         */
        Identifier<?> select(Set<Identifier<?>> values);

    }

    /**
     * Random label selection.
     */
    public static class RandomSelection implements LabelSelection {

        /**
         * Selects an identifier from a given set of values using
         * the random selection algorithm.
         *
         * @param values the values to select from
         * @return the selected identifier if values are present, null otherwise
         */
        @Override
        public Identifier<?> select(Set<Identifier<?>> values) {
            if (!values.isEmpty()) {
                int size = values.size();
                int index = RandomUtils.nextInt(size);
                return Iterables.get(values, index);
            }
            return null;
        }
    }

    /**
     * First fit label selection.
     */
    public static class FirstFitSelection implements LabelSelection {

        /**
         * Selects an identifier from a given set of values using
         * the first fir selection algorithm.
         *
         * @param values the values to select from
         * @return the selected identifier if values are present, null otherwise.
         */
        @Override
        public Identifier<?> select(Set<Identifier<?>> values) {
            if (!values.isEmpty()) {
                return values.iterator().next();
            }
            return null;
        }
    }

}
