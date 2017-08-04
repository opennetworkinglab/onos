/*
 * Copyright 2015-present Open Networking Foundation
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
package org.onosproject.net.flowobjective.impl;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test FlowObjectiveCompositionTree.
 */
@Ignore
public class FlowObjectiveCompositionTreeTest {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Before
    public void setUp() {}

    @After
    public void tearDown() {}

    /*@Test
    public void testParallelComposition() {
        FlowObjectiveCompositionTree policyTree = FlowObjectiveCompositionUtil.parsePolicyString("31+32");

        {
            TrafficSelector selector = DefaultTrafficSelector.builder()
                    .matchIPSrc(IpPrefix.valueOf("1.0.0.0/24"))
                    .build();

            TrafficTreatment treatment = DefaultTrafficTreatment.builder().build();

            ForwardingObjective forwardingObjective = DefaultForwardingObjective.builder()
                    .fromApp(new DefaultApplicationId(31, "a"))
                    .makePermanent()
                    .withFlag(ForwardingObjective.Flag.VERSATILE)
                    .withPriority(1)
                    .withSelector(selector)
                    .withTreatment(treatment)
                    .add();

            helper(policyTree, forwardingObjective);
        }

        {
            TrafficSelector selector = DefaultTrafficSelector.builder().build();

            TrafficTreatment treatment = DefaultTrafficTreatment.builder().build();

            ForwardingObjective forwardingObjective = DefaultForwardingObjective.builder()
                    .fromApp(new DefaultApplicationId(31, "a"))
                    .makePermanent()
                    .withFlag(ForwardingObjective.Flag.VERSATILE)
                    .withPriority(0)
                    .withSelector(selector)
                    .withTreatment(treatment)
                    .add();

            helper(policyTree, forwardingObjective);
        }


        {
            TrafficSelector selector = DefaultTrafficSelector.builder()
                    .matchIPDst(IpPrefix.valueOf("2.0.0.1/32"))
                    .build();

            TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                    .setOutput(PortNumber.portNumber(1))
                    .build();

            ForwardingObjective forwardingObjective = DefaultForwardingObjective.builder()
                    .fromApp(new DefaultApplicationId(32, "b"))
                    .makePermanent()
                    .withFlag(ForwardingObjective.Flag.VERSATILE)
                    .withPriority(1)
                    .withSelector(selector)
                    .withTreatment(treatment)
                    .add();

            helper(policyTree, forwardingObjective);
        }

        {
            TrafficSelector selector = DefaultTrafficSelector.builder()
                    .matchIPDst(IpPrefix.valueOf("2.0.0.2/32"))
                    .build();

            TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                    .setOutput(PortNumber.portNumber(2))
                    .build();

            ForwardingObjective forwardingObjective = DefaultForwardingObjective.builder()
                    .fromApp(new DefaultApplicationId(32, "b"))
                    .makePermanent()
                    .withFlag(ForwardingObjective.Flag.VERSATILE)
                    .withPriority(1)
                    .withSelector(selector)
                    .withTreatment(treatment)
                    .add();
            helper(policyTree, forwardingObjective);
        }

        {
            TrafficSelector selector = DefaultTrafficSelector.builder().build();

            TrafficTreatment treatment = DefaultTrafficTreatment.builder().build();

            ForwardingObjective forwardingObjective = DefaultForwardingObjective.builder()
                    .fromApp(new DefaultApplicationId(32, "b"))
                    .makePermanent()
                    .withFlag(ForwardingObjective.Flag.VERSATILE)
                    .withPriority(0)
                    .withSelector(selector)
                    .withTreatment(treatment)
                    .add();

            helper(policyTree, forwardingObjective);
        }

        System.out.println("---------- Parallel ----------");
        for (ForwardingObjective fo : policyTree.forwardTable.getForwardingObjectives()) {
            System.out.println(forwardingObjectiveToString(fo));
        }

        {
            TrafficSelector selector = DefaultTrafficSelector.builder()
                    .matchIPDst(IpPrefix.valueOf("2.0.0.3/32"))
                    .build();

            TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                    .setOutput(PortNumber.portNumber(3))
                    .build();

            ForwardingObjective forwardingObjective = DefaultForwardingObjective.builder()
                    .fromApp(new DefaultApplicationId(32, "b"))
                    .makePermanent()
                    .withFlag(ForwardingObjective.Flag.VERSATILE)
                    .withPriority(1)
                    .withSelector(selector)
                    .withTreatment(treatment)
                    .add();
            helper(policyTree, forwardingObjective);
        }

        System.out.println("---------- Parallel ----------");
        for (ForwardingObjective fo : policyTree.forwardTable.getForwardingObjectives()) {
            System.out.println(forwardingObjectiveToString(fo));
        }

        {
            TrafficSelector selector = DefaultTrafficSelector.builder()
                    .matchIPDst(IpPrefix.valueOf("2.0.0.3/32"))
                    .build();

            TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                    .setOutput(PortNumber.portNumber(3))
                    .build();

            ForwardingObjective forwardingObjective = DefaultForwardingObjective.builder()
                    .fromApp(new DefaultApplicationId(32, "b"))
                    .makePermanent()
                    .withFlag(ForwardingObjective.Flag.VERSATILE)
                    .withPriority(1)
                    .withSelector(selector)
                    .withTreatment(treatment)
                    .remove();
            helper(policyTree, forwardingObjective);
        }

        {
            TrafficSelector selector = DefaultTrafficSelector.builder()
                    .matchIPSrc(IpPrefix.valueOf("1.0.0.0/24"))
                    .build();

            TrafficTreatment treatment = DefaultTrafficTreatment.builder().build();

            ForwardingObjective forwardingObjective = DefaultForwardingObjective.builder()
                    .fromApp(new DefaultApplicationId(31, "a"))
                    .makePermanent()
                    .withFlag(ForwardingObjective.Flag.VERSATILE)
                    .withPriority(1)
                    .withSelector(selector)
                    .withTreatment(treatment)
                    .remove();

            helper(policyTree, forwardingObjective);
        }

        {
            TrafficSelector selector = DefaultTrafficSelector.builder().build();

            TrafficTreatment treatment = DefaultTrafficTreatment.builder().build();

            ForwardingObjective forwardingObjective = DefaultForwardingObjective.builder()
                    .fromApp(new DefaultApplicationId(31, "a"))
                    .makePermanent()
                    .withFlag(ForwardingObjective.Flag.VERSATILE)
                    .withPriority(0)
                    .withSelector(selector)
                    .withTreatment(treatment)
                    .remove();

            helper(policyTree, forwardingObjective);
        }

        System.out.println("---------- Parallel ----------");
        for (ForwardingObjective fo : policyTree.forwardTable.getForwardingObjectives()) {
            System.out.println(forwardingObjectiveToString(fo));
        }
    }

    @Test
    public void testSequentialComposition() {
        FlowObjectiveCompositionTree policyTree = FlowObjectiveCompositionUtil.parsePolicyString("31>32");

        {
            TrafficSelector selector = DefaultTrafficSelector.builder()
                    .matchIPSrc(IpPrefix.valueOf("0.0.0.0/2"))
                    .matchIPDst(IpPrefix.valueOf("3.0.0.0/32"))
                    .build();

            TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                    .setIpDst(IpAddress.valueOf("2.0.0.1"))
                    .build();

            ForwardingObjective forwardingObjective = DefaultForwardingObjective.builder()
                    .fromApp(new DefaultApplicationId(31, "a"))
                    .makePermanent()
                    .withFlag(ForwardingObjective.Flag.VERSATILE)
                    .withPriority(3)
                    .withSelector(selector)
                    .withTreatment(treatment)
                    .add();

            helper(policyTree, forwardingObjective);
        }

        {
            TrafficSelector selector = DefaultTrafficSelector.builder()
                    .matchIPDst(IpPrefix.valueOf("3.0.0.0/32"))
                    .build();

            TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                    .setIpDst(IpAddress.valueOf("2.0.0.2"))
                    .build();

            ForwardingObjective forwardingObjective = DefaultForwardingObjective.builder()
                    .fromApp(new DefaultApplicationId(31, "a"))
                    .makePermanent()
                    .withFlag(ForwardingObjective.Flag.VERSATILE)
                    .withPriority(1)
                    .withSelector(selector)
                    .withTreatment(treatment)
                    .add();

            helper(policyTree, forwardingObjective);
        }

        {
            TrafficSelector selector = DefaultTrafficSelector.builder().build();

            TrafficTreatment treatment = DefaultTrafficTreatment.builder().build();

            ForwardingObjective forwardingObjective = DefaultForwardingObjective.builder()
                    .fromApp(new DefaultApplicationId(31, "a"))
                    .makePermanent()
                    .withFlag(ForwardingObjective.Flag.VERSATILE)
                    .withPriority(0)
                    .withSelector(selector)
                    .withTreatment(treatment)
                    .add();

            helper(policyTree, forwardingObjective);
        }

        {
            TrafficSelector selector = DefaultTrafficSelector.builder()
                    .matchIPDst(IpPrefix.valueOf("2.0.0.1/32"))
                    .build();

            TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                    .setOutput(PortNumber.portNumber(1))
                    .build();

            ForwardingObjective forwardingObjective = DefaultForwardingObjective.builder()
                    .fromApp(new DefaultApplicationId(32, "b"))
                    .makePermanent()
                    .withFlag(ForwardingObjective.Flag.VERSATILE)
                    .withPriority(1)
                    .withSelector(selector)
                    .withTreatment(treatment)
                    .add();

            helper(policyTree, forwardingObjective);
        }

        {
            TrafficSelector selector = DefaultTrafficSelector.builder()
                    .matchIPDst(IpPrefix.valueOf("2.0.0.2/32"))
                    .build();

            TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                    .setOutput(PortNumber.portNumber(2))
                    .build();

            ForwardingObjective forwardingObjective = DefaultForwardingObjective.builder()
                    .fromApp(new DefaultApplicationId(32, "b"))
                    .makePermanent()
                    .withFlag(ForwardingObjective.Flag.VERSATILE)
                    .withPriority(1)
                    .withSelector(selector)
                    .withTreatment(treatment)
                    .add();
            helper(policyTree, forwardingObjective);
        }

        {
            TrafficSelector selector = DefaultTrafficSelector.builder().build();

            TrafficTreatment treatment = DefaultTrafficTreatment.builder().build();

            ForwardingObjective forwardingObjective = DefaultForwardingObjective.builder()
                    .fromApp(new DefaultApplicationId(32, "b"))
                    .makePermanent()
                    .withFlag(ForwardingObjective.Flag.VERSATILE)
                    .withPriority(0)
                    .withSelector(selector)
                    .withTreatment(treatment)
                    .add();

            helper(policyTree, forwardingObjective);
        }

        System.out.println("---------- Sequential ----------");
        for (ForwardingObjective fo : policyTree.forwardTable.getForwardingObjectives()) {
            System.out.println(forwardingObjectiveToString(fo));
        }

        {
            TrafficSelector selector = DefaultTrafficSelector.builder()
                    .matchIPDst(IpPrefix.valueOf("2.0.0.3/32"))
                    .build();

            TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                    .setOutput(PortNumber.portNumber(3))
                    .build();

            ForwardingObjective forwardingObjective = DefaultForwardingObjective.builder()
                    .fromApp(new DefaultApplicationId(32, "b"))
                    .makePermanent()
                    .withFlag(ForwardingObjective.Flag.VERSATILE)
                    .withPriority(1)
                    .withSelector(selector)
                    .withTreatment(treatment)
                    .add();

            helper(policyTree, forwardingObjective);
        }

        {
            TrafficSelector selector = DefaultTrafficSelector.builder()
                    .matchIPSrc(IpPrefix.valueOf("0.0.0.0/1"))
                    .matchIPDst(IpPrefix.valueOf("3.0.0.0/32"))
                    .build();

            TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                    .setIpDst(IpAddress.valueOf("2.0.0.3"))
                    .build();

            ForwardingObjective forwardingObjective = DefaultForwardingObjective.builder()
                    .fromApp(new DefaultApplicationId(31, "a"))
                    .makePermanent()
                    .withFlag(ForwardingObjective.Flag.VERSATILE)
                    .withPriority(3)
                    .withSelector(selector)
                    .withTreatment(treatment)
                    .add();

            helper(policyTree, forwardingObjective);
        }

        System.out.println("---------- Sequential ----------");
        for (ForwardingObjective fo : policyTree.forwardTable.getForwardingObjectives()) {
            System.out.println(forwardingObjectiveToString(fo));
        }

        {
            TrafficSelector selector = DefaultTrafficSelector.builder()
                    .matchIPDst(IpPrefix.valueOf("2.0.0.3/32"))
                    .build();

            TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                    .setOutput(PortNumber.portNumber(3))
                    .build();

            ForwardingObjective forwardingObjective = DefaultForwardingObjective.builder()
                    .fromApp(new DefaultApplicationId(32, "b"))
                    .makePermanent()
                    .withFlag(ForwardingObjective.Flag.VERSATILE)
                    .withPriority(1)
                    .withSelector(selector)
                    .withTreatment(treatment)
                    .remove();

            helper(policyTree, forwardingObjective);
        }

        {
            TrafficSelector selector = DefaultTrafficSelector.builder()
                    .matchIPSrc(IpPrefix.valueOf("0.0.0.0/1"))
                    .matchIPDst(IpPrefix.valueOf("3.0.0.0/32"))
                    .build();

            TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                    .setIpDst(IpAddress.valueOf("2.0.0.3"))
                    .build();

            ForwardingObjective forwardingObjective = DefaultForwardingObjective.builder()
                    .fromApp(new DefaultApplicationId(31, "a"))
                    .makePermanent()
                    .withFlag(ForwardingObjective.Flag.VERSATILE)
                    .withPriority(3)
                    .withSelector(selector)
                    .withTreatment(treatment)
                    .remove();

            helper(policyTree, forwardingObjective);
        }

        System.out.println("---------- Sequential ----------");
        for (ForwardingObjective fo : policyTree.forwardTable.getForwardingObjectives()) {
            System.out.println(forwardingObjectiveToString(fo));
        }
    }

    @Test
    public void testOverrideComposition() {
        FlowObjectiveCompositionTree policyTree = FlowObjectiveCompositionUtil.parsePolicyString("31/32");

        {
            TrafficSelector selector = DefaultTrafficSelector.builder()
                    .matchIPSrc(IpPrefix.valueOf("1.0.0.0/32"))
                    .matchIPDst(IpPrefix.valueOf("2.0.0.1/32"))
                    .build();

            TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                    .setOutput(PortNumber.portNumber(3))
                    .build();

            ForwardingObjective forwardingObjective = DefaultForwardingObjective.builder()
                    .fromApp(new DefaultApplicationId(31, "a"))
                    .makePermanent()
                    .withFlag(ForwardingObjective.Flag.VERSATILE)
                    .withPriority(1)
                    .withSelector(selector)
                    .withTreatment(treatment)
                    .add();

            helper(policyTree, forwardingObjective);
        }

        {
            TrafficSelector selector = DefaultTrafficSelector.builder()
                    .matchIPDst(IpPrefix.valueOf("2.0.0.1/32"))
                    .build();

            TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                    .setOutput(PortNumber.portNumber(1))
                    .build();

            ForwardingObjective forwardingObjective = DefaultForwardingObjective.builder()
                    .fromApp(new DefaultApplicationId(32, "b"))
                    .makePermanent()
                    .withFlag(ForwardingObjective.Flag.VERSATILE)
                    .withPriority(1)
                    .withSelector(selector)
                    .withTreatment(treatment)
                    .add();

            helper(policyTree, forwardingObjective);
        }

        {
            TrafficSelector selector = DefaultTrafficSelector.builder()
                    .matchIPDst(IpPrefix.valueOf("2.0.0.2/32"))
                    .build();

            TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                    .setOutput(PortNumber.portNumber(2))
                    .build();

            ForwardingObjective forwardingObjective = DefaultForwardingObjective.builder()
                    .fromApp(new DefaultApplicationId(32, "b"))
                    .makePermanent()
                    .withFlag(ForwardingObjective.Flag.VERSATILE)
                    .withPriority(1)
                    .withSelector(selector)
                    .withTreatment(treatment)
                    .add();
            helper(policyTree, forwardingObjective);
        }

        {
            TrafficSelector selector = DefaultTrafficSelector.builder().build();

            TrafficTreatment treatment = DefaultTrafficTreatment.builder().build();

            ForwardingObjective forwardingObjective = DefaultForwardingObjective.builder()
                    .fromApp(new DefaultApplicationId(32, "b"))
                    .makePermanent()
                    .withFlag(ForwardingObjective.Flag.VERSATILE)
                    .withPriority(0)
                    .withSelector(selector)
                    .withTreatment(treatment)
                    .add();

            helper(policyTree, forwardingObjective);
        }

        System.out.println("---------- Override ----------");
        for (ForwardingObjective fo : policyTree.forwardTable.getForwardingObjectives()) {
            System.out.println(forwardingObjectiveToString(fo));
        }

        {
            TrafficSelector selector = DefaultTrafficSelector.builder()
                    .matchIPDst(IpPrefix.valueOf("2.0.0.3/32"))
                    .build();

            TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                    .setOutput(PortNumber.portNumber(3))
                    .build();

            ForwardingObjective forwardingObjective = DefaultForwardingObjective.builder()
                    .fromApp(new DefaultApplicationId(32, "b"))
                    .makePermanent()
                    .withFlag(ForwardingObjective.Flag.VERSATILE)
                    .withPriority(1)
                    .withSelector(selector)
                    .withTreatment(treatment)
                    .add();
            helper(policyTree, forwardingObjective);
        }

        System.out.println("---------- Override ----------");
        for (ForwardingObjective fo : policyTree.forwardTable.getForwardingObjectives()) {
            System.out.println(forwardingObjectiveToString(fo));
        }

        {
            TrafficSelector selector = DefaultTrafficSelector.builder()
                    .matchIPDst(IpPrefix.valueOf("2.0.0.3/32"))
                    .build();

            TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                    .setOutput(PortNumber.portNumber(3))
                    .build();

            ForwardingObjective forwardingObjective = DefaultForwardingObjective.builder()
                    .fromApp(new DefaultApplicationId(32, "b"))
                    .makePermanent()
                    .withFlag(ForwardingObjective.Flag.VERSATILE)
                    .withPriority(1)
                    .withSelector(selector)
                    .withTreatment(treatment)
                    .remove();
            helper(policyTree, forwardingObjective);
        }

        System.out.println("---------- Override ----------");
        for (ForwardingObjective fo : policyTree.forwardTable.getForwardingObjectives()) {
            System.out.println(forwardingObjectiveToString(fo));
        }
    }

    private void helper(FlowObjectiveCompositionTree policyTree, ForwardingObjective forwardingObjective) {
        log.info("before composition");
        log.info("\t{}", forwardingObjectiveToString(forwardingObjective));
        List<ForwardingObjective> forwardingObjectives
                = policyTree.updateForward(forwardingObjective);
        log.info("after composition");
        for (ForwardingObjective fo : forwardingObjectives) {
            log.info("\t{}", forwardingObjectiveToString(fo));
        }
    }*/
}
