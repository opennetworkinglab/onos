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
package org.onosproject.lisp.msg.protocols;

import org.onosproject.lisp.msg.types.lcaf.LispNatLcafAddress;

/**
 * LISP info reply message interface.
 * <p>
 * LISP info reply message format is defined in draft-ermagan-lisp-nat-traversal-11.
 * https://tools.ietf.org/html/draft-ermagan-lisp-nat-traversal-11#page-9
 *
 * <pre>
 * {@literal
 *     0                   1                   2                   3
 *     0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *    |Type=7 |R|               Reserved                              |
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *    |                         Nonce . . .                           |
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *    |                      . . . Nonce                              |
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *    |            Key ID             |  Authentication Data Length   |
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *    ~                     Authentication Data                       ~
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *    |                              TTL                              |
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *    |   Reserved    | EID mask-len  |        EID-prefix-AFI         |
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *    |                          EID-prefix                           |
 * +->+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |  |           AFI = 16387         |    Rsvd1      |     Flags     |
 * |  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |  |    Type = 7     |     Rsvd2   |             4 + n             |
 * |  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * N  |        MS UDP Port Number     |      ETR UDP Port Number      |
 * A  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * T  |              AFI = x          | Global ETR RLOC Address  ...  |
 *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * L  |              AFI = x          |       MS RLOC Address  ...    |
 * C  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * A  |              AFI = x          | Private ETR RLOC Address ...  |
 * F  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |  |              AFI = x          |      RTR RLOC Address 1 ...   |
 * |  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |  |              AFI = x          |       RTR RLOC Address n ...  |
 * +->+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * }</pre>
 */
public interface LispInfoReply extends LispInfo {

    /**
     * Obtains NAT LCAF address.
     *
     * @return NAT LCAF address
     */
    LispNatLcafAddress getNatLcafAddress();

    interface InfoReplyBuilder extends InfoBuilder<InfoReplyBuilder> {

        /**
         * Sets NAT LCAF address.
         *
         * @param natLcafAddress NAT LCAF address
         * @return InfoReplyBuilder object
         */
        InfoReplyBuilder withNatLcafAddress(LispNatLcafAddress natLcafAddress);

        /**
         * Builds LISP info reply message.
         *
         * @return LISP info reply message
         */
        LispInfoReply build();
    }
}
