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

/*
 ONOS GUI -- Topology Hosts Module.
 Module that holds the hosts for a region
 */

(function () {
    'use strict';

    var Collection, Model, t2vs;

    function createHostCollection(data, region) {

        var HostCollection = Collection.extend({
            model: Model
        });

        var hosts = [];
        data.forEach(function (hostsLayer) {
            hostsLayer.forEach(function (host) {
                hosts.push(host);
            });
        });

        return new HostCollection(hosts);
    }

    angular.module('ovTopo2')
    .factory('Topo2HostService',
    [
        'Topo2Collection', 'Topo2NodeModel', 'Topo2ViewService',
        function (_Collection_, _NodeModel_, classnames, _t2vs_) {

            t2vs = _t2vs_;
            Collection = _Collection_;

            Model = _NodeModel_.extend({
                nodeType: 'host'
            });

            return {
                createHostCollection: createHostCollection
            };
        }
    ]);

})();
