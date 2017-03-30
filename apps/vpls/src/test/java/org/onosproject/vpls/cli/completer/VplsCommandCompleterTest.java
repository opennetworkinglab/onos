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

package org.onosproject.vpls.cli.completer;

import com.google.common.collect.ImmutableList;
import org.apache.felix.service.command.CommandSession;
import org.apache.karaf.shell.console.CommandSessionHolder;
import org.apache.karaf.shell.console.completer.ArgumentCompleter;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.incubator.net.intf.Interface;
import org.onosproject.net.EncapsulationType;
import org.onosproject.vpls.VplsTest;
import org.onosproject.vpls.cli.VplsCommandEnum;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class VplsCommandCompleterTest extends VplsTest {
    private static final String VPLS_CMD = "vpls";
    private TestCommandSession commandSession;

    @Before
    public void setup() {
        commandSession = new TestCommandSession();
        CommandSessionHolder.setSession(commandSession);
    }

    /**
     * Test VPLS command completer.
     */
    @Test
    public void testCommandCompleter() {
        VplsCommandCompleter commandCompleter = new VplsCommandCompleter();
        List<String> choices = commandCompleter.choices();
        List<String> expected = VplsCommandEnum.toStringList();
        assertEquals(expected, choices);
    }

    /**
     * Test VPLS name completer.
     */
    @Test
    public void testNameCompleter() {
        VplsNameCompleter vplsNameCompleter = new VplsNameCompleter();
        vplsNameCompleter.vpls = new TestVpls();
        ((TestVpls) vplsNameCompleter.vpls).initSampleData();
        List<String> choices = vplsNameCompleter.choices();
        List<String> expected = ImmutableList.of(VPLS1, VPLS2);

        // Can not ensure the order, use contains all instead of equals
        assertEquals(choices.size(), expected.size());
        assertTrue(choices.containsAll(expected));
    }

    /**
     * Test VPLS option arguments completer.
     */
    @Test
    public void testOptArgCompleter() {
        VplsOptArgCompleter completer = new VplsOptArgCompleter();
        completer.vpls = new TestVpls();
        ((TestVpls) completer.vpls).initSampleData();
        completer.interfaceService = new TestInterfaceService();

        // Add interface to VPLS
        commandSession.updateArguments(VPLS_CMD, VplsCommandEnum.ADD_IFACE.toString(), VPLS1);

        List<String> choices = completer.choices();
        List<String> expected = ImmutableList.of(V300H1.name(),
                                                 V300H2.name(),
                                                 V400H1.name(),
                                                 VNONEH1.name(),
                                                 VNONEH2.name(),
                                                 VNONEH3.name());

        // Can not ensure the order, use contains all instead of equals
        assertEquals(choices.size(), expected.size());
        assertTrue(choices.containsAll(expected));

        // Removes interface from VPLS
        commandSession.updateArguments(VPLS_CMD, VplsCommandEnum.REMOVE_IFACE.toString(), VPLS1);
        choices = completer.choices();
        expected = completer.vpls.getVpls(VPLS1).interfaces().stream()
                .map(Interface::name)
                .collect(Collectors.toList());

        // Can not ensure the order, use contains all instead of equals
        assertEquals(choices.size(), expected.size());
        assertTrue(choices.containsAll(expected));

        // Sets encapsulation
        commandSession.updateArguments(VPLS_CMD, VplsCommandEnum.SET_ENCAP.toString(), VPLS1);
        choices = completer.choices();
        expected = Arrays.stream(EncapsulationType.values())
                .map(Enum::toString)
                .collect(Collectors.toList());

        // Can not ensure the order, use contains all instead of equals
        assertEquals(choices.size(), expected.size());
        assertTrue(choices.containsAll(expected));
    }

    /**
     * Test command session.
     */
    class TestCommandSession implements CommandSession {
        ArgumentCompleter.ArgumentList argumentList;
        public TestCommandSession() {
            String[] emptyStringArr = new String[0];
            argumentList = new ArgumentCompleter.ArgumentList(emptyStringArr,
                                                              0,
                                                              0,
                                                              0);
        }

        /**
         * Updates argument list for the command session.
         *
         * @param args new arguments
         */
        public void updateArguments(String... args) {
            argumentList = new ArgumentCompleter.ArgumentList(args,
                                                              0,
                                                              0,
                                                              0);
        }

        @Override
        public Object execute(CharSequence charSequence) throws Exception {
            return null;
        }

        @Override
        public void close() {

        }

        @Override
        public InputStream getKeyboard() {
            return null;
        }

        @Override
        public PrintStream getConsole() {
            return null;
        }

        @Override
        public Object get(String s) {
            return argumentList;
        }

        @Override
        public void put(String s, Object o) {

        }

        @Override
        public CharSequence format(Object o, int i) {
            return null;
        }

        @Override
        public Object convert(Class<?> aClass, Object o) {
            return null;
        }
    }
}
