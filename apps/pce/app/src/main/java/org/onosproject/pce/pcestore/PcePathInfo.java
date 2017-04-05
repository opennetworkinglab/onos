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
import org.onosproject.pce.pceservice.ExplicitPathInfo;
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

    private List<ExplicitPathInfo> explicitPathInfo; //Explicit path info to compute explicit path

    private boolean loadBalancing; //load balancing option

    /**
     * Initialization of member variables.
     *
     * @param src source device id
     * @param dst destination device id
     * @param name tunnel name
     * @param constraints list of constraints
     * @param lspType lsp type
     * @param explicitPathInfo explicit path info
     * @param loadBalancing load balancing option
     */
    public PcePathInfo(DeviceId src,
                    DeviceId dst,
                    String name,
                    List<Constraint> constraints,
                    LspType lspType,
                    List<ExplicitPathInfo> explicitPathInfo,
                    boolean loadBalancing) {
       this.src = src;
       this.dst = dst;
       this.name = name;
       this.constraints = constraints;
       this.lspType = lspType;
       this.explicitPathInfo = explicitPathInfo;
       this.loadBalancing = loadBalancing;
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
       this.explicitPathInfo = null;
       this.loadBalancing = false;
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

    /**
     * Returns list of explicit path info.
     *
     * @return list of explicit path info
     */
    public List<ExplicitPathInfo> explicitPathInfo() {
        return explicitPathInfo;
    }

    /**
     * Sets list of explicit path info.
     *
     * @param explicitPathInfo list of explicit path info
     */
    public void explicitPathInfo(List<ExplicitPathInfo> explicitPathInfo) {
        this.explicitPathInfo = explicitPathInfo;
    }

    /**
     * Returns whether stored path has enabled load balancing.
     *
     * @return load balancing option is enable
     */
    public boolean isLoadBalancing() {
        return loadBalancing;
    }

    /**
     * Sets load balancing option is enable.
     *
     * @param loadBalancing load balancing option is enable
     */
    public void loadBalancing(boolean loadBalancing) {
        this.loadBalancing = loadBalancing;
    }

    @Override
    public int hashCode() {
        return Objects.hash(src, dst, name, constraints, lspType, explicitPathInfo, loadBalancing);
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
                    Objects.equals(this.lspType, other.lspType) &&
                    Objects.equals(this.explicitPathInfo, other.explicitPathInfo) &&
                    Objects.equals(this.loadBalancing, other.loadBalancing);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .omitNullValues()
                .add("Source", src)
                .add("Destination", dst)
                .add("Name", name)
                .add("Constraints", constraints)
                .add("explicitPathInfo", explicitPathInfo)
                .add("LspType", lspType)
                .add("loadBalancing", loadBalancing)
                .toString();
    }
}
