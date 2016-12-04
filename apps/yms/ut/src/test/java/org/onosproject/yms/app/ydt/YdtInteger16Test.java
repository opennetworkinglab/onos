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

import org.junit.Test;
import org.onosproject.yms.app.ydt.exceptions.YdtException;

import static org.onosproject.yms.app.ydt.YdtTestConstants.MAXIWR;
import static org.onosproject.yms.app.ydt.YdtTestConstants.MAXUIWR;
import static org.onosproject.yms.app.ydt.YdtTestConstants.MIDIWR;
import static org.onosproject.yms.app.ydt.YdtTestConstants.MIDUIWR;
import static org.onosproject.yms.app.ydt.YdtTestConstants.MINIWR;
import static org.onosproject.yms.app.ydt.YdtTestConstants.MINUIWR;
import static org.onosproject.yms.app.ydt.YdtTestConstants.MRV;
import static org.onosproject.yms.app.ydt.YdtTestConstants.RUI;
import static org.onosproject.yms.app.ydt.YdtTestConstants.TYPE;
import static org.onosproject.yms.app.ydt.YdtTestUtils.integer16Ydt;
import static org.onosproject.yms.app.ydt.YdtTestUtils.validateLeafContents;
import static org.onosproject.yms.app.ydt.YdtTestUtils.validateNodeContents;
import static org.onosproject.yms.ydt.YdtContextOperationType.MERGE;

public class YdtInteger16Test {

    /*
    Positive scenario

    input at boundry for integer
        i. min value
        ii. max value

    input at boundry for uinteger
        i. min value
        ii. max value

    input with in range
        if range is 10 to 100 for integer
            i.1. input 11
            i.2. min value 10
            i.3. max value 100

        if range is 10 to 100 for uinteger
            i.1. input 11
            i.2. min value 10
            i.3. max value 100

    input with multi interval range
        if range is 10..40 | 50..100 for integer
            i.1. input 11
            i.2. input 10
            i.3. input 40
            i.4. input 50
            i.5. input 55
            i.6. input 100

        if range is 10..40 | 50..100 for uinteger
            i.1. input 11
            i.2. input 10
            i.3. input 40
            i.4. input 50
            i.5. input 55
            i.6. input 100

        if range is "min .. 2 | 10 | 20..max" for integer
            i.1. input -32768
            i.2. input 1
            i.3. input 2
            i.4. input 10
            i.5. input 20
            i.6. input 100
            i.7. input 32767

         if range is "min .. 2 | 10 | 20..max" for uInteger
            i.1. input 0
            i.2. input 1
            i.3. input 2
            i.4. input 10
            i.5. input 20
            i.6. input 100
            i.7. input 65535
    */

