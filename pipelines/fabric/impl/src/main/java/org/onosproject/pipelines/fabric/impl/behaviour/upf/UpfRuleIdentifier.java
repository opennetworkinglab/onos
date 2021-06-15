/*
 * Copyright 2021-present Open Networking Foundation
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

package org.onosproject.pipelines.fabric.impl.behaviour.upf;

import org.onlab.util.ImmutableByteSequence;

import java.util.Objects;

/**
 * Wrapper for identifying information of FARs and PDRs.
 */
public final class UpfRuleIdentifier {
    private final int sessionlocalId;
    private final ImmutableByteSequence pfcpSessionId;

    /**
     * A PDR or FAR can be globally uniquely identified by the combination of the ID of the PFCP session that
     * produced it, and the ID that the rule was assigned in that PFCP session.
     *
     * @param pfcpSessionId  The PFCP session that produced the rule ID
     * @param sessionlocalId The rule ID
     */
    public UpfRuleIdentifier(ImmutableByteSequence pfcpSessionId, int sessionlocalId) {
        this.pfcpSessionId = pfcpSessionId;
        this.sessionlocalId = sessionlocalId;
    }

    /**
     * Create an instance of this class from the given PFCP session ID and the session-local Rule ID.
     *
     * @param pfcpSessionId  PFCP session ID of the rule to identify
     * @param sessionlocalId session-local Rule ID of the rule to identify
     * @return a new rule identifier
     */
    public static UpfRuleIdentifier of(ImmutableByteSequence pfcpSessionId, int sessionlocalId) {
        return new UpfRuleIdentifier(pfcpSessionId, sessionlocalId);
    }

    /**
     * Get the PFCP session-local rule ID.
     *
     * @return session-local rule ID
     */
    public int getSessionLocalId() {
        return sessionlocalId;
    }

    /**
     * Get the PFCP session ID.
     *
     * @return PFCP session ID
     */
    public ImmutableByteSequence getPfcpSessionId() {
        return pfcpSessionId;
    }

    @Override
    public String toString() {
        return "RuleIdentifier{" +
                "sessionlocalId=" + sessionlocalId +
                ", pfcpSessionId=" + pfcpSessionId +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        UpfRuleIdentifier that = (UpfRuleIdentifier) obj;
        return (this.sessionlocalId == that.sessionlocalId) && (this.pfcpSessionId.equals(that.pfcpSessionId));
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.sessionlocalId, this.pfcpSessionId);
    }
}
