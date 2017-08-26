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

package org.onosproject.ui.impl;

import org.junit.Before;
import org.junit.Test;
import org.onosproject.driver.extensions.Ofdpa3SetMplsType;
import org.onosproject.net.DeviceId;
import org.onosproject.net.flow.instructions.ExtensionTreatment;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions;

import static org.junit.Assert.assertEquals;
import static org.onosproject.net.DeviceId.deviceId;
import static org.onosproject.net.PortNumber.portNumber;

/**
 * Unit tests for {@link FlowViewMessageHandler}.
 */
public class FlowViewMessageHandlerTest extends AbstractUiImplTest {

    private static final String DEV_OF_204 = "of:0000000000000204";

    private static final String EXT_FULL_STR =
            "EXTENSION:of:0000000000000204/Ofdpa3SetMplsType{mplsType=32}";
    private static final String EXT_NO_DPID =
            "EXTENSION:Ofdpa3SetMplsType{mplsType=32}";

    private FlowViewMessageHandler handler;
    private Instruction instr;
    private String string;
    private String render;


    @Before
    public void setUp() {
        handler = new FlowViewMessageHandler();
    }

    @Test
    public void renderOutputInstruction() {
        title("renderOutputInstruction");
        instr = Instructions.createOutput(portNumber(4));
        string = instr.toString();
        render = handler.renderInstructionForDisplay(instr);

        print(string);
        assertEquals("not same output", string, render);
        assertEquals("not output to port 4", "OUTPUT:4", render);
    }


    @Test
    public void renderExtensionInstruction() {
        title("renderExtensionInstruction");

        ExtensionTreatment extn = new Ofdpa3SetMplsType((short) 32);
        DeviceId devid = deviceId(DEV_OF_204);

        instr = Instructions.extension(extn, devid);
        string = instr.toString();
        render = handler.renderInstructionForDisplay(instr);

        print(string);
        print(render);

        assertEquals("unexpected toString", EXT_FULL_STR, string);
        assertEquals("unexpected short string", EXT_NO_DPID, render);
    }
}