    /**
     * Creates and validates integer16 ydt covering different positive scenario.
     */
    @Test
    public void positiveTest() throws YdtException {
        YangRequestWorkBench ydtBuilder = integer16Ydt();
        validateTree(ydtBuilder);
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
        validateNodeContents(ydtNode, "integer16", MERGE);
        ydtNode = ydtNode.getFirstChild();
        validateLeafContents(ydtNode, "negInt", "-32768");
        ydtNode = ydtNode.getNextSibling();
        validateLeafContents(ydtNode, "posInt", "32767");
        ydtNode = ydtNode.getNextSibling();
        validateLeafContents(ydtNode, "minUInt", "0");
        ydtNode = ydtNode.getNextSibling();
        validateLeafContents(ydtNode, "maxUInt", "65535");
        ydtNode = ydtNode.getNextSibling();
        validateLeafContents(ydtNode, MIDIWR, "11");
        ydtNode = ydtNode.getNextSibling();
        validateLeafContents(ydtNode, MINIWR, "10");
        ydtNode = ydtNode.getNextSibling();
        validateLeafContents(ydtNode, MAXIWR, "100");
        ydtNode = ydtNode.getNextSibling();
        validateLeafContents(ydtNode, MIDUIWR, "11");
        ydtNode = ydtNode.getNextSibling();
        validateLeafContents(ydtNode, MINUIWR, "10");
        ydtNode = ydtNode.getNextSibling();
        validateLeafContents(ydtNode, MAXUIWR, "100");
        ydtNode = ydtNode.getNextSibling();
        validateNodeContents(ydtNode, MRV, MERGE);
        ydtNode = ydtNode.getFirstChild();
        validateLeafContents(ydtNode, "integer", "11");
        ydtNode = ydtNode.getParent();
        ydtNode = ydtNode.getNextSibling();
        validateNodeContents(ydtNode, MRV, MERGE);
        ydtNode = ydtNode.getFirstChild();
        validateLeafContents(ydtNode, "integer", "10");
        ydtNode = ydtNode.getParent();
        ydtNode = ydtNode.getNextSibling();
        validateNodeContents(ydtNode, MRV, MERGE);
        ydtNode = ydtNode.getFirstChild();
        validateLeafContents(ydtNode, "integer", "40");
        ydtNode = ydtNode.getParent();
        ydtNode = ydtNode.getNextSibling();
        validateNodeContents(ydtNode, MRV, MERGE);
        ydtNode = ydtNode.getFirstChild();
        validateLeafContents(ydtNode, "integer", "50");
        ydtNode = ydtNode.getParent();
        ydtNode = ydtNode.getNextSibling();
        validateNodeContents(ydtNode, MRV, MERGE);
        ydtNode = ydtNode.getFirstChild();
        validateLeafContents(ydtNode, "integer", "55");
        ydtNode = ydtNode.getParent();
        ydtNode = ydtNode.getNextSibling();
        validateNodeContents(ydtNode, MRV, MERGE);
        ydtNode = ydtNode.getFirstChild();
        validateLeafContents(ydtNode, "integer", "100");
        ydtNode = ydtNode.getParent();
        ydtNode = ydtNode.getNextSibling();
        validateNodeContents(ydtNode, MRV, MERGE);
        ydtNode = ydtNode.getFirstChild();
        validateLeafContents(ydtNode, "UnInteger", "11");
        ydtNode = ydtNode.getParent();
        ydtNode = ydtNode.getNextSibling();
        validateNodeContents(ydtNode, MRV, MERGE);
        ydtNode = ydtNode.getFirstChild();
        validateLeafContents(ydtNode, "UnInteger", "10");
        ydtNode = ydtNode.getParent();
        ydtNode = ydtNode.getNextSibling();
        validateNodeContents(ydtNode, MRV, MERGE);
        ydtNode = ydtNode.getFirstChild();
        validateLeafContents(ydtNode, "UnInteger", "40");
        ydtNode = ydtNode.getParent();
        ydtNode = ydtNode.getNextSibling();
        validateNodeContents(ydtNode, MRV, MERGE);
        ydtNode = ydtNode.getFirstChild();
        validateLeafContents(ydtNode, "UnInteger", "50");
        ydtNode = ydtNode.getParent();
        ydtNode = ydtNode.getNextSibling();
        validateNodeContents(ydtNode, MRV, MERGE);
        ydtNode = ydtNode.getFirstChild();
        validateLeafContents(ydtNode, "UnInteger", "55");
        ydtNode = ydtNode.getParent();
        ydtNode = ydtNode.getNextSibling();
        validateNodeContents(ydtNode, MRV, MERGE);
        ydtNode = ydtNode.getFirstChild();
        validateLeafContents(ydtNode, "UnInteger", "100");

        ydtNode = ydtNode.getParent();
        ydtNode = ydtNode.getNextSibling();
        validateNodeContents(ydtNode, MRV, MERGE);
        ydtNode = ydtNode.getFirstChild();
        validateLeafContents(ydtNode, "revInteger", "-32768");
        ydtNode = ydtNode.getParent();
        ydtNode = ydtNode.getNextSibling();
        validateNodeContents(ydtNode, MRV, MERGE);
        ydtNode = ydtNode.getFirstChild();
        validateLeafContents(ydtNode, "revInteger", "1");
        ydtNode = ydtNode.getParent();
        ydtNode = ydtNode.getNextSibling();
        validateNodeContents(ydtNode, MRV, MERGE);

        validate1Tree(ydtNode);
    }

