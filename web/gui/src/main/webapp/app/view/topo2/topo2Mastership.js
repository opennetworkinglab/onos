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
 ONOS GUI -- Topology Mastership controller.
 Controller that handles the state of the selected mastership
 */

(function () {

    var instance;

    angular.module('ovTopo2')
        .factory('Topo2MastershipService', [

            function () {

                var MastershipController = function () {
                    instance = this;
                    this.region = null;
                    this.currentMastership = null;
                };

                MastershipController.prototype = {
                    displayMastership: function () {
                        var nodes = this.region.regionNodes(),
                            links = this.region.regionLinks();

                        _.each(nodes.concat(links), function (n) {
                            n.displayMastership(this.currentMastership);
                        });
                    },
                    mastership: function () {
                        return this.currentMastership;
                    },
                    setMastership: function (id) {
                        this.currentMastership = id;
                        this.displayMastership();
                    },
                };

                return instance || new MastershipController();
            },
        ]);
})();
