/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onlab.jdvue;

import com.google.common.testing.EqualsTester;
import org.junit.Test;
import org.onlab.jdvue.Dependency;
import org.onlab.jdvue.JavaPackage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * Unit test for the dependency entity.
 *
 * @author Thomas Vachuska
 */
public class DependencyTest {

    @Test
    public void basics() {
        JavaPackage x = new JavaPackage("x");
        JavaPackage y = new JavaPackage("y");

        new EqualsTester()
                .addEqualityGroup(new Dependency(x, y), new Dependency(x, y))
                .addEqualityGroup(new Dependency(y, x), new Dependency(y, x))
                .testEquals();
    }

}