    /**
     * Validates the given built ydt.
     */
    private void validate1Tree(YdtNode ydtNode) {

        ydtNode = ydtNode.getFirstChild();
        validateLeafContents(ydtNode, "revInteger", "2");
        ydtNode = ydtNode.getParent();
        ydtNode = ydtNode.getNextSibling();
        validateNodeContents(ydtNode, MRV, MERGE);
        ydtNode = ydtNode.getFirstChild();
        validateLeafContents(ydtNode, "revInteger", "10");
        ydtNode = ydtNode.getParent();
        ydtNode = ydtNode.getNextSibling();
        validateNodeContents(ydtNode, MRV, MERGE);
        ydtNode = ydtNode.getFirstChild();
        validateLeafContents(ydtNode, "revInteger", "20");
        ydtNode = ydtNode.getParent();
        ydtNode = ydtNode.getNextSibling();
        validateNodeContents(ydtNode, MRV, MERGE);
        ydtNode = ydtNode.getFirstChild();
        validateLeafContents(ydtNode, "revInteger", "100");
        ydtNode = ydtNode.getParent();
        ydtNode = ydtNode.getNextSibling();
        validateNodeContents(ydtNode, MRV, MERGE);
        ydtNode = ydtNode.getFirstChild();
        validateLeafContents(ydtNode, "revInteger", "32767");

        ydtNode = ydtNode.getParent();
        ydtNode = ydtNode.getNextSibling();
        validateNodeContents(ydtNode, MRV, MERGE);
        ydtNode = ydtNode.getFirstChild();
        validateLeafContents(ydtNode, RUI, "0");
        ydtNode = ydtNode.getParent();
        ydtNode = ydtNode.getNextSibling();
        validateNodeContents(ydtNode, MRV, MERGE);
        ydtNode = ydtNode.getFirstChild();
        validateLeafContents(ydtNode, RUI, "1");
        ydtNode = ydtNode.getParent();
        ydtNode = ydtNode.getNextSibling();
        validateNodeContents(ydtNode, MRV, MERGE);
        ydtNode = ydtNode.getFirstChild();
        validateLeafContents(ydtNode, RUI, "2");
        ydtNode = ydtNode.getParent();
        ydtNode = ydtNode.getNextSibling();
        validateNodeContents(ydtNode, MRV, MERGE);
        ydtNode = ydtNode.getFirstChild();
        validateLeafContents(ydtNode, RUI, "10");
        ydtNode = ydtNode.getParent();
        ydtNode = ydtNode.getNextSibling();
        validateNodeContents(ydtNode, MRV, MERGE);
        ydtNode = ydtNode.getFirstChild();
        validateLeafContents(ydtNode, RUI, "20");
        ydtNode = ydtNode.getParent();
        ydtNode = ydtNode.getNextSibling();
        validateNodeContents(ydtNode, MRV, MERGE);
        ydtNode = ydtNode.getFirstChild();
        validateLeafContents(ydtNode, RUI, "100");
        ydtNode = ydtNode.getParent();
        ydtNode = ydtNode.getNextSibling();
        validateNodeContents(ydtNode, MRV, MERGE);
        ydtNode = ydtNode.getFirstChild();
        validateLeafContents(ydtNode, RUI, "65535");
    }

    //TODO negative scenario will be handled later
    /*
        Negative scenario

        wrong type input
            i. input string instead of integer
            ii. input string instead of uinteger

    input out of range
        i. input for int 8 range -32768 to 32767
        i.1. input -32769
        i.2. input 32768

        ii. input for uint 8 range 0 to 65535
        ii.1. input -32769
        ii.2. input 65536

        input out of range parameter
            if range is 10 to 100 for int
                i.1. input 9
                i.2. input 101

            if range is 10 to 100 for uInt
                i.1. input 9
                i.2. input 101

        input with multi interval range
        if range is 10..40 | 50..100 for integer
            i.1. input 9
            i.2. input 41
            i.3. input 49
            i.4. input 101

        if range is 10..40 | 50..100 for uinteger
            i.1. input 9
            i.2. input 41
            i.3. input 49
            i.4. input 101

        input with multi interval range
        if range is min ..  | 10 | 20..max for integer
            i.1. input -32769
            i.2. input 4
            i.3. input 9
            i.4. input 11
            i.5. input 19
            i.6. input 32768

        if range is min .. 3 | 10 | 20..max for uinteger
            i.1. input -32769
            i.2. input 4
            i.3. input 9
            i.4. input 11
            i.5. input 19
            i.6. input 65536

    */

