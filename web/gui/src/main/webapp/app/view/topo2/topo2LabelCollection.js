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

/*
 ONOS GUI -- Topo2LabelCollection
 A collection of any type of label (Topo2Label, Topo2Badge, Topo2LinkLabel)
 */

(function () {

    var instance;

    angular.module('ovTopo2')
        .factory('Topo2LabelCollection', [
            'Topo2Collection',
            function (Collection) {

                var LabelCollection = Collection.extend({
                    initialize: function () {
                        instance = this;
                    },
                    addLabel: function (Model, label, targetNode, options) {
                        if (this._byId[label.id]) {
                            this.get(label.id).set(label);
                        } else {
                            var lab = new Model(label, targetNode, options);
                            this.add(lab);
                        }
                    },
                });

                return instance || new LabelCollection();
            },
        ]);
})();
