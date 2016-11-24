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

package org.onosproject.yms.app.ydt;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.onosproject.yms.app.ydt.exceptions.YdtException;

import static org.onosproject.yms.app.ydt.YdtTestConstants.A;
import static org.onosproject.yms.app.ydt.YdtTestConstants.B;
import static org.onosproject.yms.app.ydt.YdtTestConstants.C;
import static org.onosproject.yms.app.ydt.YdtTestConstants.E;
import static org.onosproject.yms.app.ydt.YdtTestConstants.F;
import static org.onosproject.yms.app.ydt.YdtTestConstants.G;
import static org.onosproject.yms.app.ydt.YdtTestConstants.H;
import static org.onosproject.yms.app.ydt.YdtTestConstants.MAXIWR;
import static org.onosproject.yms.app.ydt.YdtTestConstants.MIDIWR;
import static org.onosproject.yms.app.ydt.YdtTestConstants.MINIWR;
import static org.onosproject.yms.app.ydt.YdtTestConstants.MRV;
import static org.onosproject.yms.app.ydt.YdtTestConstants.NIWMF;
import static org.onosproject.yms.app.ydt.YdtTestConstants.NWF;
import static org.onosproject.yms.app.ydt.YdtTestConstants.PIWMF;
import static org.onosproject.yms.app.ydt.YdtTestConstants.PWF;
import static org.onosproject.yms.app.ydt.YdtTestConstants.TYPE;
import static org.onosproject.yms.app.ydt.YdtTestUtils.decimal64Ydt;
import static org.onosproject.yms.app.ydt.YdtTestUtils.validateLeafContents;
import static org.onosproject.yms.app.ydt.YdtTestUtils.validateNodeContents;
import static org.onosproject.yms.ydt.YdtContextOperationType.MERGE;

public class YdtDecimal64Test {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    /*

    Positive scenario

    input at boundry for decimal64 with fraction 2
        i. min value
        ii. max value

    input at boundry for decimal64 with minimum fraction
        i. min value
        ii. mid value
        iii. max value

    input at boundry for decimal64 with maximum fraction
        i. min value
        ii. mid value
        iii. max value

    input with in range
        if range is 10 to 100 for integer
            i.1. input 11
            i.2. min value 10
            i.3. max value 100

    input with multi interval range
        if range is 10..40 | 50..100 for decimal64
            i.1. input 11
            i.2. input 10
            i.3. input 40
            i.4. input 50
            i.5. input 55
            i.6. input 100

        if range is "min .. 3.14 | 10 | 20..max" for decimal64
            i.1. input min
            i.2. input 2.505
            i.3. input 3.14
            i.4. input 10
            i.5. input 20
            i.6. input 92233720368547757
            i.7. input 92233720368547758.07

    */

    /**
     * Creates and validates decimal64 ydt covering different positive scenario.
     */
    @Test
    public void positiveTest() throws YdtException {
        YangRequestWorkBench ydtBuilder = decimal64Ydt();
        validateTree(ydtBuilder);

        //TODO need to be handled later
//        YangRequestWorkBench sbiYdt = validateYangObject(
//                ydtBuilder, "builtInType", "ydt.decimal64");
//        validateTree(sbiYdt);
    }