    /**
     * Tests all the minimum and maximum value's negative scenario's for
     * signed integer16 data type.
     */
//    @Test
//    public void negative1Test() throws YdtException {
//        validateErrMsg("posInt", INT16NS, "integer", SINT16, null);
//        validateErrMsg("posInt", INT16NS, "127.0", SINT16, null);
//        validateErrMsg("negInt", INT16NS, "-32769", SINT16, null);
//        validateErrMsg("posInt", INT16NS, "32768", SINT16, null);
//        validateErrMsg(MINIWR, INT16NS, "9", CAPSINT16, null);
//        validateErrMsg(MAXIWR, INT16NS, "101", CAPSINT16, null);
//    }
//
//    /**
//     * Tests all the minimum and maximum value's negative scenario's for
//     * unsigned integer16 data type.
//     */
//    @Test
//    public void negative2Test() throws YdtException {
//        validateErrMsg("maxUInt", INT16NS, "integer", SUINT16, null);
//        validateErrMsg("maxUInt", INT16NS, "127.0", SUINT16, null);
//        validateErrMsg("minUInt", INT16NS, "-32769", MINVALUE, null);
//        validateErrMsg("maxUInt", INT16NS, "65536", MAXUINT16, null);
//        validateErrMsg(MINUIWR, INT16NS, "9", CAPSUINT16, null);
//        validateErrMsg(MAXUIWR, INT16NS, "101", CAPSUINT16, null);
//    }
//
//    /**
//     * Tests all possible negative scenario's for signed integer16 data type
//     * with range "10..40 | 50..100".
//     */
//    @Test
//    public void negative3Test() throws YdtException {
//        validateErrMsg("integer", INT16NS, "9", CAPSINT16, MRV);
//        validateErrMsg("integer", INT16NS, "41", CAPSINT16, MRV);
//        validateErrMsg("integer", INT16NS, "49", CAPSINT16, MRV);
//        validateErrMsg("integer", INT16NS, "101", CAPSINT16, MRV);
//    }
//
//    /**
//     * Tests all possible negative scenario's for unsigned integer16 data type
//     * with range "10..40 | 50..100".
//     */
//    @Test
//    public void negative4Test() throws YdtException {
//        validateErrMsg("UnInteger", INT16NS, "9", CAPSUINT16, MRV);
//        validateErrMsg("UnInteger", INT16NS, "41", CAPSUINT16, MRV);
//        validateErrMsg("UnInteger", INT16NS, "49", CAPSUINT16, MRV);
//        validateErrMsg("UnInteger", INT16NS, "101", CAPSUINT16, MRV);
//    }
//
//    /**
//     * Tests all possible negative scenario's for signed integer16 data type
//     * with range "min .. 2 | 10 | 20..max".
//     */
//    @Test
//    public void negative5Test() throws YdtException {
//        // multi range validation
//        validateErrMsg("revInteger", INT16NS, "-32769", SINT16, MRV);
//        validateErrMsg("revInteger", INT16NS, "19", CAPSINT16, MRV);
//        validateErrMsg("revInteger", INT16NS, "4", CAPSINT16, MRV);
//        validateErrMsg("revInteger", INT16NS, "32768", SINT16, MRV);
//        validateErrMsg("revInteger", INT16NS, "9", CAPSINT16, MRV);
//        validateErrMsg("revInteger", INT16NS, "11", CAPSINT16, MRV);
//    }
//
//    /**
//     * Tests all possible negative scenario's for unsigned integer16 data type
//     * with range "min .. 2 | 10 | 20..max".
//     */
//    @Test
//    public void negative6Test() throws YdtException {
//        // multi range validation
//        validateErrMsg(RUI, INT16NS, "-32769", MINVALUE, MRV);
//        validateErrMsg(RUI, INT16NS, "4", CAPSUINT16, MRV);
//        validateErrMsg(RUI, INT16NS, "9", CAPSUINT16, MRV);
//        validateErrMsg(RUI, INT16NS, "11", CAPSUINT16, MRV);
//        validateErrMsg(RUI, INT16NS, "19", CAPSUINT16, MRV);
//        validateErrMsg(RUI, INT16NS, "65536", MAXUINT16, MRV);
//    }
}
