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
package org.onosproject.driver.flowspec.extensions;

import org.onlab.util.KryoNamespace;
import org.onosproject.flowapi.ExtFlowContainer;
import org.onosproject.net.flow.AbstractExtension;
import org.onosproject.net.flow.criteria.ExtensionSelector;
import org.onosproject.net.flow.criteria.ExtensionSelectorType;

import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;

 /**
  * Implementation of extension selector for multi value.
  */
 public final class ExtMatchExtension extends AbstractExtension implements ExtensionSelector  {
     private ExtFlowContainer container;
     private final KryoNamespace appKryo = new KryoNamespace.Builder().register(ExtMatchExtension.class).build();

     /**
      * Creates an object of ExtMatchExtension.
      */
     public ExtMatchExtension() {
         this.container = null;
     }
     /**
      * Returns the container.
      *
      * @return the container to match
      */
     public ExtFlowContainer container() {
         return container;
     }

     @Override
     public ExtensionSelectorType type() {
         return ExtensionSelectorType.ExtensionSelectorTypes.EXT_MATCH_FLOW_TYPE.type();
     }

     @Override
     public byte[] serialize() {
         return appKryo.serialize(container);
     }

     @Override
     public void deserialize(byte[] data) {
         container = ExtFlowContainer.of(appKryo.deserialize(data));
     }

     @Override
     public String toString() {
         return toStringHelper(type().toString())
                 .add("container", container)
                 .toString();
     }

     @Override
     public int hashCode() {
         return Objects.hash(type(), container);
     }

     @Override
     public boolean equals(Object obj) {
         if (this == obj) {
             return true;
         }
         if (obj instanceof ExtMatchExtension) {
             ExtMatchExtension that = (ExtMatchExtension) obj;
             return Objects.equals(container, that.container) &&
                     Objects.equals(this.type(), that.type());
         }
         return false;
     }
 }
