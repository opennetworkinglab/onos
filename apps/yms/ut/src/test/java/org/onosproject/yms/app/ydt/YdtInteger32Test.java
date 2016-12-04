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
import static org.onosproject.yms.app.ydt.YdtTestConstants.MAXUINT32;
import static org.onosproject.yms.app.ydt.YdtTestConstants.MAXUIWR;
import static org.onosproject.yms.app.ydt.YdtTestConstants.MIDIWR;
import static org.onosproject.yms.app.ydt.YdtTestConstants.MIDUIWR;
import static org.onosproject.yms.app.ydt.YdtTestConstants.MINIWR;
import static org.onosproject.yms.app.ydt.YdtTestConstants.MINUIWR;
import static org.onosproject.yms.app.ydt.YdtTestConstants.MINVALUE;
import static org.onosproject.yms.app.ydt.YdtTestConstants.MRV;
import static org.onosproject.yms.app.ydt.YdtTestConstants.RUI;
import static org.onosproject.yms.app.ydt.YdtTestConstants.TYPE;
import static org.onosproject.yms.app.ydt.YdtTestUtils.integer32Ydt;
import static org.onosproject.yms.app.ydt.YdtTestUtils.validateLeafContents;
import static org.onosproject.yms.app.ydt.YdtTestUtils.validateNodeContents;
import static org.onosproject.yms.ydt.YdtContextOperationType.MERGE;

public class YdtInteger32Test {

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
            i.1. input -2147483648
            i.2. input 1
            i.3. input 2
            i.4. input 10
            i.5. input 20
            i.6. input 100
            i.7. input 2147483647

         if range is "min .. 2 | 10 | 20..max" for uInteger
            i.1. input 0
            i.2. input 1
            i.3. input 2
            i.4. input 10
            i.5. input 20
            i.6. input 100
            i.7. input 4294967295
    */

    /**
     * Creates and validates integer32 ydt covering different positive scenario.
     */
    @Test
    public void positiveTest() throws YdtException {
        YangRequestWorkBench ydtBuilder = integer32Ydt();
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
        validateNodeContents(ydtNode, "integer32", MERGE);
        ydtNode = ydtNode.getFirstChild();
        validateLeafContents(ydtNode, "negInt", "-2147483648");
        ydtNode = ydtNode.getNextSibling();
        validateLeafContents(ydtNode, "posInt", "2147483647");
        ydtNode = ydtNode.getNextSibling();
        validateLeafContents(ydtNode, "minUInt", MINVALUE);
        ydtNode = ydtNode.getNextSibling();
        validateLeafContents(ydtNode, "maxUInt", MAXUINT32);
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
        validateLeafContents(ydtNode, "revInteger", "-2147483648");
        ydtNode = ydtNode.getParent();
        ydtNode = ydtNode.getNextSibling();
        validateNodeContents(ydtNode, MRV, MERGE);
        ydtNode = ydtNode.getFirstChild();
        validateLeafContents(ydtNode, "revInteger", "1");
        ydtNode = ydtNode.getParent();
        ydtNode = ydtNode.getNextSibling();
        validateNodeContents(ydtNode, MRV, MERGE);
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

        validate1Tree(ydtNode);
    }

    /**
     * Validates the given built ydt.
     */
    private void validate1Tree(YdtNode ydtNode) {
        ydtNode = ydtNode.getFirstChild();
        validateLeafContents(ydtNode, "revInteger", "100");
        ydtNode = ydtNode.getParent();
        ydtNode = ydtNode.getNextSibling();
        validateNodeContents(ydtNode, MRV, MERGE);
        ydtNode = ydtNode.getFirstChild();
        validateLeafContents(ydtNode, "revInteger", "2147483647");

        ydtNode = ydtNode.getParent();
        ydtNode = ydtNode.getNextSibling();
        validateNodeContents(ydtNode, MRV, MERGE);
        ydtNode = ydtNode.getFirstChild();
        validateLeafContents(ydtNode, RUI, MINVALUE);
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
        validateLeafContents(ydtNode, RUI, MAXUINT32);
    }

    //TODO negative scenario will be handled later
    /*
        Negative scenario

        wrong type input
            i. input string instead of integer
            ii. input string instead of uinteger

        input out of range
            i. input for int 8 range -2147483648 to 2147483647
            i.1. input -2147483649
            i.2. input 2147483648

            ii. input for uint 8 range 0 to 4294967295
            ii.1. input -2147483649
            ii.2. input 4294967296

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
                i.1. input -2147483649
                i.2. input 4
                i.3. input 9
                i.4. input 11
                i.5. input 19
                i.6. input 256

            if range is min .. 3 | 10 | 20..max for uinteger
                i.1. input -2147483649
                i.2. input 4
                i.3. input 9
                i.4. input 11
                i.5. input 19
                i.6. input 4294967296

        */

