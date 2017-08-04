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

/**
 * Set of abstractions for conveying high-level intents for treatment of
 * selected network traffic by allowing applications to express the
 * <em>what</em> rather than the <em>how</em>. This makes such instructions
 * largely independent of topology and device specifics, thus allowing them to
 * survive topology mutations.
 * <p>
 * The controller core provides a suite of built-in intents and their compilers
 * and installers. However, the intent framework is extensible in that it allows
 * additional intents and their compilers or installers to be added
 * dynamically at run-time. This allows others to enhance the initial arsenal of
 * connectivity and policy-based intents available in base controller software.
 * </p>
 * <p>
 * The following diagram depicts the state transition diagram for each top-level intent:<br>
 * <img src="doc-files/intent-states.png" alt="ONOS intent states">
 * </p>
 * <p>
 * The controller core accepts the intent specifications and translates them, via a
 * process referred to as intent compilation, to installable intents, which are
 * essentially actionable operations on the network environment.
 * These actions are carried out by intent installation process, which results
 * in some changes to the environment, e.g. tunnel links being provisioned,
 * flow rules being installed on the data-plane, optical lambdas being reserved.
 * </p>
 * <p>
 * After an intent is submitted by an application, it will be sent immediately
 * (but asynchronously) into a compiling phase, then to installing phase and if
 * all goes according to plan into installed state. Once an application decides
 * it no longer wishes the intent to hold, it can withdraw it. This describes
 * the nominal flow. However, it may happen that some issue is encountered.
 * For example, an application may ask for an objective that is not currently
 * achievable, e.g. connectivity across to unconnected network segments.
 * If this is the case, the compiling phase may fail to produce a set of
 * installable intents and instead result in a failed compile. If this occurs,
 * only a change in the environment can trigger a transition back to the
 * compiling state.
 * </p>
 * <p>
 * Similarly, an issue may be encountered during the installation phase in
 * which case the framework will attempt to recompile the intent to see if an
 * alternate approach is available. If so, the intent will be sent back to
 * installing phase. Otherwise, it will be parked in the failed state. Another
 * scenario that’s very likely to be encountered is where the intent is
 * successfully compiled and installed, but due to some topology event, such
 * as a downed or downgraded link, loss of throughput may occur or connectivity
 * may be lost altogether, thus impacting the viability of a previously
 * satisfied intent. If this occurs, the framework will attempt to recompile
 * the intent, and if an alternate approach is available, its installation
 * will be attempted. Otherwise, the original top-level intent will be parked
 * in the failed state.
 * </p>
 * <p>
 * Please note that all *ing states, depicted in orange, are transitional and
 * are expected to last only a brief amount of time. The rest of the states
 * are parking states where the intent may spent some time; except for the
 * submitted state of course. There, the intent may pause, but only briefly,
 * while the system determines where to perform the compilation or while it
 * performs global recomputation/optimization across all prior intents.
 * </p>
 * <p>
 * The figure below depicts the general interactions between different
 * components of the intent subsystem.<br>
 * <img src="doc-files/intent-design.png" alt="ONOS intent subsystem design">
 * </p>
 */
package org.onosproject.net.intent;
