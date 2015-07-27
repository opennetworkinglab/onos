/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onlab.stc;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Computes scenario process flow layout for the Monitor GUI.
 */
public class MonitorLayout {

    public static final int WIDTH = 210;
    public static final int HEIGHT = 30;
    public static final int W_GAP = 40;
    public static final int H_GAP = 50;
    public static final int SLOT_WIDTH = WIDTH + H_GAP;

    private final Compiler compiler;
    private final ProcessFlow flow;

    private Map<Step, Box> boxes = Maps.newHashMap();

    /**
     * Creates a new shared process flow monitor.
     *
     * @param compiler scenario compiler
     */
    MonitorLayout(Compiler compiler) {
        this.compiler = compiler;
        this.flow = compiler.processFlow();

        // Extract the flow and create initial bounding boxes.
        boxes.put(null, new Box(null, 0));
        flow.getVertexes().forEach(this::createBox);

        computeLayout(null, 0, 1);
    }

    // Computes the graph layout giving preference to group associations.
    private void computeLayout(Group group, int absoluteTier, int tier) {
        Box box = boxes.get(group);

        // Find all children of the group, or items with no group if at top.
        Set<Step> children = group != null ? group.children() :
                flow.getVertexes().stream().filter(s -> s.group() == null)
                        .collect(Collectors.toSet());

        children.forEach(s -> visit(s, absoluteTier, 1, group));

        // Figure out what the group root vertexes are.
        Set<Step> roots = findRoots(group);

        // Compute the boxes for each of the roots.
        roots.forEach(s -> updateBox(s, absoluteTier + 1, 1, group));

        // Update the tier and depth of the group bounding box.
        computeTiersAndDepth(group, box, absoluteTier, tier, children);

        // Compute the minimum breadth of this group's bounding box.
        computeBreadth(group, box, children);

        // Compute child placements
        computeChildPlacements(group, box, children);
    }

    // Updates the box for the specified step, given the tier number, which
    // is relative to the parent.
    private Box updateBox(Step step, int absoluteTier, int tier, Group group) {
        Box box = boxes.get(step);
        if (step instanceof Group) {
            computeLayout((Group) step, absoluteTier, tier);
        } else {
            box.setTierAndDepth(absoluteTier, tier, 1, group);
        }

        // Follow the steps downstream of this one.
        follow(step, absoluteTier + box.depth(), box.tier() + box.depth());
        return box;
    }

    // Backwards follows edges leading towards the specified step to visit
    // the source vertex and compute layout of those vertices that had
    // sufficient number of visits to compute their tier.
    private void follow(Step step, int absoluteTier, int tier) {
        Group from = step.group();
        flow.getEdgesTo(step).stream()
                .filter(d -> visit(d.src(), absoluteTier, tier, from))
                .forEach(d -> updateBox(d.src(), absoluteTier, tier, from));
    }

    // Visits each step, records maximum tier and returns true if this
    // was the last expected visit.
    private boolean visit(Step step, int absoluteTier, int tier, Group from) {
        Box box = boxes.get(step);
        return box.visitAndLatchMaxTier(absoluteTier, tier, from);
    }

    // Computes the absolute and relative tiers and the depth of the group
    // bounding box.
    private void computeTiersAndDepth(Group group, Box box,
                                      int absoluteTier, int tier, Set<Step> children) {
        int depth = children.stream().mapToInt(this::bottomMostTier).max().getAsInt();
        box.setTierAndDepth(absoluteTier, tier, depth, group);
    }

    // Returns the bottom-most tier this step occupies relative to its parent.
    private int bottomMostTier(Step step) {
        Box box = boxes.get(step);
        return box.tier() + box.depth();
    }

    // Computes breadth of the specified group.
    private void computeBreadth(Group group, Box box, Set<Step> children) {
        if (box.breadth() == 0) {
            // Scan through all tiers and determine the maximum breadth of each.
            IntStream.range(1, box.depth)
                    .forEach(t -> computeTierBreadth(t, box, children));
            box.latchBreadth(children.stream()
                                     .mapToInt(s -> boxes.get(s).breadth())
                                     .max().getAsInt());
        }
    }

    // Computes tier width.
    private void computeTierBreadth(int t, Box box, Set<Step> children) {
        box.latchBreadth(children.stream().map(boxes::get)
                                 .filter(b -> isSpanningTier(b, t))
                                 .mapToInt(Box::breadth).sum());
    }

