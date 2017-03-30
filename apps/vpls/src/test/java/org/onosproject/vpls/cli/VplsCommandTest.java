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

package org.onosproject.vpls.cli;

import org.junit.Before;
import org.junit.Test;
import org.onosproject.net.EncapsulationType;
import org.onosproject.vpls.VplsTest;
import org.onosproject.vpls.api.VplsData;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Collection;

import static org.junit.Assert.*;

/**
 * Test for {@link VplsCommand}.
 */
public class VplsCommandTest extends VplsTest {
    private static final String NEW_LINE = "\n";
    private static final String SHOW_ALL_RES = "----------------\n" +
            "VPLS name: \u001B[1mvpls2\u001B[0m\n" +
            "Associated interfaces: [v200h2, v200h1]\n" +
            "Encapsulation: NONE\n" +
            "State: ADDED\n" +
            "----------------\n" +
            "VPLS name: \u001B[1mvpls1\u001B[0m\n" +
            "Associated interfaces: [v100h1, v100h2]\n" +
            "Encapsulation: NONE\n" +
            "State: ADDED\n" +
            "----------------\n";
    private static final String SHOW_ONE_RES = "VPLS name: \u001B[1mvpls1\u001B[0m\n" +
            "Associated interfaces: [v100h1, v100h2]\n" +
            "Encapsulation: NONE\n" +
            "State: ADDED\n";
    private static final String IFACE_ALREADY_USED =
            "\u001B[31mInterface \u001B[1mv200h1\u001B[0m\u001B[31m already associated " +
            "to VPLS \u001B[1mvpls2\u001B[0m\u001B[31m\u001B[0m\n";
    private static final String LIST_OUTPUT = VPLS1 + NEW_LINE + VPLS2 + NEW_LINE;
    VplsCommand vplsCommand;

    @Before
    public void setup() {
        vplsCommand = new VplsCommand();
        vplsCommand.vpls = new TestVpls();
        vplsCommand.interfaceService = new TestInterfaceService();
    }

    /**
     * Creates a new VPLS.
     */
    @Test
    public void testCreate() {
        vplsCommand.command = VplsCommandEnum.CREATE.toString();
        vplsCommand.vplsName = VPLS1;
        vplsCommand.execute();
        Collection<VplsData> vplss = vplsCommand.vpls.getAllVpls();
        assertEquals(1, vplss.size());
        VplsData result = vplss.iterator().next();
        VplsData expected = VplsData.of(VPLS1, EncapsulationType.NONE);
        assertEquals(expected, result);
    }

    /**
     * Adds new network interface to a VPLS.
     */
    @Test
    public void testAddIf() {
        vplsCommand.create(VPLS1);

        vplsCommand.command = VplsCommandEnum.ADD_IFACE.toString();
        vplsCommand.vplsName = VPLS1;
        vplsCommand.optArg = V100H1.name();
        vplsCommand.execute();

        vplsCommand.command = VplsCommandEnum.ADD_IFACE.toString();
        vplsCommand.vplsName = VPLS1;
        vplsCommand.optArg = V200H1.name();
        vplsCommand.execute();

        Collection<VplsData> vplss = vplsCommand.vpls.getAllVpls();
        assertEquals(1, vplss.size());
        VplsData result = vplss.iterator().next();
        assertEquals(2, result.interfaces().size());
        assertTrue(result.interfaces().contains(V100H1));
        assertTrue(result.interfaces().contains(V200H1));
    }

    /**
     * Removes network interface from a VPLS.
     */
    @Test
    public void testRemIf() {
        vplsCommand.create(VPLS1);
        vplsCommand.addIface(VPLS1, V100H1.name());
        vplsCommand.addIface(VPLS1, V200H1.name());

        vplsCommand.command = VplsCommandEnum.REMOVE_IFACE.toString();
        vplsCommand.vplsName = VPLS1;
        vplsCommand.optArg = V200H1.name();
        vplsCommand.execute();

        Collection<VplsData> vplss = vplsCommand.vpls.getAllVpls();
        assertEquals(1, vplss.size());
        VplsData result = vplss.iterator().next();
        assertEquals(1, result.interfaces().size());
        assertTrue(result.interfaces().contains(V100H1));
        assertFalse(result.interfaces().contains(V200H1));
    }

