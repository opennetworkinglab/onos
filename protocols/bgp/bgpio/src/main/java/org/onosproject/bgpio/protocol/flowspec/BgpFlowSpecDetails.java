/*
 * Copyright 2016 Open Networking Laboratory
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
package org.onosproject.bgpio.protocol.flowspec;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import org.onosproject.bgpio.types.BgpValueType;
import org.onosproject.bgpio.types.RouteDistinguisher;
import com.google.common.base.MoreObjects;

/**
 * This Class stores flow specification components and action.
 */
public class BgpFlowSpecDetails {
    private List<BgpValueType> flowSpecComponents;
    private List<BgpValueType> fsActionTlv;
    private RouteDistinguisher routeDistinguisher;

    /**
     * Flow specification details object constructor with the parameter.
     *
     * @param flowSpecComponents flow specification components
     */
    public BgpFlowSpecDetails(List<BgpValueType> flowSpecComponents) {
        this.flowSpecComponents = flowSpecComponents;
    }

    /**
     * Flow specification details object constructor.
     *
     */
    public BgpFlowSpecDetails() {

    }

    /**
     * Returns flow specification action tlv.
     *
     * @return flow specification action tlv
     */
    public List<BgpValueType> fsActionTlv() {
        return this.fsActionTlv;
    }

    /**
     * Set flow specification action tlv.
     *
     * @param fsActionTlv flow specification action tlv
     */
    public void setFsActionTlv(List<BgpValueType> fsActionTlv) {
        this.fsActionTlv = fsActionTlv;
    }

    /**
     * Returns route distinguisher for the flow specification components.
     *
     * @return route distinguisher for the flow specification components
     */
    public RouteDistinguisher routeDistinguisher() {
        return this.routeDistinguisher;
    }

    /**
     * Set route distinguisher for flow specification component.
     *
     * @param routeDistinguisher route distinguisher
     */
    public void setRouteDistinguiher(RouteDistinguisher routeDistinguisher) {
        this.routeDistinguisher = routeDistinguisher;
    }

    /**
     * Returns flow specification components.
     *
     * @return flow specification components
     */
    public List<BgpValueType> flowSpecComponents() {
        return this.flowSpecComponents;
    }

    /**
     * Sets flow specification components.
     *
     * @param flowSpecComponents flow specification components
     */
    public void setFlowSpecComponents(List<BgpValueType> flowSpecComponents) {
        this.flowSpecComponents = flowSpecComponents;
    }

    @Override
    public int hashCode() {
        return Objects.hash(flowSpecComponents);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof BgpFlowSpecDetails) {
            int countObjSubTlv = 0;
            int countOtherSubTlv = 0;
            boolean isCommonSubTlv = true;
            BgpFlowSpecDetails other = (BgpFlowSpecDetails) obj;
            Iterator<BgpValueType> objListIterator = other.flowSpecComponents.iterator();
            countOtherSubTlv = other.flowSpecComponents.size();
            countObjSubTlv = flowSpecComponents.size();
            if (countObjSubTlv != countOtherSubTlv) {
                return false;
            } else {
                while (objListIterator.hasNext() && isCommonSubTlv) {
                    BgpValueType subTlv = objListIterator.next();
                    if (flowSpecComponents.contains(subTlv) && other.flowSpecComponents.contains(subTlv)) {
                        isCommonSubTlv = Objects.equals(flowSpecComponents.get(flowSpecComponents.indexOf(subTlv)),
                                            other.flowSpecComponents.get(other.flowSpecComponents.indexOf(subTlv)));
                    } else {
                        isCommonSubTlv = false;
                    }
                }
                return isCommonSubTlv;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("flowSpecComponents", flowSpecComponents)
                .toString();
    }
}
