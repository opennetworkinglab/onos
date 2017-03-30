/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.bgp.controller.impl;

import org.onosproject.bgpio.protocol.linkstate.PathAttrNlriDetailsLocalRib;
import org.onosproject.bgpio.types.AsPath;
import org.onosproject.bgpio.types.BgpValueType;
import org.onosproject.bgpio.types.LocalPref;
import org.onosproject.bgpio.types.Med;
import org.onosproject.bgpio.types.Origin;
import org.onosproject.bgpio.types.Origin.OriginType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;

/**
 * Implementation of BGP best path Selection process.
 */
public final class BgpSelectionAlgo implements Comparator<PathAttrNlriDetailsLocalRib> {
    private static final Logger log = LoggerFactory.getLogger(BgpSelectionAlgo.class);
    LocalPref obj1LocPref = null;
    AsPath obj1Aspath = null;
    Origin obj1Origin = null;
    Med obj1Med = null;
    LocalPref obj2LocPref = null;
    AsPath obj2Aspath = null;
    Origin obj2Origin = null;
    Med obj2Med = null;

    @Override
    public int compare(PathAttrNlriDetailsLocalRib pathNlriDetails1, PathAttrNlriDetailsLocalRib pathNlriDetails2) {
        if (pathNlriDetails1 == null) {
            return -1;
        }
        if (pathNlriDetails2 == null) {
            return 1;
        }
        if (pathNlriDetails1.equals(pathNlriDetails2)) {
            return 0;
        }

        List<BgpValueType> o1 = pathNlriDetails1.localRibNlridetails().pathAttributes();
        List<BgpValueType> o2 = pathNlriDetails2.localRibNlridetails().pathAttributes();
        ListIterator<BgpValueType> listIteratorObj1 = o1.listIterator();
        ListIterator<BgpValueType> listIteratorObj2 = o2.listIterator();
        storeAttr(listIteratorObj1, listIteratorObj2);

        // prefer attribute with higher local preference
        if (obj1LocPref != null || obj2LocPref != null && (obj1LocPref != null && !obj1LocPref.equals(obj2LocPref))) {
            return compareLocalPref(obj1LocPref, obj2LocPref);
        }

        // prefer attribute with shortest Aspath
        if (!obj1Aspath.equals(obj2Aspath)) {
            Integer obj1Size = countASSize(obj1Aspath);
            Integer obj2Size = countASSize(obj2Aspath);
            if (!obj1Size.equals(obj2Size)) {
                return compareAsPath(obj1Size, obj2Size);
            }
        }

        // prefer attribute with lowest origin type
        if (!obj1Origin.equals(obj2Origin)) {
            return compareOrigin(obj1Origin, obj2Origin);
        }

        // prefer attribute with lowest MED
        if (obj1Med != null || obj2Med != null && (obj1Med != null && !obj1Med.equals(obj2Med))) {
            return compareMed(obj1Med, obj2Med);
        }

        if (!pathNlriDetails1.equals(pathNlriDetails2)) {
            return comparePeerDetails(pathNlriDetails1, pathNlriDetails2);
        }
        return 0;
    }

    /**
     * Compares local preference of two objects and returns object with higher preference.
     *
     * @param obj1LocPref local preference object1
     * @param obj2LocPref local preference object2
     * @return object with higher preference
     */
    int compareLocalPref(LocalPref obj1LocPref, LocalPref obj2LocPref) {
            return ((Integer) (obj1LocPref.localPref())).compareTo((Integer) (obj2LocPref.localPref()));
    }

    /**
     * Compares AsPath of two objects and returns object with shortest AsPath.
     *
     * @param obj1Size object1 AS count
     * @param obj2Size object2 AS count
     * @return object with shortest AsPath
     */
    int compareAsPath(Integer obj1Size, Integer obj2Size) {
            return obj1Size.compareTo(obj2Size);
    }

    /**
     * Compare Origin of two objects and returns object with lowest origin value.
     *
     * @param obj1Origin Origin object1
     * @param obj2Origin Origin object1
     * @return object with lowest origin value
     */
    int compareOrigin(Origin obj1Origin, Origin obj2Origin) {
        if (obj1Origin.origin() == OriginType.IGP) {
            return 1;
        }
        if (obj2Origin.origin() == OriginType.IGP) {
            return -1;
        }
        if (obj1Origin.origin() == OriginType.EGP) {
            return 1;
        } else {
            return -1;
        }
    }

