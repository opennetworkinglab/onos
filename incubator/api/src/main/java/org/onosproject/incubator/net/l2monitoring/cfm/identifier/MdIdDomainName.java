/*
 * Copyright 2017-present Open Networking Foundation
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
package org.onosproject.incubator.net.l2monitoring.cfm.identifier;

import org.onlab.util.Identifier;

import com.google.common.net.InternetDomainName;

/**
 * A representation of a domain name as an MD identifier.
 */
public class MdIdDomainName extends Identifier<InternetDomainName> implements MdId {

    protected MdIdDomainName(InternetDomainName mdDomainName) {
        super(mdDomainName);
    }

    @Override
    public String mdName() {
        return identifier.toString();
    }

    @Override
    public int getNameLength() {
        return identifier.toString().length();
    }

    public static MdId asMdId(String mdName) {
        if (mdName == null || !InternetDomainName.isValid(mdName)) {
            throw new IllegalArgumentException("MD Name must follow internet domain name pattern "
                    + " Rejecting: " + mdName);
        }
        return asMdId(InternetDomainName.from(mdName));
    }

    @Override
    public MdNameType nameType() {
        return MdNameType.DOMAINNAME;
    }


    public static MdId asMdId(InternetDomainName mdName) {
        if (mdName == null || mdName.toString().length() < MD_NAME_MIN_LEN ||
                mdName.toString().length() > MD_NAME_MAX_LEN) {
            throw new IllegalArgumentException("MD Domain Name must be between " +
                    MD_NAME_MIN_LEN + " and " + MD_NAME_MAX_LEN + " chars long"
                    + " Rejecting: " + mdName);
        }
        return new MdIdDomainName(mdName);
    }
}
