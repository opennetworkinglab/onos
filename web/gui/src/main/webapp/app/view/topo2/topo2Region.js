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
 ONOS GUI -- Topology Region Module.
 Module that holds the current region in memory
 */

(function () {
    'use strict';

    var $log,
        wss,
        t2sr,
        t2ds,
        t2hs,
        t2ls;

    var regions;

    function init() {
        regions = {};
    }

    function addRegion(data) {

        var region = {
            subregions: t2sr.createSubRegionCollection(data.subregions),
            devices: t2ds.createDeviceCollection(data.devices, data),
            hosts: t2hs.createHostCollection(data.hosts),
            links: t2ls.createLinkCollection(data.links),
        };

        $log.debug('Region: ', region);
    }

    angular.module('ovTopo2')
    .factory('Topo2RegionService',
        ['$log', 'WebSocketService', 'Topo2SubRegionService', 'Topo2DeviceService',
        'Topo2HostService', 'Topo2LinkService',

        function (_$log_, _wss_, _t2sr_, _t2ds_, _t2hs_, _t2ls_) {

            $log = _$log_;
            wss = _wss_;
            t2sr = _t2sr_;
            t2ds = _t2ds_;
            t2hs = _t2hs_;
            t2ls = _t2ls_;

            return {
                init: init,

                addRegion: addRegion,
                getSubRegions: t2sr.getSubRegions
            };
        }]);

})();
