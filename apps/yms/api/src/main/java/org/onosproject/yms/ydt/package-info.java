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

/**
 * Provides interfaces to build and obtain YANG data tree which is data
 * (sub)instance representation, abstract of protocol.
 *
 * NBI protocol implementation takes care of the protocol specific
 * operations. They are abstracted from the intricacies of understanding
 * the application identification or handling the interaction with
 * applications.
 *
 * NBI protocols need to handle the encoding and decoding of data to the
 * protocol specific format. They are unaware of the YANG of applications,
 * i.e. protocols are unaware of the data structure / organization in
 * applications.
 *
 * They need to translate the protocol operation request, into a protocol
 * independent abstract tree called the YANG data tree (YDT). In order to
 * enable the protocol in building these abstract data tree, YANG
 * management system provides a utility called the YANG data tree builder.
 *
 * Using the YANG data tree utility API's protocols are expected to walk
 * the data received in request and pass the information during the walk.
 * YANG data tree builder, identifies the application which supports the
 * request and validates it against the schema defined in YANG, and
 * constructs the abstract YANG data tree.
 */
package org.onosproject.yms.ydt;
