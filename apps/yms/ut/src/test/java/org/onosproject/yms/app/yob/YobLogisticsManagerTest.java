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

package org.onosproject.yms.app.yob;

import org.junit.Assert;
import org.junit.Test;
import org.onosproject.yms.app.ydt.YdtTestUtils;
import org.onosproject.yms.app.ydt.YangRequestWorkBench;
import org.onosproject.yms.app.ydt.YdtExtendedContext;
import org.onosproject.yms.ydt.YdtContext;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;

import static org.junit.Assert.assertEquals;

public class YobLogisticsManagerTest {

    @Test
    public void logisticsManagerTest() throws IOException {
        YangRequestWorkBench defaultYdtBuilder = YdtTestUtils
                .logisticsManagerYdt();

        YdtContext rootCtx = defaultYdtBuilder.getRootNode();

        YdtContext childCtx = rootCtx.getFirstChild();

        DefaultYobBuilder builder = new DefaultYobBuilder();

        while (childCtx != null) {

            Object yangObject = builder.getYangObject(
                    (YdtExtendedContext) childCtx, YdtTestUtils
                            .getSchemaRegistry());
            Class<?> aClass = yangObject.getClass();
            if ("CustomssupervisorOpParam".equals(aClass.getSimpleName())) {
                try {
                    Field field = aClass.getDeclaredField("supervisor");
                    Field onosYangNodeOperationType = aClass
                            .getDeclaredField("yangCustomssupervisorOpType");
                    field.setAccessible(true);
                    onosYangNodeOperationType.setAccessible(true);
                    try {
                        assertEquals("abc", field.get(yangObject).toString());
                        assertEquals("MERGE", onosYangNodeOperationType
                                .get(yangObject).toString());
                    } catch (IllegalAccessException e) {
                        Assert.fail();
                    }
                } catch (NoSuchFieldException e) {
                    Assert.fail();
                }
            }

            if ("MerchandisersupervisorOpParam".equals(aClass.getSimpleName())) {
                try {
                    Field field = aClass.getDeclaredField("supervisor");
                    field.setAccessible(true);
                    try {
                        assertEquals("abc", field.get(yangObject).toString());
                    } catch (IllegalAccessException e) {
                        Assert.fail();
                    }
                } catch (NoSuchFieldException e) {
                    Assert.fail();
                }
            }

            if ("WarehousesupervisorOpParam".equals(aClass.getSimpleName())) {
                try {
                    Field field = aClass.getDeclaredField("supervisor");
                    field.setAccessible(true);
                    try {
                        ArrayList<String> arrayList =
                                (ArrayList<String>) field.get(yangObject);
                        assertEquals("1", arrayList.get(0));
                        assertEquals("2", arrayList.get(1));
                        assertEquals("3", arrayList.get(2));
                        assertEquals("4", arrayList.get(3));
                        assertEquals("5", arrayList.get(4));
                    } catch (IllegalAccessException e) {
                        Assert.fail();
                    }
                } catch (NoSuchFieldException e) {
                    Assert.fail();
                }
            }
            childCtx = childCtx.getNextSibling();
        }
    }
}
