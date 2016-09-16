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
package org.onosproject.net.utils;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Objects;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.onosproject.net.Annotations;
import org.onosproject.net.Element;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;

import com.google.common.annotations.Beta;
import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;

/**
 * A Port which forwards all its method calls to another Port.
 */
@Beta
public abstract class ForwardingPort implements Port {

    private final Port delegate;

    protected ForwardingPort(Port delegate) {
        this.delegate = checkNotNull(delegate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(element().id(),
                            number(),
                            isEnabled(),
                            type(),
                            portSpeed(),
                            annotations());
    }

    /**
     * Returns {@link EqualsBuilder} comparing all Port attributes
     * including annotations.
     * <p>
     * To add extra fields to equality,
     * call {@code super.toEqualsBuilder(..)} and append fields.
     * To remove field from comparison, override this method
     * or manually implement equals().
     *
     * @param that object to compare to
     * @return builder object
     */
    protected EqualsBuilder toEqualsBuilder(Port that) {
        if (that == null) {
            return new EqualsBuilder().appendSuper(false);
        }
        return new EqualsBuilder()
                .append(this.element().id(), that.element().id())
                .append(this.number(), that.number())
                .append(this.isEnabled(), that.isEnabled())
                .append(this.type(), that.type())
                .append(this.portSpeed(), that.portSpeed())
                .append(this.annotations(), that.annotations());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj != null && getClass() == obj.getClass()) {
            final ForwardingPort that = (ForwardingPort) obj;
            return toEqualsBuilder(that)
                    .isEquals();
        }
        return false;
    }

    /**
     * Returns {@link ToStringHelper} with Port attributes excluding annotations.
     *
     * @return {@link ToStringHelper}
     */
    protected ToStringHelper toStringHelper() {
        return MoreObjects.toStringHelper(this)
                .add("element", element().id())
                .add("number", number())
                .add("isEnabled", isEnabled())
                .add("type", type())
                .add("portSpeed", portSpeed());
    }

    @Override
    public String toString() {
        return toStringHelper()
                .toString();
    }

    @Override
    public Annotations annotations() {
        return delegate.annotations();
    }

    @Override
    public Element element() {
        return delegate.element();
    }

    @Override
    public PortNumber number() {
        return delegate.number();
    }

    @Override
    public boolean isEnabled() {
        return delegate.isEnabled();
    }

    @Override
    public Port.Type type() {
        return delegate.type();
    }

    @Override
    public long portSpeed() {
        return delegate.portSpeed();
    }

}
