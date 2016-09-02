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

    // Injected Services
    var $log, t2sr, t2ds, t2hs, t2ls;
    var Model;

    // Internal
    var region;

    function init() {}

    function addRegion(data) {

        var RegionModel = Model.extend({
            findNodeById: findNodeById
        });

        region = new RegionModel({
            id: data.id,
            layerOrder: data.layerOrder
        });

        region.set({
            subregions: t2sr.createSubRegionCollection(data.subregions, region),
            devices: t2ds.createDeviceCollection(data.devices, region),
            hosts: t2hs.createHostCollection(data.hosts, region),
            links: t2ls.createLinkCollection(data.links, region)
        });

        angular.forEach(region.get('links').models, function (link) {
            link.createLink();
        });

        $log.debug('Region: ', region);
    }

    function findNodeById(id) {

        // Remove /{port} from id if needed
        var regex = new RegExp('^[^/]*');
        id = regex.exec(id)[0];

        return region.get('devices').get(id) ||
            region.get('hosts').get(id) ||
            region.get('subregions').get(id);
    }

    function regionNodes() {

        if (region) {
            return [].concat(
                region.get('devices').models,
                region.get('hosts').models,
                region.get('subregions').models
            );
        }

        return [];
    }

    function regionLinks() {
        return (region) ? region.get('links').models : [];
    }

    angular.module('ovTopo2')
    .factory('Topo2RegionService',
        ['$log', 'Topo2Model',
        'Topo2SubRegionService', 'Topo2DeviceService',
        'Topo2HostService', 'Topo2LinkService',

        function (_$log_, _Model_, _t2sr_, _t2ds_, _t2hs_, _t2ls_) {

            $log = _$log_;
            Model = _Model_;
            t2sr = _t2sr_;
            t2ds = _t2ds_;
            t2hs = _t2hs_;
            t2ls = _t2ls_;

            return {
                init: init,

                addRegion: addRegion,
                regionNodes: regionNodes,
                regionLinks: regionLinks,

                getSubRegions: t2sr.getSubRegions
            };
        }]);

})();
