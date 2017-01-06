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
package org.onosproject.cli.net;

import static org.onosproject.net.DeviceId.deviceId;

import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import com.google.common.collect.Iterables;
import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.onlab.packet.MplsLabel;
import org.onlab.packet.VlanId;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.TributarySlot;
import org.onosproject.net.resource.ContinuousResource;
import org.onosproject.net.resource.DiscreteResource;
import org.onosproject.net.resource.Resource;
import org.onosproject.net.resource.Resources;
import org.onosproject.net.resource.ResourceQueryService;

import com.google.common.base.Strings;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.DiscreteDomain;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.Range;
import com.google.common.collect.RangeSet;
import com.google.common.collect.TreeRangeSet;

/**
 * Lists registered resources.
 */
@Command(scope = "onos", name = "resources",
         description = "Lists registered resources")
public class ResourcesCommand extends AbstractShellCommand {

    @Option(name = "-a", aliases = "--available",
            description = "Output available resources only",
            required = false, multiValued = false)
    boolean availablesOnly = false;

    @Option(name = "-s", aliases = "--sort", description = "Sort output",
            required = false, multiValued = false)
    boolean sort = false;

    @Option(name = "-t", aliases = "--typeStrings", description = "List of resource types to be printed",
            required = false, multiValued = true)
    String[] typeStrings = null;

    Set<String> typesToPrint;

    @Argument(index = 0, name = "deviceIdString", description = "Device ID",
              required = false, multiValued = false)
    String deviceIdStr = null;

    @Argument(index = 1, name = "portNumberString", description = "PortNumber",
              required = false, multiValued = false)
    String portNumberStr = null;


    private ResourceQueryService resourceService;

    @Override
    protected void execute() {
        resourceService = get(ResourceQueryService.class);

        if (typeStrings != null) {
            typesToPrint = new HashSet<>(Arrays.asList(typeStrings));
        } else {
            typesToPrint = Collections.emptySet();
        }

        if (deviceIdStr != null && portNumberStr != null) {
            DeviceId deviceId = deviceId(deviceIdStr);
            PortNumber portNumber = PortNumber.fromString(portNumberStr);

            printResource(Resources.discrete(deviceId, portNumber).resource(), 0);
        } else if (deviceIdStr != null) {
            DeviceId deviceId = deviceId(deviceIdStr);

            printResource(Resources.discrete(deviceId).resource(), 0);
        } else {
            printResource(Resource.ROOT, 0);
        }
    }

    private void printResource(Resource resource, int level) {
        // workaround to preserve the original behavior of ResourceService#getRegisteredResources
        Set<Resource> children;
        if (resource instanceof DiscreteResource) {
            children = resourceService.getRegisteredResources(((DiscreteResource) resource).id());
        } else {
            children = Collections.emptySet();
        }

        if (resource.equals(Resource.ROOT)) {
            print("ROOT");
        } else {
            String resourceName = resource.simpleTypeName();
            if (resource instanceof ContinuousResource) {
                if (availablesOnly) {
                    // Get the total resource
                    double total = ((ContinuousResource) resource).value();
                    // Get allocated resource
                    double allocated = resourceService.getResourceAllocations(resource.id()).stream()
                            .mapToDouble(rA -> ((ContinuousResource) rA.resource()).value())
                            .sum();
                    // Difference
                    double difference = total - allocated;
                    print("%s%s: %f", Strings.repeat(" ", level),
                          resourceName, difference);
                } else {
                    print("%s%s: %f", Strings.repeat(" ", level),
                          resourceName,
                          ((ContinuousResource) resource).value());
                }
                // Continuous resource is terminal node, stop here
                return;
            } else {
                String availability = "";
                if (availablesOnly && !children.isEmpty()) {
                    // intermediate nodes cannot be omitted, print availability
                    if (resourceService.isAvailable(resource)) {
                        availability = " ✔";
                    } else {
                        availability = " ✘";
                    }
                }
                String toString = String.valueOf(resource.valueAs(Object.class).orElse(""));
                if (toString.startsWith(resourceName)) {
                    print("%s%s%s", Strings.repeat(" ", level),
                          toString, availability);
                } else {
                    print("%s%s: %s%s", Strings.repeat(" ", level),
                          resourceName,
                          toString, availability);
                }
            }
        }


        // Classify children into aggregatable terminal resources and everything else

        Set<Class<?>> aggregatableTypes = ImmutableSet.<Class<?>>builder()
                .add(VlanId.class)
                .add(MplsLabel.class)
                .build();
        // (last() resource name) -> { Resource }
        Multimap<String, Resource> aggregatables = ArrayListMultimap.create();
        List<Resource> nonAggregatable = new ArrayList<>();

        for (Resource r : children) {
            if (!isPrintTarget(r)) {
                continue;
            }

            if (r instanceof ContinuousResource) {
                // non-aggregatable terminal node
                nonAggregatable.add(r);
            } else if (Iterables.any(aggregatableTypes, r::isTypeOf)) {
                // aggregatable & terminal node
                String simpleName = r.simpleTypeName();
                aggregatables.put(simpleName, r);
            } else {
                nonAggregatable.add(r);
            }
        }

        // print aggregated (terminal)
        aggregatables.asMap().entrySet()
            .forEach(e -> {
                // for each type...
                String resourceName = e.getKey();

                RangeSet<Long> rangeSet = TreeRangeSet.create();

                // aggregate into RangeSet
                e.getValue().stream()
                    .map(res -> {
                            if (res.isTypeOf(VlanId.class)) {
                                return (long) res.valueAs(VlanId.class).get().toShort();
                            } else if (res.isTypeOf(MplsLabel.class)) {
                                return (long) res.valueAs(MplsLabel.class).get().toInt();
                            } else if (res.isTypeOf(TributarySlot.class)) {
                                return res.valueAs(TributarySlot.class).get().index();
                            }
                            // TODO support Lambda (OchSignal types)
                            return 0L;
                        })
                    .map(Range::singleton)
                    .map(range -> range.canonical(DiscreteDomain.longs()))
                    .forEach(rangeSet::add);

                print("%s%s: %s", Strings.repeat(" ", level + 1),
                      resourceName,
                      rangeSet);
            });


        // print non-aggregatables (recurse)
        if (sort) {
            nonAggregatable.stream()
                    .sorted((o1, o2) -> String.valueOf(o1.id()).compareTo(String.valueOf(o2.id())))
                    .forEach(r -> printResource(r, level + 1));
        } else {
            nonAggregatable.forEach(r -> printResource(r, level + 1));
        }
    }

    private boolean isPrintTarget(Resource resource) {
        if (typesToPrint.isEmpty()) {
            return true;
        }

        String resourceName = resource.simpleTypeName();
        if (resource instanceof DiscreteResource) {
            // TODO This distributed store access incurs overhead.
            //      This should be merged with the one in printResource()
            if (!resourceService.getRegisteredResources(((DiscreteResource) resource).id()).isEmpty()) {
                // resource which has children should be printed
                return true;
            }
            if (availablesOnly && !resourceService.isAvailable(resource)) {
                // don't print unavailable discrete resource
                return false;
            }
        } else if (!(resource instanceof ContinuousResource)) {
            log.warn("Unexpected resource class: {}", resource.getClass().getSimpleName());
            return false;
        }

        return typesToPrint.contains(resourceName);
    }
}
