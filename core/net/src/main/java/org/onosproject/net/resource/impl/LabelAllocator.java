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
import org.slf4j.Logger;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Helper class which interacts with the ResourceService and provides
 * a unified API to allocate MPLS labels and VLAN Ids.
 */
public final class LabelAllocator {

    private final Logger log = getLogger(getClass());

    /**
     * Defines possible behaviors for the selection of the labels.
     */
    private enum SelectionBehavior {
        /**
         * Random selection.
         */
        RANDOM,
        /**
         * First fit selection.
         */
        FIRST_FIT
    }

    /**
     * Defines possible optimizations for the selection of the labels.
     */
    enum OptimizationBehavior {
        /**
         * Allocator does not try to optimize, it defines a new candidates
         * set at each hop and select label according to the selection strategy.
         */
        NONE,
        /**
         * Allocator enforces same label along the path, it builds a common
         * set of candidate and select a common label according to the selection
         * strategy.
         */
        NO_SWAP,
        /**
         * Allocator try to minimize the swapping of labels along the path. If
         * it is possible try to reuse the label of the previous hop.
         */
        MIN_SWAP
    }

    private ResourceService resourceService;
    private LabelSelection labelSelection;
    private OptimizationBehavior optLabelSelection;

    /**
     * Creates a new label allocator. Random is the default selection behavior.
     * None is the default optimization behavior
     *
     * @param rs the resource service
     */
    public LabelAllocator(ResourceService rs) {
        this.resourceService = checkNotNull(rs);
        this.labelSelection = this.getLabelSelection(SelectionBehavior.RANDOM);
        this.optLabelSelection = OptimizationBehavior.NONE;
    }

