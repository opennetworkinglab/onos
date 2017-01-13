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
package org.onosproject.tetopology.management.impl;

import org.onlab.packet.Ip4Address;
import org.onosproject.core.ApplicationId;
import org.onosproject.incubator.net.config.basics.ConfigException;
import org.onosproject.net.config.Config;

/**
 * Configuration for TE Topology parameters.
 */
public class TeTopologyConfig extends Config<ApplicationId>  {
    private static final String CONFIG_VALUE_ERROR = "Error parsing config value";
    private static final String PROVIDER_ID = "provider-id";
    private static final String MDSC = "mdsc";
    private static final String TENODE_ID_START = "tenode-id-start";
    private static final String TENODE_ID_END = "tenode-id-end";

    /**
     * Retrieves TE topology provider identifier.
     *
     * @return provider Id
     * @throws ConfigException if the parameters are not correctly configured
     * or conversion of the parameters fails
     */
    public long providerId() throws ConfigException {
        try {
            return object.path(PROVIDER_ID).asLong();
        } catch (IllegalArgumentException e) {
            throw new ConfigException(CONFIG_VALUE_ERROR, e);
        }
    }

   /**
    * Retrieves TE node starting IPv4 address.
    *
    * @return the IPv4 address
    * @throws ConfigException if the parameters are not correctly configured
    * or conversion of the parameters fails
    */
   public Ip4Address teNodeIpStart() throws ConfigException {
       try {
           return Ip4Address.valueOf(object.path(TENODE_ID_START).asText());
       } catch (IllegalArgumentException e) {
           throw new ConfigException(CONFIG_VALUE_ERROR, e);
       }
   }

  /**
   * Retrieves TE node end IPv4 address.
   *
   * @return the IPv4 address
   * @throws ConfigException if the parameters are not correctly configured
   * or conversion of the parameters fails
   */
  public Ip4Address teNodeIpEnd() throws ConfigException {
      try {
          return Ip4Address.valueOf(object.path(TENODE_ID_END).asText());
      } catch (IllegalArgumentException e) {
          throw new ConfigException(CONFIG_VALUE_ERROR, e);
      }
  }

  /**
   * Retrieves if this is a MDSC(Multi-Domain Super Controller).
   *
   * @return MDSC value
   * @throws ConfigException if the parameters are not correctly configured or
   *             conversion of the parameters fails
   */
  public String mdsc() throws ConfigException {
      try {
          return object.path(MDSC).asText();
      } catch (IllegalArgumentException e) {
          throw new ConfigException(CONFIG_VALUE_ERROR, e);
      }
  }

}
