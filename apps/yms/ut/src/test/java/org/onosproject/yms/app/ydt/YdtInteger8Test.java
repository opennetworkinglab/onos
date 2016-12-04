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
import static org.onosproject.yms.app.ydt.YdtTestConstants.MAXUINT8;
import static org.onosproject.yms.app.ydt.YdtTestConstants.MAXUIWR;
import static org.onosproject.yms.app.ydt.YdtTestConstants.MIDIWR;
import static org.onosproject.yms.app.ydt.YdtTestConstants.MIDUIWR;
import static org.onosproject.yms.app.ydt.YdtTestConstants.MINIWR;
import static org.onosproject.yms.app.ydt.YdtTestConstants.MINUIWR;
import static org.onosproject.yms.app.ydt.YdtTestConstants.MINVALUE;
import static org.onosproject.yms.app.ydt.YdtTestConstants.MRV;
import static org.onosproject.yms.app.ydt.YdtTestConstants.RUI;
import static org.onosproject.yms.app.ydt.YdtTestConstants.TYPE;
import static org.onosproject.yms.app.ydt.YdtTestUtils.integer8Ydt;
import static org.onosproject.yms.app.ydt.YdtTestUtils.validateLeafContents;
import static org.onosproject.yms.app.ydt.YdtTestUtils.validateNodeContents;
import static org.onosproject.yms.ydt.YdtContextOperationType.MERGE;

public class YdtInteger8Test {

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
            i.1. input -128
            i.2. input 1
            i.3. input 2
            i.4. input 10
            i.5. input 20
            i.6. input 100
            i.7. input 127

         if range is "min .. 2 | 10 | 20..max" for uInteger
            i.1. input 0
            i.2. input 1
            i.3. input 2
            i.4. input 10
            i.5. input 20
            i.6. input 100
            i.7. input 255
    */

    /**
     * Creates and validates integer8 ydt covering different positive scenario.
     */
    @Test
    public void positiveTest() throws YdtException {
        YangRequestWorkBench ydtBuilder = integer8Ydt();
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
        validateNodeContents(ydtNode, "integer8", MERGE);
        ydtNode = ydtNode.getFirstChild();
        validateLeafContents(ydtNode, "negInt", "-128");
        ydtNode = ydtNode.getNextSibling();
        validateLeafContents(ydtNode, "posInt", "127");
        ydtNode = ydtNode.getNextSibling();
        validateLeafContents(ydtNode, "minUInt", MINVALUE);
        ydtNode = ydtNode.getNextSibling();
        validateLeafContents(ydtNode, "maxUInt", MAXUINT8);
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
        validateLeafContents(ydtNode, "revInteger", "-128");
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
        validate1Tree(ydtNode);
    }

    /**
     * Validates the given built ydt.
     */
    private void validate1Tree(YdtNode ydtNode) {
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
        validateLeafContents(ydtNode, "revInteger", "127");

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
        validateLeafContents(ydtNode, RUI, MAXUINT8);
    }

    //TODO negative scenario will be handled later
    /*
        Negative scenario

        wrong type input
            i. input string instead of integer
            ii. input string instead of uinteger

        input out of range
            i. input for int 8 range -128 to 127
            i.1. input -129
            i.2. input 128

            ii. input for uint 8 range 0 to 255
            ii.1. input -128
            ii.2. input 256

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
            i.1. input -129
            i.2. input 4
            i.3. input 9
            i.4. input 11
            i.5. input 19
            i.6. input 128

        if range is min .. 3 | 10 | 20..max for uinteger
            i.1. input -129
            i.2. input 4
            i.3. input 9
            i.4. input 11
            i.5. input 19
            i.6. input 256

    */