    /**
     * Checks if a given string is a valid Selection Behavior.
     *
     * @param value the string to check
     * @return true if value is a valid Selection Behavior, false otherwise
     */
    public static boolean isInSelEnum(String value) {
        for (SelectionBehavior b : SelectionBehavior.values()) {
            if (b.name().equals(value)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if a given string is a valid Optimization Behavior.
     *
     * @param value the string to check
     * @return true if value is a valid Optimization Behavior, false otherwise
     */
    public static boolean isInOptEnum(String value) {
        for (OptimizationBehavior b : OptimizationBehavior.values()) {
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
        if (isInSelEnum(type)) {
            this.labelSelection = this.getLabelSelection(type);
        }
    }

    /**
     * Retrieves the selection behavior.
     *
     * @return the selection behavior in use
     */
    public LabelSelection getLabelSelection() {
        return this.labelSelection;
    }

    /**
     * Changes the optimization behavior.
     *
     * @param type the optimization type
     */
    public void setOptLabelSelection(String type) {
        if (isInOptEnum(type)) {
            this.optLabelSelection = OptimizationBehavior.valueOf(type);
        }
    }

    /**
     * Retrieves the optimization behavior.
     *
     * @return the optimization behavior in use
     */
    public OptimizationBehavior getOptLabelSelection() {
        return this.optLabelSelection;
    }

    /**
     * Returns the label selection behavior, given a behavior type.
     *
     * @param type the behavior type
     * @return the label selection behavior in use
     */
    private LabelSelection getLabelSelection(String type) {
        SelectionBehavior behavior = SelectionBehavior.valueOf(type);
        return this.getLabelSelection(behavior);
    }

    /**
     * Creates a new LabelSelection. Random is the default
     * label selection behavior.
     *
     * @param type the behavior type
     * @return the object implementing the behavior
     */
    private LabelSelection getLabelSelection(SelectionBehavior type) {
        LabelSelection selection;
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

    // Given a link and a encapsulation type, returns a set of candidates
    private Set<Identifier<?>> getCandidates(LinkKey link, EncapsulationType type) {
        // Available ids on src port
        Set<Identifier<?>> availableIDsatSrc = getAvailableIDs(link.src(), type);
        // Available ids on dst port
        Set<Identifier<?>> availableIDsatDst = getAvailableIDs(link.dst(), type);
        // Create the candidate set doing an intersection of the previous sets
        return Sets.intersection(availableIDsatSrc, availableIDsatDst);
    }

    // Implements NONE behavior
    private Map<LinkKey, Identifier<?>> noOptimizeBehavior(Set<LinkKey> links, EncapsulationType type) {
        // Init step
        Map<LinkKey, Identifier<?>> ids = Maps.newHashMap();
        Set<Identifier<?>> candidates;
        Identifier<?> selected;
        // Iterates for each link selecting a label in the candidate set
        for (LinkKey link : links) {
            // Get candidates set for the current link
            candidates = getCandidates(link, type);
            // Select a label for the current link
            selected = labelSelection.select(candidates);
            // If candidates is empty, selected is null
            if (selected == null) {
                log.warn("No labels for {}", link);
                return Collections.emptyMap();
            }
            // Selected is associated to link
            ids.put(link, selected);
        }
        return ids;
    }

    // Implements suggestedIdentifier behavior
    private Map<LinkKey, Identifier<?>> suggestedIdentifierBehavior(Set<LinkKey> links,
                                                           EncapsulationType type,
                                                           Identifier<?> suggested) {
        // Init step
        Map<LinkKey, Identifier<?>> ids = Maps.newHashMap();
        Set<Identifier<?>> candidates;
        Identifier<?> selected = null;

        // Iterates for each link selecting a label in the candidate set
        // Select the suggested if available on the whole path
        for (LinkKey link : links) {
            // Get candidates set for the current link
            candidates = getCandidates(link, type);

            // Select the suggested if included in the candidates
            // Otherwise select an other label for the current link
            if (candidates.contains(suggested)) {
                selected = suggested;
            } else {
                // If candidates is empty or does not contain suggested
                log.warn("Suggested label {} is not available on link {}", suggested, link);
                return Collections.emptyMap();
            }
            // Selected is associated to link
            ids.put(link, selected);
        }
        return ids;
    }

    // Implements NO_SWAP behavior
    private Map<LinkKey, Identifier<?>> noSwapBehavior(Set<LinkKey> links, EncapsulationType type) {
        // Init steps
        Map<LinkKey, Identifier<?>> ids = Maps.newHashMap();
        Identifier<?> selected;
        Set<Identifier<?>> candidates = null;
        Set<Identifier<?>> linkCandidates;
        // Iterates for each link building the candidate set
        for (LinkKey link : links) {
            // Get candidates set for the current link
            linkCandidates = getCandidates(link, type);
            // Warm up
            if (candidates == null) {
                candidates = linkCandidates;
            // Build step by step the intersection
            } else {
                candidates = Sets.intersection(candidates, linkCandidates);
            }
        }
        // Pick a label according to the defined strategy
        selected = labelSelection.select(candidates);
        if (selected == null) {
            // If there are no candidates, exit. This will throw a compile exception
            log.warn("No common label for path");
            return Collections.emptyMap();
        }
        // For each link create an entry
        links.forEach(linkKey -> ids.put(linkKey, selected));
        return ids;
    }

    // Implements MIN_SWAP behavior
    private Map<LinkKey, Identifier<?>> minSwapBehavior(Set<LinkKey> links, EncapsulationType type) {
        // Init step
        Map<LinkKey, Identifier<?>> ids = Maps.newHashMap();
        Set<Identifier<?>> candidates;
        Identifier<?> selected = null;
        // Iterates for each link selecting a label in the candidate set
        for (LinkKey link : links) {
            // Get candidates set for the current link
            candidates = getCandidates(link, type);
            // If we are in the first link or selected is not available
            if (selected == null || !candidates.contains(selected)) {
                // Select a label for the current link
                selected = labelSelection.select(candidates);
                // If candidates is empty, selected is null
                if (selected == null) {
                    log.warn("No labels for {}", link);
                    return Collections.emptyMap();
                }
            }
            // Selected is associated to link
            ids.put(link, selected);
        }
        return ids;
    }

    /**
     * Looks for available Ids.
     *
     * @param links the links where to look for Ids
     * @param  type the encapsulation type
     * @return the mappings between key and id
     */
    private Map<LinkKey, Identifier<?>> findAvailableIDs(Set<LinkKey> links,
                                                         EncapsulationType type,
                                                         Optional<Identifier<?>> suggestedIdentifier) {
        // Init step
        Map<LinkKey, Identifier<?>> ids;

        //Use suggested identifier if possible
        if (suggestedIdentifier.isPresent()) {
            ids = suggestedIdentifierBehavior(links, type, suggestedIdentifier.get());

            if (!ids.isEmpty()) {
                return ids;
            }
        }

        // Performs label selection according to the defined optimization behavior
        switch (optLabelSelection) {
            // No swapping of the labels
            case NO_SWAP:
                ids = noSwapBehavior(links, type);
                break;
            // Swapping is minimized
            case MIN_SWAP:
                ids = minSwapBehavior(links, type);
                break;
            // No optimizations are in place
            case NONE:
            default:
                ids = noOptimizeBehavior(links, type);
        }
        // Done exit
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
     * @param suggestedIdentifier used if available
     * @return the list of links and associated labels
     */
    public Map<LinkKey, Identifier<?>> assignLabelToLinks(Set<Link> links,
                                                          ResourceConsumer resourceConsumer,
                                                          EncapsulationType type,
                                                          Optional<Identifier<?>> suggestedIdentifier) {
        // To preserve order of the links. This is important for MIN_SWAP behavior
        Set<LinkKey> linkRequest = links.stream()
                .map(LinkKey::linkKey)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Map<LinkKey, Identifier<?>> availableIds = findAvailableIDs(linkRequest, type, suggestedIdentifier);
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
        // To preserve order of the links. This is important for MIN_SWAP behavior
        Set<LinkKey> linkRequest = links.stream()
                .map(LinkKey::linkKey)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Map<LinkKey, Identifier<?>> availableIds = findAvailableIDs(linkRequest, type, Optional.empty());
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
     * @param suggestedIdentifier used if available
     * @return the list of ports and associated labels
     */
    public Map<ConnectPoint, Identifier<?>> assignLabelToPorts(Set<Link> links,
                                                               ResourceConsumer resourceConsumer,
                                                               EncapsulationType type,
                                                               Optional<Identifier<?>> suggestedIdentifier) {
        Map<LinkKey, Identifier<?>> allocation = this.assignLabelToLinks(links,
                resourceConsumer,
                type,
                suggestedIdentifier);
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
         * the first fit selection algorithm.
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