    // Computes the actual child box placements relative to the parent using
    // the previously established tier, depth and breadth attributes.
    private void computeChildPlacements(Group group, Box box,
                                        Set<Step> children) {
        // Order the root-nodes in alphanumeric order first.
        List<Box> tierBoxes = Lists.newArrayList(boxesOnTier(1, children));
        tierBoxes.sort((a, b) -> a.step().name().compareTo(b.step().name()));

        // Place the boxes centered on the parent box; left to right.
        int tierBreadth = tierBoxes.stream().mapToInt(Box::breadth).sum();
        int slot = 1;
        for (Box b : tierBoxes) {
            b.updateCenter(1, slot(slot, tierBreadth));
            slot += b.breadth();
        }
    }

    // Returns the horizontal offset off the parent center.
    private int slot(int slot, int tierBreadth) {
        boolean even = tierBreadth % 2 == 0;
        int multiplier = -tierBreadth / 2 + slot - 1;
        return even ? multiplier * SLOT_WIDTH + SLOT_WIDTH / 2 : multiplier * SLOT_WIDTH;
    }

    // Returns a list of all child step boxes that start on the specified tier.
    private List<Box> boxesOnTier(int tier, Set<Step> children) {
        return boxes.values().stream()
                .filter(b -> b.tier() == tier && children.contains(b.step()))
                .collect(Collectors.toList());
    }

    // Determines whether the specified box spans, or occupies a tier.
    private boolean isSpanningTier(Box b, int tier) {
        return (b.depth() == 1 && b.tier() == tier) ||
                (b.tier() <= tier && tier < b.tier() + b.depth());
    }


    // Determines roots of the specified group or of the entire graph.
    private Set<Step> findRoots(Group group) {
        Set<Step> steps = group != null ? group.children() : flow.getVertexes();
        return steps.stream().filter(s -> isRoot(s, group)).collect(Collectors.toSet());
    }

    private boolean isRoot(Step step, Group group) {
        if (step.group() != group) {
            return false;
        }

        Set<Dependency> requirements = flow.getEdgesFrom(step);
        return requirements.stream().filter(r -> r.dst().group() == group)
                .collect(Collectors.toSet()).isEmpty();
    }

    /**
     * Returns the bounding box for the specified step. If null is given, it
     * returns the overall bounding box.
     *
     * @param step step or group; null for the overall bounding box
     * @return bounding box
     */
    public Box get(Step step) {
        return boxes.get(step);
    }

    /**
     * Returns the bounding box for the specified step name. If null is given,
     * it returns the overall bounding box.
     *
     * @param name name of step or group; null for the overall bounding box
     * @return bounding box
     */
    public Box get(String name) {
        return get(name == null ? null : compiler.getStep(name));
    }

    // Creates a bounding box for the specified step or group.
    private void createBox(Step step) {
        boxes.put(step, new Box(step, flow.getEdgesFrom(step).size()));
    }

    /**
     * Bounding box data for a step or group.
     */
    final class Box {

        private Step step;
        private int remainingRequirements;

        private int absoluteTier = 0;
        private int tier;
        private int depth = 1;
        private int breadth;
        private int center, top;

        private Box(Step step, int remainingRequirements) {
            this.step = step;
            this.remainingRequirements = remainingRequirements + 1;
            breadth = step == null || step instanceof Group ? 0 : 1;
        }

        private void latchTiers(int absoluteTier, int tier, Group from) {
            this.absoluteTier = Math.max(this.absoluteTier, absoluteTier);
            if (step == null || step.group() == from) {
                this.tier = Math.max(this.tier, tier);
            }
        }

        public void latchBreadth(int breadth) {
            this.breadth = Math.max(this.breadth, breadth);
        }

        void setTierAndDepth(int absoluteTier, int tier, int depth, Group from) {
            latchTiers(absoluteTier, tier, from);
            this.depth = depth;
        }

        boolean visitAndLatchMaxTier(int absoluteTier, int tier, Group from) {
            latchTiers(absoluteTier, tier, from);
            --remainingRequirements;
            return remainingRequirements == 0;
        }

        Step step() {
            return step;
        }

        public int absoluteTier() {
            return absoluteTier;
        }

        int tier() {
            return tier;
        }

        int depth() {
            return depth;
        }

        int breadth() {
            return breadth;
        }

        int top() {
            return top;
        }

        int center() {
            return center;
        }

        public void updateCenter(int top, int center) {
            this.top = top;
            this.center = center;
        }
    }
}
