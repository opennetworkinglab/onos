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

package org.onosproject.yangutils.datamodel;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import org.onosproject.yangutils.datamodel.exceptions.DataModelException;
import org.onosproject.yangutils.datamodel.utils.Parsable;
import org.onosproject.yangutils.datamodel.utils.ResolvableStatus;
import org.onosproject.yangutils.datamodel.utils.YangConstructType;

/*
 * Reference RFC 6020.
 *
 *  The "if-feature" statement makes its parent statement conditional.
 *  The argument is the name of a feature, as defined by a "feature"
 *  statement.  The parent statement is implemented by servers that
 *  support this feature.  If a prefix is present on the feature name, it
 *  refers to a feature defined in the module that was imported with that
 *  prefix, or the local module if the prefix matches the local module's
 *  prefix.  Otherwise, a feature with the matching name MUST be defined
 *  in the current module or an included submodule.
 *
 *  Since submodules cannot include the parent module, any features in
 *  the module that need to be exposed to submodules MUST be defined in a
 *  submodule.  Submodules can then include this submodule to find the
 *  definition of the feature.
 */

/**
 * Represents data model node to maintain information defined in YANG if-feature.
 */
public class YangIfFeature implements Parsable, Resolvable, Serializable {

    private static final long serialVersionUID = 806201635L;

    /**
     * if-feature argument.
     */
    YangNodeIdentifier name;

    /**
     * Referred feature information.
     */
    YangFeature referredFeature;

    /**
     * Referred feature parent information.
     */
    YangNode referredFeatureHolder;

    /**
     * Status of resolution. If completely resolved enum value is "RESOLVED",
     * if not enum value is "UNRESOLVED", in case reference of grouping/typedef
     * is added to uses/type but it's not resolved value of enum should be
     * "INTRA_FILE_RESOLVED".
     */
    private ResolvableStatus resolvableStatus;

    /**
     * Returns referred feature holder.
     *
     * @return referred feature holder
     */
    public YangNode getReferredFeatureHolder() {
        return referredFeatureHolder;
    }

    /**
     * Sets the referred feature holder.
     *
     * @param referredFeatureHolder referred feature holder
     */
    public void setReferredFeatureHolder(YangNode referredFeatureHolder) {
        this.referredFeatureHolder = referredFeatureHolder;
    }

    /**
     * Returns prefix associated with identifier.
     *
     * @return prefix associated with identifier
     */
    public String getPrefix() {
        return name.getPrefix();
    }

    /**
     * Sets prefix associated with identifier.
     *
     * @param prefix prefix associated with identifier
     */
    public void setPrefix(String prefix) {
        name.setPrefix(prefix);
    }

    /**
     * Returns referred feature associated with if-feature.
     *
     * @return referred feature associated with if-feature
     */
    public YangFeature getReferredFeature() {
        return referredFeature;
    }

    /**
     * Sets referred feature associated with if-feature.
     *
     * @param referredFeature referred feature associated with if-feature
     */
    public void setReferredFeature(YangFeature referredFeature) {
        this.referredFeature = referredFeature;
    }

    /**
     * Returns the YANG name of if-feature.
     *
     * @return the name of if-feature as defined in YANG file
     */
    public YangNodeIdentifier getName() {
        return name;
    }

    /**
     * Sets the YANG name of if-feature.
     *
     * @param name the name of if-feature as defined in YANG file
     */
    public void setName(YangNodeIdentifier name) {
        this.name = name;
    }

    @Override
    public YangConstructType getYangConstructType() {
        return YangConstructType.IF_FEATURE_DATA;
    }

    @Override
    public void validateDataOnEntry() throws DataModelException {
        // do nothing, no validation required for if-feature
    }

    @Override
    public void validateDataOnExit() throws DataModelException {
        // do nothing, no validation required for if-feature
    }

    @Override
    public ResolvableStatus getResolvableStatus() {
        return resolvableStatus;
    }

    @Override
    public void setResolvableStatus(ResolvableStatus resolvableStatus) {
        this.resolvableStatus = resolvableStatus;
    }

    @Override
    public Object resolve() throws DataModelException {
        YangFeature feature = getReferredFeature();

        // check whether feature has if-feature
        List<YangIfFeature> ifFeatureList = feature.getIfFeatureList();
        if (ifFeatureList != null && !ifFeatureList.isEmpty()) {
            Iterator<YangIfFeature> ifFeatureIterator = ifFeatureList.iterator();
            while (ifFeatureIterator.hasNext()) {
                YangIfFeature ifFeature = ifFeatureIterator.next();
                if (ifFeature.getResolvableStatus() != ResolvableStatus.RESOLVED) {
                    setResolvableStatus(ResolvableStatus.INTRA_FILE_RESOLVED);
                    return null;
                }
            }
        }
        return null;
    }
}
