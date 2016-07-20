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
package org.onosproject.pce.pcestore;

import com.google.common.base.MoreObjects;

import java.util.List;
import java.util.Objects;

import org.onosproject.net.DeviceId;
import org.onosproject.net.intent.Constraint;
import org.onosproject.pce.pceservice.LspType;

/**
 * Input path information to compute CSPF path.
 * This path information will be stored in pce store and will be used later to recalculate the path.
 */
public final class PcePathInfo {

    private DeviceId src; // source path

    private DeviceId dst; // destination path

    private String name; // tunnel name

    private List<Constraint> constraints; // list of constraints (cost, bandwidth, etc.)

    private LspType lspType; // lsp type

    /**
     * Initialization of member variables.
     *
     * @param src source device id
     * @param dst destination device id
     * @param name tunnel name
     * @param constraints list of constraints
     * @param lspType lsp type
     */
    public PcePathInfo(DeviceId src,
                    DeviceId dst,
                    String name,
                    List<Constraint> constraints,
                    LspType lspType) {
       this.src = src;
       this.dst = dst;
       this.name = name;
       this.constraints = constraints;
       this.lspType = lspType;
    }

    /**
     * Initialization for serialization.
     */
    public PcePathInfo() {
       this.src = null;
       this.dst = null;
       this.name = null;
       this.constraints = null;
       this.lspType = null;
    }

    /**
     * Returns source device id.
     *
     * @return source device id
     */
    public DeviceId src() {
       return src;
    }

    /**
     * Sets source device id.
     *
     * @param id source device id
     */
    public void src(DeviceId id) {
        this.src = id;
    }

    /**
     * Returns destination device id.
     *
     * @return destination device id
     */
    public DeviceId dst() {
       return dst;
    }

    /**
     * Sets destination device id.
     *
     * @param id destination device id
     */
    public void dst(DeviceId id) {
        this.dst = id;
    }


    /**
     * Returns tunnel name.
     *
     * @return name
     */
    public String name() {
       return name;
    }

    /**
     * Sets tunnel name.
     *
     * @param name tunnel name
     */
    public void name(String name) {
        this.name = name;
    }

    /**
     * Returns list of constraints including cost, bandwidth, etc.
     *
     * @return list of constraints
     */
    public List<Constraint> constraints() {
       return constraints;
    }

    /**
     * Sets list of constraints.
     * @param constraints list of constraints
     */
    public void constraints(List<Constraint> constraints) {
        this.constraints = constraints;
    }

    /**
     * Returns lsp type.
     *
     * @return lsp type
     */
    public LspType lspType() {
       return lspType;
    }

    /**
     * Sets lsp type.
     *
     * @param lspType lsp type
     */
    public void lspType(LspType lspType) {
        this.lspType = lspType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(src, dst, name, constraints, lspType);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof PcePathInfo) {
            final PcePathInfo other = (PcePathInfo) obj;
            return Objects.equals(this.src, other.src) &&
                    Objects.equals(this.dst, other.dst) &&
                    Objects.equals(this.name, other.name) &&
                    Objects.equals(this.constraints, other.constraints) &&
                    Objects.equals(this.lspType, other.lspType);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .omitNullValues()
                .add("Source", src.toString())
                .add("Destination", dst.toString())
                .add("Name", name.toString())
                .add("Constraints", constraints.toString())
                .add("LspType", lspType.toString())
                .toString();
    }
}
