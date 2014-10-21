package org.onlab.onos.config;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Object to store address configuration read from a JSON file.
 */
public class AddressConfiguration {

    private List<AddressEntry> addresses;

    /**
     * Gets a list of addresses in the system.
     *
     * @return the list of addresses
     */
    public List<AddressEntry> getAddresses() {
        return Collections.unmodifiableList(addresses);
    }

    /**
     * Sets a list of addresses in the system.
     *
     * @param addresses the list of addresses
     */
    @JsonProperty("addresses")
    public void setAddresses(List<AddressEntry> addresses) {
        this.addresses = addresses;
    }

}
