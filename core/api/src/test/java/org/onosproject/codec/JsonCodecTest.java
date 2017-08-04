/*
 * Copyright 2014-present Open Networking Foundation
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
package org.onosproject.codec;

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
        public ObjectNode encode(Foo entity, CodecContext context) {
            return context.mapper().createObjectNode().put("name", entity.name);
        }

        @Override
        public Foo decode(ObjectNode json, CodecContext context) {
            return new Foo(json.get("name").asText());
        }
    }

    @Test
    public void encode() {
        Foo f1 = new Foo("foo");
        Foo f2 = new Foo("bar");
        FooCodec codec = new FooCodec();
        ImmutableList<Foo> entities = ImmutableList.of(f1, f2);
        ArrayNode json = codec.encode(entities, new TestContext());
        List<Foo> foos = codec.decode(json, new TestContext());
        assertEquals("incorrect encode/decode", entities, foos);
    }

    private class TestContext implements CodecContext {
        private ObjectMapper mapper = new ObjectMapper();
        @Override
        public ObjectMapper mapper() {
            return mapper;
        }

        @Override
        public <T> JsonCodec<T> codec(Class<T> entityClass) {
            return null;
        }

        @Override
        public <T> T getService(Class<T> serviceClass) {
            return null;
        }
    }
}
