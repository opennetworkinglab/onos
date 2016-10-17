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

package org.onosproject.yms.app.yab;

import org.onosproject.yang.gen.v1.ydt.test.rev20160524.Test;
import org.onosproject.yang.gen.v1.ydt.test.rev20160524.TestOpParam;
import org.onosproject.yang.gen.v1.ydt.test.rev20160524.TestService;
import org.onosproject.yang.gen.v1.ydt.test.rev20160524.test.Cont1;
import org.onosproject.yang.gen.v1.ydt.test.rev20160524.test.DefaultCont1;
import org.onosproject.yang.gen.v1.ydt.test.rev20160524.test.rockthehouse.DefaultRockTheHouseOutput;
import org.onosproject.yang.gen.v1.ydt.test.rev20160524.test.rockthehouse.RockTheHouseInput;
import org.onosproject.yang.gen.v1.ydt.test.rev20160524.test.rockthehouse.RockTheHouseOutput;
import org.onosproject.yang.gen.v1.ydt.test.rev20160524.test.rockthehouse1.RockTheHouse1Input;
import org.onosproject.yang.gen.v1.ydt.test.rev20160524.test.rockthehouse2.DefaultRockTheHouse2Output;
import org.onosproject.yang.gen.v1.ydt.test.rev20160524.test.rockthehouse2.RockTheHouse2Output;

/**
 * Implementation of the application management service.
 */
public class TestManager implements TestService {

    Test response;

    @Override
    public Test getTest(TestOpParam test) {
        Cont1 cont = new DefaultCont1.Cont1Builder().leaf4("4").build();
        Test response = new TestOpParam.TestBuilder().cont1(cont).build();
        return response;
    }

    @Override
    public void setTest(TestOpParam test) {
        response = test;
    }

    @Override
    public Test getAugmentedTestCont4(TestOpParam test) {
        Cont1 cont = new DefaultCont1.Cont1Builder().leaf4("4").build();
        Test response = new TestOpParam.TestBuilder().cont1(cont).build();
        return response;
    }

    @Override
    public void setAugmentedTestCont4(TestOpParam augmentedTestCont4) {
        response = augmentedTestCont4;
    }

    @Override
    public RockTheHouseOutput rockTheHouse(RockTheHouseInput inputVar) {
        return DefaultRockTheHouseOutput.builder().hello("hello").build();
    }


    @Override
    public void rockTheHouse1(RockTheHouse1Input inputVar) {
        // TODO : to be implemented
    }

    @Override
    public RockTheHouse2Output rockTheHouse2() {
        return DefaultRockTheHouse2Output
                .builder().leaf14("14").build();
    }

    @Override
    public void rockTheHouse3() {
    }
}
