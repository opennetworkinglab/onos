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
    var $log, t2sr, t2ds, t2hs, t2ls, t2zs, t2dps;
    var Model;

    // Internal
    var region

    function init() {}

    function addRegion(data) {

        var RegionModel = Model.extend({
            findNodeById: findNodeById,
            nodes: regionNodes
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


        // TODO: replace with an algorithm that computes appropriate transition
        //        based on the location of the "region node" on the parent map

        // TEMP Map Zoom
        var regionPanZooms = {
            "(root)": {
                scale: 4.21,
                translate: [-2066.3049871603093, -2130.190726668792]
            },
            c01: {
                scale: 19.8855,
                translate: [-10375.91165337411, -10862.217941271818]
            },
            c02: {
                scale: 24.25,
                translate: [-14169.70851936781, -15649.174761455488]
            },
            c03: {
                scale: 22.72,
                translate: [-14950.92246589002, -15390.955326616648]
            },
            c04: {
                scale: 26.24,
                translate: [-16664.006814209282, -16217.021478816077]
            }
        };


        setTimeout(function () {
            var regionPZ = regionPanZooms[region.get('id')];
            t2zs.panAndZoom(regionPZ.translate, regionPZ.scale);
        }, 10);

        $log.debug('Region: ', region);
    }

    function findNodeById(link, id) {


        if (link.get('type') !== 'UiEdgeLink') {
            // Remove /{port} from id if needed
            var regex = new RegExp('^[^/]*');
            id = regex.exec(id)[0];
        }

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

    function filterRegionNodes(predicate) {
        var nodes = regionNodes();
        return _.filter(nodes, predicate);
    }

    function regionLinks() {
        return (region) ? region.get('links').models : [];
    }

    function deselectAllNodes() {

        var selected = filterRegionNodes(function (node) {
            return node.get('selected', true);
        });

        if (selected.length) {

            selected.forEach(function (node) {
                node.deselect();
            });

            t2dps().el.hide();
            return true;
        }

        // TODO: close details panel

        return false;
    }

    function deselectLink() {

        var selected = _.filter(regionLinks(), function (link) {
            return link.get('selected', true);
        });

        if (selected.length) {

            selected.forEach(function (link) {
                link.deselect();
            });

            t2dps().el.hide();
            return true;
        }

        return false;
    }

    angular.module('ovTopo2')
    .factory('Topo2RegionService',
        ['$log', 'Topo2Model',
        'Topo2SubRegionService', 'Topo2DeviceService',
        'Topo2HostService', 'Topo2LinkService', 'Topo2ZoomService', 'Topo2DetailsPanelService',

        function (_$log_, _Model_, _t2sr_, _t2ds_, _t2hs_, _t2ls_, _t2zs_, _t2dps_) {

            $log = _$log_;
            Model = _Model_;
            t2sr = _t2sr_;
            t2ds = _t2ds_;
            t2hs = _t2hs_;
            t2ls = _t2ls_;
            t2zs = _t2zs_;
            t2dps = _t2dps_;

            return {
                init: init,

                addRegion: addRegion,
                regionNodes: regionNodes,
                regionLinks: regionLinks,
                filterRegionNodes: filterRegionNodes,

                deselectAllNodes: deselectAllNodes,
                deselectLink: deselectLink,

                getSubRegions: t2sr.getSubRegions
            };
        }]);

})();