    /**
     * Lists all VPLS names.
     */
    @Test
    public void testList() {
        ((TestVpls) vplsCommand.vpls).initSampleData();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        System.setOut(ps);
        vplsCommand.command = VplsCommandEnum.LIST.toString();
        vplsCommand.execute();
        String result = baos.toString();

        assertEquals(LIST_OUTPUT, result);
    }

    /**
     * Sets encapsulation to a VPLS.
     */
    @Test
    public void testSetEncap() {
        ((TestVpls) vplsCommand.vpls).initSampleData();

        // Sets to NONE
        vplsCommand.command = VplsCommandEnum.SET_ENCAP.toString();
        vplsCommand.vplsName = VPLS1;
        vplsCommand.optArg = EncapsulationType.NONE.name();
        vplsCommand.execute();
        VplsData result = vplsCommand.vpls.getVpls(VPLS1);
        assertEquals(result.encapsulationType(), EncapsulationType.NONE);

        // Sets to VLAN
        vplsCommand.command = VplsCommandEnum.SET_ENCAP.toString();
        vplsCommand.vplsName = VPLS1;
        vplsCommand.optArg = EncapsulationType.VLAN.name();
        vplsCommand.execute();
        result = vplsCommand.vpls.getVpls(VPLS1);
        assertEquals(result.encapsulationType(), EncapsulationType.VLAN);

        // Sets to MPLS
        vplsCommand.command = VplsCommandEnum.SET_ENCAP.toString();
        vplsCommand.vplsName = VPLS1;
        vplsCommand.optArg = EncapsulationType.MPLS.name();
        vplsCommand.execute();
        result = vplsCommand.vpls.getVpls(VPLS1);
        assertEquals(result.encapsulationType(), EncapsulationType.MPLS);
    }

    /**
     * Deletes a VPLS.
     */
    @Test
    public void testDelete() {
        ((TestVpls) vplsCommand.vpls).initSampleData();
        vplsCommand.command = VplsCommandEnum.DELETE.toString();
        vplsCommand.vplsName = VPLS1;
        vplsCommand.execute();
        Collection<VplsData> vplss = vplsCommand.vpls.getAllVpls();
        assertEquals(1, vplss.size());
    }

    /**
     * Shows all VPLS information.
     */
    @Test
    public void testShowAll() {
        ((TestVpls) vplsCommand.vpls).initSampleData();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        System.setOut(ps);
        vplsCommand.command = VplsCommandEnum.SHOW.toString();
        vplsCommand.execute();
        String result = baos.toString();
        assertEquals(SHOW_ALL_RES, result);
    }

    /**
     * Shows a VPLS information.
     */
    @Test
    public void testShowOne() {
        ((TestVpls) vplsCommand.vpls).initSampleData();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        System.setOut(ps);
        vplsCommand.command = VplsCommandEnum.SHOW.toString();
        vplsCommand.vplsName = VPLS1;
        vplsCommand.execute();
        String result = baos.toString();
        assertEquals(SHOW_ONE_RES, result);
    }

    /**
     * Adds a network interface which already related to another VPLS to a VPLS.
     */
    @Test
    public void testIfaceAssociated() {
        ((TestVpls) vplsCommand.vpls).initSampleData();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        System.setOut(ps);
        vplsCommand.command = VplsCommandEnum.ADD_IFACE.toString();
        vplsCommand.vplsName = VPLS1;
        vplsCommand.optArg = V200H1.name();
        vplsCommand.execute();

        String result = baos.toString();
        assertEquals(IFACE_ALREADY_USED, result);
    }

    /**
     * Removes all VPLS.
     */
    @Test
    public void testClean() {
        ((TestVpls) vplsCommand.vpls).initSampleData();
        vplsCommand.command = VplsCommandEnum.CLEAN.toString();
        vplsCommand.execute();
        Collection<VplsData> vplss = vplsCommand.vpls.getAllVpls();
        assertEquals(0, vplss.size());
    }
}
