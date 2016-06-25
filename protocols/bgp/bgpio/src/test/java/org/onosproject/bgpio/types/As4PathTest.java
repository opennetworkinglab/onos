/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.bgpio.types;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.google.common.testing.EqualsTester;

/**
 * Test for As4Path BGP Path Attribute.
 */
public class As4PathTest {
    //Two scenarios as4path set and sequence
    private final List<Integer> as4pathSet1 = new ArrayList<>();
    private final List<Integer> as4pathSeq1 = new ArrayList<>();
    private final List<Integer> as4pathSet2 = new ArrayList<>();
    private final List<Integer> as4pathSeq2 = new ArrayList<>();
    private final As4Path attr1 = new As4Path(as4pathSet1, null);
    private final As4Path sameAsAttr1 = new As4Path(as4pathSet1, null);
    private final As4Path attr2 = new As4Path(as4pathSet2, null);
    private final As4Path attr3 = new As4Path(null, as4pathSeq1);
    private final As4Path sameAsAttr3 = new As4Path(null, as4pathSeq1);
    private final As4Path attr4 = new As4Path(null, as4pathSeq2);

    @Test
    public void basics() {
        as4pathSet1.add(197358);
        as4pathSet1.add(12883);
        as4pathSet2.add(2008989);
        as4pathSeq1.add(3009009);
        as4pathSeq2.add(409900);
        new EqualsTester()
        .addEqualityGroup(attr1, sameAsAttr1)
        .addEqualityGroup(attr2)
        .addEqualityGroup(attr3, sameAsAttr3)
        .addEqualityGroup(attr4)
        .testEquals();
    }
}
