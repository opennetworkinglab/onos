/*
 * Copyright 2017-present Open Networking Foundation
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
package org.onosproject.lisp.msg.protocols;

import io.netty.buffer.ByteBuf;
import org.onosproject.lisp.msg.exceptions.LispWriterException;
import org.onosproject.lisp.msg.types.LispAfiAddress;

/**
 * A generic LISP locator used for both location and referral purposes.
 */
public interface LispGenericLocator {

    /**
     * Obtains priority value.
     *
     * @return priority value
     */
    byte getPriority();

    /**
     * Obtains weight value.
     *
     * @return weight value
     */
    byte getWeight();

    /**
     * Obtains multi-cast priority value.
     *
     * @return multi-cast priority value
     */
    byte getMulticastPriority();

    /**
     * Obtains multi-cast weight value.
     *
     * @return multi-cast weight value
     */
    byte getMulticastWeight();

    /**
     * Obtains local locator flag.
     *
     * @return local locator flag
     */
    boolean isLocalLocator();

    /**
     * Obtains RLOC probed flag.
     *
     * @return RLOC probed flag
     */
    boolean isRlocProbed();

    /**
     * Obtains routed flag.
     *
     * @return routed flag
     */
    boolean isRouted();

    /**
     * Obtains locator AFI.
     *
     * @return locator AFI
     */
    LispAfiAddress getLocatorAfi();

    /**
     * Writes LISP message object into communication channel.
     *
     * @param byteBuf byte buffer
     * @throws LispWriterException on error
     */
    void writeTo(ByteBuf byteBuf) throws LispWriterException;

    /**
     * A builder of LISP generic locator.
     *
     * @param <T> sub-builder type
     */
    interface GenericLocatorBuilder<T> {

        /**
         * Sets priority value.
         *
         * @param priority priority
         * @return parameterized object
         */
        T withPriority(byte priority);

        /**
         * Sets weight value.
         *
         * @param weight weight
         * @return parameterized object
         */
        T withWeight(byte weight);

        /**
         * Sets multi-cast priority value.
         *
         * @param priority priority
         * @return parameterized object
         */
        T withMulticastPriority(byte priority);

        /**
         * Sets multi-cast weight value.
         *
         * @param weight weight
         * @return parameterized object
         */
        T withMulticastWeight(byte weight);

        /**
         * Sets local locator flag.
         *
         * @param localLocator local locator flag
         * @return parameterized object
         */
        T withLocalLocator(boolean localLocator);

        /**
         * Sets RLOC probed flag.
         *
         * @param rlocProbed RLOC probed flag
         * @return parameterized object
         */
        T withRlocProbed(boolean rlocProbed);

        /**
         * Sets routed flag.
         *
         * @param routed routed flag
         * @return parameterized object
         */
        T withRouted(boolean routed);

        /**
         * Sets locator AFI.
         *
         * @param locatorAfi locator AFI
         * @return parameterized object
         */
        T withLocatorAfi(LispAfiAddress locatorAfi);
    }
}