    /**
     * Compare Med of two objects and returns object with lowestMed value.
     *
     * @param obj1Med Med object1
     * @param obj2Med Med object2
     * @return returns object with lowestMed value
     */
    int compareMed(Med obj1Med, Med obj2Med) {
        return ((Integer) (obj2Med.med())).compareTo((Integer) (obj1Med.med()));
    }

    /**
     * Compares EBGP over IBGP, BGP identifier value and peer address.
     *
     * @param pathNlriDetails1 PathAttrNlriDetailsLocalRib object1
     * @param pathNlriDetails2 PathAttrNlriDetailsLocalRib object2
     * @return object which as EBGP over IBGP, lowest BGP identifier value and lowest peer address
     */
    int comparePeerDetails(PathAttrNlriDetailsLocalRib pathNlriDetails1, PathAttrNlriDetailsLocalRib pathNlriDetails2) {
        // consider EBGP over IBGP
        if (pathNlriDetails1.isLocalRibIbgpSession() != pathNlriDetails2.isLocalRibIbgpSession()) {
            if (pathNlriDetails1.isLocalRibIbgpSession()) {
                return -1;
            }
            if (pathNlriDetails2.isLocalRibIbgpSession()) {
                return 1;
            }
        }
        // prefer lowest BGP identifier value.
        if (pathNlriDetails1.localRibIdentifier() != pathNlriDetails2.localRibIdentifier()) {
            return ((Integer) pathNlriDetails2.localRibIdentifier())
                    .compareTo(pathNlriDetails1.localRibIdentifier());
        }
        //prefer lowest peer address
        if (pathNlriDetails1.localRibIpAddress() != pathNlriDetails2.localRibIpAddress()) {
            return pathNlriDetails2.localRibIpAddress().compareTo(pathNlriDetails1.localRibIpAddress());
        }
        return 0;
    }

    /**
     * Returns ASes count of AsPath attribute , if AS_SET is present then count as 1.
     *
     * @param aspath object of AsPath
     * @return count of ASes
     */
    Integer countASSize(AsPath aspath) {
        boolean isASSet = false;
        int count = 0;
        if (!aspath.asPathSet().isEmpty()) {
            isASSet = true;
        }
        if (!aspath.asPathSeq().isEmpty()) {
            count = aspath.asPathSeq().size();
        }
        return isASSet ? ++count : count;
    }

    /**
     * Stores BGP basic attributes of two objects.
     *
     * @param listIteratorObj1 list iterator of object1
     * @param listIteratorObj2 list iterator of object2
     */
    void storeAttr(ListIterator<BgpValueType> listIteratorObj1, ListIterator<BgpValueType> listIteratorObj2) {
         while (listIteratorObj1.hasNext()) {
             BgpValueType pathAttributeObj1 = listIteratorObj1.next();
             switch (pathAttributeObj1.getType()) {
             case LocalPref.LOCAL_PREF_TYPE:
                 obj1LocPref = (LocalPref) pathAttributeObj1;
                 break;
             case AsPath.ASPATH_TYPE:
                 obj1Aspath = (AsPath) pathAttributeObj1;
                 break;
             case Origin.ORIGIN_TYPE:
                 obj1Origin = (Origin) pathAttributeObj1;
                 break;
             case Med.MED_TYPE:
                 obj1Med = (Med) pathAttributeObj1;
                 break;
             default:
                 log.debug("Got other type, Not required: " + pathAttributeObj1.getType());
             }
         }
         while (listIteratorObj2.hasNext()) {
             BgpValueType pathAttributeObj2 = listIteratorObj2.next();
             switch (pathAttributeObj2.getType()) {
             case LocalPref.LOCAL_PREF_TYPE:
                 obj2LocPref = (LocalPref) pathAttributeObj2;
                 break;
             case AsPath.ASPATH_TYPE:
                 obj2Aspath = (AsPath) pathAttributeObj2;
                 break;
             case Origin.ORIGIN_TYPE:
                 obj2Origin = (Origin) pathAttributeObj2;
                 break;
             case Med.MED_TYPE:
                 obj2Med = (Med) pathAttributeObj2;
                 break;
             default:
                 log.debug("Got other type, Not required: " + pathAttributeObj2.getType());
             }
        }
    }
}
