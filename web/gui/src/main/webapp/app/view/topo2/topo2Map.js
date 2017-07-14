/*
 * Copyright 2016-present Open Networking Foundation
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
 ONOS GUI -- Topology Map Module.
 Defines behavior for loading geographical maps into the map layer.
 */

(function () {
    'use strict';

    // Injected Services
    var t2zs, countryFilters, ms;

    // internal state
    var instance, zoomer, currentMap;

    function init() {
        this.appendElement('#topo2-background', 'g');
        zoomer = t2zs.getZoomer();
        currentMap = null;
    }

    function setUpMap(mapId, mapFilePath, mapScale) {

        if (currentMap === mapId) {
            return new Promise(function (resolve) {
                resolve();
            });
        }

        currentMap = mapId;

        var loadMap = ms.loadMapInto,
            promise, cfilter;

        this.node().selectAll('*').remove();

        if (mapFilePath === '*countries') {
            cfilter = countryFilters[mapId] || countryFilters.uk;
            loadMap = ms.loadMapRegionInto;
        }

        promise = loadMap(this.node(), mapFilePath, mapId, {
            countryFilters: cfilter,
            adjustScale: mapScale || 1,
            shading: '',
        });

        return promise;
    }


    function resetZoom() {
        zoomer.reset();
    }

    function zoomCallback(sc, tr) {
        // keep the map lines constant width while zooming
        this.node().style('stroke-width', (2.0 / sc) + 'px');
    }

    function getCurrentMap() {
        return currentMap;
    }

    angular.module('ovTopo2')
    .factory('Topo2MapService', [
        'Topo2ZoomService', 'MapService', 'Topo2ViewController',

        function (_t2zs_, _ms_, ViewController) {
            t2zs = _t2zs_;
            ms = _ms_;

            var MapLayer = ViewController.extend({

                id: 'topo2-map',
                displayName: 'Map',

                init: init,
                setUpMap: setUpMap,
                resetZoom: resetZoom,
                zoomCallback: zoomCallback,
                getCurrentMap: getCurrentMap,
            });

            return instance || new MapLayer();
        },
    ]);
})();
