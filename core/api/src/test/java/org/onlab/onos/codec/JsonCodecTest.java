/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.onlab.onos.codec;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;
import org.junit.Test;

import java.util.List;
import java.util.Objects;

import static org.junit.Assert.assertEquals;

/**
 * Test of the base JSON codec abstraction.
 */
public class JsonCodecTest {

    private static class Foo {
        final String name;

        Foo(String name) {
            this.name = name;
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            final Foo other = (Foo) obj;
            return Objects.equals(this.name, other.name);
        }
    }

    private static class FooCodec extends JsonCodec<Foo> {
        @Override
        public ObjectNode encode(Foo entity, ObjectMapper mapper) {
            return mapper.createObjectNode().put("name", entity.name);
        }

        @Override
        public Foo decode(ObjectNode json) {
            return new Foo(json.get("name").asText());
        }
    }

    @Test
    public void encode() {
        Foo f1 = new Foo("foo");
        Foo f2 = new Foo("bar");
        FooCodec codec = new FooCodec();
        ImmutableList<Foo> entities = ImmutableList.of(f1, f2);
        ArrayNode json = codec.encode(entities, new ObjectMapper());
        List<Foo> foos = codec.decode(json);
        assertEquals("incorrect encode/decode", entities, foos);
    }

}