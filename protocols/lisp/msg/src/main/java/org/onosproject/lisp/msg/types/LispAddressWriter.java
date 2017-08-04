/*
 * Copyright 2016-present Open Networking Foundation
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
package org.onosproject.lisp.msg.types;

import io.netty.buffer.ByteBuf;
import org.onosproject.lisp.msg.exceptions.LispWriterException;

/**
 * An interface for serializing LISP address.
 */
public interface LispAddressWriter<T> {

    /**
     * Serializes LISP address object and writes to byte buffer.
     *
     * @param byteBuf byte buffer
     * @param address LISP address type instance
     * @throws LispWriterException LISP writer exception
     */
    void writeTo(ByteBuf byteBuf, T address) throws LispWriterException;

}