    /**
     * Tests all the minimum and maximum value's negative scenario's for
     * signed integer32 data type.
     */
//    @Test
//    public void negative1Test() throws YdtException {
//        validateErrMsg("posInt", INT32NS, "integer", SINT32, null);
//        validateErrMsg("posInt", INT32NS, "127.0", SINT32, null);
//        validateErrMsg("negInt", INT32NS, "-2147483649", SINT32, null);
//        validateErrMsg("posInt", INT32NS, "2147483648", SINT32, null);
//        validateErrMsg(MINIWR, INT32NS, "9", CAPSINT32, null);
//        validateErrMsg(MAXIWR, INT32NS, "101", CAPSINT32, null);
//    }
//
//    /**
//     * Tests all the minimum and maximum value's negative scenario's for
//     * unsigned integer32 data type.
//     */
//    @Test
//    public void negative2Test() throws YdtException {
//        validateErrMsg("maxUInt", INT32NS, "integer", SUINT32, null);
//        validateErrMsg("maxUInt", INT32NS, "127.0", SUINT32, null);
//        validateErrMsg("minUInt", INT32NS, "-2147483649", MINVALUE, null);
//        validateErrMsg("maxUInt", INT32NS, "4294967296", MAXUINT32, null);
//        validateErrMsg(MINUIWR, INT32NS, "9", CAPSUINT32, null);
//        validateErrMsg(MAXUIWR, INT32NS, "101", CAPSUINT32, null);
//    }
//
//    /**
//     * Tests all possible negative scenario's for signed integer32 data type
//     * with range "10..40 | 50..100".
//     */
//    @Test
//    public void negative3Test() throws YdtException {
//        validateErrMsg("integer", INT32NS, "9", CAPSINT32, MRV);
//        validateErrMsg("integer", INT32NS, "41", CAPSINT32, MRV);
//        validateErrMsg("integer", INT32NS, "49", CAPSINT32, MRV);
//        validateErrMsg("integer", INT32NS, "101", CAPSINT32, MRV);
//    }
//
//    /**
//     * Tests all possible negative scenario's for unsigned integer32 data type
//     * with range "10..40 | 50..100".
//     */
//    @Test
//    public void negative4Test() throws YdtException {
//        validateErrMsg("UnInteger", INT32NS, "9", CAPSUINT32, MRV);
//        validateErrMsg("UnInteger", INT32NS, "41", CAPSUINT32, MRV);
//        validateErrMsg("UnInteger", INT32NS, "49", CAPSUINT32, MRV);
//        validateErrMsg("UnInteger", INT32NS, "101", CAPSUINT32, MRV);
//    }
//
//    /**
//     * Tests all possible negative scenario's for signed integer32 data type
//     * with range "min .. 2 | 10 | 20..max".
//     */
//    @Test
//    public void negative5Test() throws YdtException {
//        // Multi range validation
//        validateErrMsg("revInteger", INT32NS, "-2147483649", SINT32, MRV);
//        validateErrMsg("revInteger", INT32NS, "4", CAPSINT32, MRV);
//        validateErrMsg("revInteger", INT32NS, "9", CAPSINT32, MRV);
//        validateErrMsg("revInteger", INT32NS, "11", CAPSINT32, MRV);
//        validateErrMsg("revInteger", INT32NS, "19", CAPSINT32, MRV);
//        validateErrMsg("revInteger", INT32NS, "2147483648", SINT32, MRV);
//    }
//
//    /**
//     * Tests all possible negative scenario's for unsigned integer32 data type
//     * with range "min .. 2 | 10 | 20..max".
//     */
//    @Test
//    public void negative6Test() throws YdtException {
//        // Multi range validation
//        validateErrMsg(RUI, INT32NS, "-2147483649", MINVALUE, MRV);
//        validateErrMsg(RUI, INT32NS, "4", CAPSUINT32, MRV);
//        validateErrMsg(RUI, INT32NS, "9", CAPSUINT32, MRV);
//        validateErrMsg(RUI, INT32NS, "11", CAPSUINT32, MRV);
//        validateErrMsg(RUI, INT32NS, "19", CAPSUINT32, MRV);
//        validateErrMsg(RUI, INT32NS, "4294967296", MAXUINT32, MRV);
//    }
}