    /**
     * Tests all the minimum and maximum value's negative scenario's for
     * signed integer8 data type.
     */
//    @Test
//    public void negative1Test() throws YdtException {
//        validateErrMsg("posInt", INT8NS, "integer", SMALLINT8, null);
//        validateErrMsg("posInt", INT8NS, "127.0", SMALLINT8, null);
//        validateErrMsg("negInt", INT8NS, "-129", SMALLINT8, null);
//        validateErrMsg("posInt", INT8NS, "128", SMALLINT8, null);
//        validateErrMsg(MINIWR, INT8NS, "9", CAPSINT8, null);
//        validateErrMsg(MAXIWR, INT8NS, "101", CAPSINT8, null);
//    }
//
//    /**
//     * Tests all the minimum and maximum value's negative scenario's for
//     * unsigned integer8 data type.
//     */
//    @Test
//    public void negative2Test() throws YdtException {
//        validateErrMsg("maxUInt", INT8NS, "integer", SMALLUINT8, null);
//        validateErrMsg("maxUInt", INT8NS, "127.0", SMALLUINT8, null);
//        validateErrMsg("minUInt", INT8NS, "-128", MINVALUE, null);
//        validateErrMsg("maxUInt", INT8NS, "256", MAXUINT8, null);
//        validateErrMsg(MINUIWR, INT8NS, "9", CAPSUINT8, null);
//        validateErrMsg(MAXUIWR, INT8NS, "101", CAPSUINT8, null);
//    }
//
//    /**
//     * Tests all possible negative scenario's for signed integer8 data type
//     * with range "10..40 | 50..100".
//     */
//    @Test
//    public void negative3Test() throws YdtException {
//        validateErrMsg("integer", INT8NS, "9", CAPSINT8, MRV);
//        validateErrMsg("integer", INT8NS, "41", CAPSINT8, MRV);
//        validateErrMsg("integer", INT8NS, "49", CAPSINT8, MRV);
//        validateErrMsg("integer", INT8NS, "101", CAPSINT8, MRV);
//    }
//
//    /**
//     * Tests all possible negative scenario's for unsigned integer8 data type
//     * with range "10..40 | 50..100".
//     */
//    @Test
//    public void negative4Test() throws YdtException {
//        validateErrMsg("UnInteger", INT8NS, "9", CAPSUINT8, MRV);
//        validateErrMsg("UnInteger", INT8NS, "41", CAPSUINT8, MRV);
//        validateErrMsg("UnInteger", INT8NS, "49", CAPSUINT8, MRV);
//        validateErrMsg("UnInteger", INT8NS, "101", CAPSUINT8, MRV);
//    }
//
//    /**
//     * Tests all possible negative scenario's for signed integer8 data type
//     * with range "min .. 2 | 10 | 20..max".
//     */
//    @Test
//    public void negative5Test() throws YdtException {
//        // multi range validation
//        validateErrMsg("revInteger", INT8NS, "-129", SMALLINT8, MRV);
//        validateErrMsg("revInteger", INT8NS, "128", SMALLINT8, MRV);
//        validateErrMsg("revInteger", INT8NS, "4", CAPSINT8, MRV);
//        validateErrMsg("revInteger", INT8NS, "11", CAPSINT8, MRV);
//        validateErrMsg("revInteger", INT8NS, "9", CAPSINT8, MRV);
//        validateErrMsg("revInteger", INT8NS, "19", CAPSINT8, MRV);
//    }
//
//    /**
//     * Tests all possible negative scenario's for unsigned integer8 data type
//     * with range "min .. 2 | 10 | 20..max".
//     */
//    @Test
//    public void negative6Test() throws YdtException {
//        // multi range validation
//        validateErrMsg(RUI, INT8NS, "-129", MINVALUE, MRV);
//        validateErrMsg(RUI, INT8NS, "4", CAPSUINT8, MRV);
//        validateErrMsg(RUI, INT8NS, "9", CAPSUINT8, MRV);
//        validateErrMsg(RUI, INT8NS, "11", CAPSUINT8, MRV);
//        validateErrMsg(RUI, INT8NS, "19", CAPSUINT8, MRV);
//        validateErrMsg(RUI, INT8NS, "256", MAXUINT8, MRV);
//    }
}