    /**
     * Validates the given built ydt.
     */
    private void validateTree(YangRequestWorkBench ydtBuilder) {

        // assign root node to ydtNode for validating purpose.
        YdtNode ydtNode = (YdtNode) ydtBuilder.getRootNode();
        // Logical root node does not have operation type
        validateNodeContents(ydtNode, TYPE, null);

        ydtNode = ydtNode.getFirstChild();
        validateNodeContents(ydtNode, "decimal64", MERGE);
        ydtNode = ydtNode.getFirstChild();
        validateLeafContents(ydtNode, "negInt", C);
        ydtNode = ydtNode.getNextSibling();
        validateLeafContents(ydtNode, "posInt", A);
        ydtNode = ydtNode.getNextSibling();
        validateLeafContents(ydtNode, NIWMF, F);
        ydtNode = ydtNode.getNextSibling();
        validateLeafContents(ydtNode, PIWMF, G);
        ydtNode = ydtNode.getNextSibling();
        validateLeafContents(ydtNode, NWF, H);
        ydtNode = ydtNode.getNextSibling();
        validateLeafContents(ydtNode, PWF, E);
        ydtNode = ydtNode.getNextSibling();
        validateLeafContents(ydtNode, MIDIWR, "11");
        ydtNode = ydtNode.getNextSibling();
        validateLeafContents(ydtNode, MINIWR, "10");
        ydtNode = ydtNode.getNextSibling();
        validateLeafContents(ydtNode, MAXIWR, "100");
        ydtNode = ydtNode.getNextSibling();
        validateNodeContents(ydtNode, MRV, MERGE);
        ydtNode = ydtNode.getFirstChild();
        validateLeafContents(ydtNode, "decimal", "11");
        ydtNode = ydtNode.getParent();
        ydtNode = ydtNode.getNextSibling();
        validateNodeContents(ydtNode, MRV, MERGE);
        ydtNode = ydtNode.getFirstChild();
        validateLeafContents(ydtNode, "decimal", "10");
        ydtNode = ydtNode.getParent();
        ydtNode = ydtNode.getNextSibling();
        validateNodeContents(ydtNode, MRV, MERGE);
        ydtNode = ydtNode.getFirstChild();
        validateLeafContents(ydtNode, "decimal", "40");
        ydtNode = ydtNode.getParent();
        ydtNode = ydtNode.getNextSibling();
        validateNodeContents(ydtNode, MRV, MERGE);
        ydtNode = ydtNode.getFirstChild();
        validateLeafContents(ydtNode, "decimal", "50");
        ydtNode = ydtNode.getParent();
        ydtNode = ydtNode.getNextSibling();
        validateNodeContents(ydtNode, MRV, MERGE);
        ydtNode = ydtNode.getFirstChild();
        validateLeafContents(ydtNode, "decimal", "55");
        ydtNode = ydtNode.getParent();
        ydtNode = ydtNode.getNextSibling();
        validateNodeContents(ydtNode, MRV, MERGE);
        ydtNode = ydtNode.getFirstChild();
        validateLeafContents(ydtNode, "decimal", "100");
        ydtNode = ydtNode.getParent();
        ydtNode = ydtNode.getNextSibling();
        validateNodeContents(ydtNode, MRV, MERGE);
        ydtNode = ydtNode.getFirstChild();
        validateLeafContents(ydtNode, "revDecimal", C);
        ydtNode = ydtNode.getParent();
        ydtNode = ydtNode.getNextSibling();
        validateNodeContents(ydtNode, MRV, MERGE);
        ydtNode = ydtNode.getFirstChild();
        validateLeafContents(ydtNode, "revDecimal", "2.505");
        ydtNode = ydtNode.getParent();
        ydtNode = ydtNode.getNextSibling();
        validateNodeContents(ydtNode, MRV, MERGE);
        ydtNode = ydtNode.getFirstChild();
        validateLeafContents(ydtNode, "revDecimal", "3.14");
        ydtNode = ydtNode.getParent();
        ydtNode = ydtNode.getNextSibling();
        validateNodeContents(ydtNode, MRV, MERGE);
        ydtNode = ydtNode.getFirstChild();
        validateLeafContents(ydtNode, "revDecimal", "10");
        ydtNode = ydtNode.getParent();
        ydtNode = ydtNode.getNextSibling();
        validateNodeContents(ydtNode, MRV, MERGE);
        ydtNode = ydtNode.getFirstChild();
        validateLeafContents(ydtNode, "revDecimal", "20");
        ydtNode = ydtNode.getParent();
        ydtNode = ydtNode.getNextSibling();
        validateNodeContents(ydtNode, MRV, MERGE);
        ydtNode = ydtNode.getFirstChild();
        validateLeafContents(ydtNode, "revDecimal", B);
        ydtNode = ydtNode.getParent();
        ydtNode = ydtNode.getNextSibling();
        validateNodeContents(ydtNode, MRV, MERGE);
        ydtNode = ydtNode.getFirstChild();
        validateLeafContents(ydtNode, "revDecimal", A);
    }

    //TODO negative scenario will be handled later
    /*
        Negative scenario

        input with position 0
        input with position 1
        input with position 2
    */

//    /**
//     * Tests all the negative scenario's for bit data type.
//     */
//    @Test
//    public void negativeTest() {
//        thrown.expect(IllegalArgumentException.class);
//        thrown.expectMessage(E_D64);
//        YangRequestWorkBench ydtBuilder;
//        ydtBuilder = getYdtBuilder("builtInType", "decimal64", "ydt.decimal64",
//                                   MERGE);
//        ydtBuilder.addLeaf("l1", null, "-9.1999999999e17");
//        ydtBuilder.traverseToParent();
//    }
}
