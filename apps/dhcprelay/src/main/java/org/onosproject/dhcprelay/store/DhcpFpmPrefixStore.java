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

package org.onosproject.dhcprelay.store;
import org.onosproject.routing.fpm.api.FpmRecord;
import org.onosproject.routing.fpm.api.FpmPrefixStore;



import org.onlab.packet.IpPrefix;
import java.util.Optional;



/**
 * Interface to store DhcpFpm records.
 */

public interface DhcpFpmPrefixStore extends FpmPrefixStore {


    /**
     * Add a dhcp fpm record.
     *
     * @param prefix the route prefix in the advertisement
     * @param fpmRecord the route for fpm
     **/
    public void addFpmRecord(IpPrefix prefix, FpmRecord fpmRecord);

    /**
     * Remove a dhcp fpm entry
     *  and return the removed record; return empty value if not exists.
     *
     * @param prefix the route prefix in the advertisement
     * @return none
     **/
    public Optional<FpmRecord> removeFpmRecord(IpPrefix prefix);
}
