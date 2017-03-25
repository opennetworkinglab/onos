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
 ONOS GUI -- Topology Map Module.
 Defines behavior for loading geographical maps into the map layer.
 */

(function () {
    'use strict';

    // Injected Services
    var $log, $loc, ps, ms, flash, sus, t2zs, countryFilters;

    // Injected Classes
    var MapSelectionDialog;

    // internal state
    var instance, mapG, zoomLayer, zoomer;

    function init() {
        this.appendElement('#topo2-background', 'g');
        zoomLayer = d3.select('#topo2-zoomlayer');
        zoomer = t2zs.getZoomer();
    }

    // TODO: to be re-worked: map-id, filePath, scale/pan to be passed as params
    function setUpMap(mapId, mapFilePath, mapScale) {

        var loadMap = ms.loadMapInto,
            promise, cfilter;

        this.node().selectAll("*").remove();

        if (mapFilePath === '*countries') {
            cfilter = countryFilters[mapId] || countryFilters.uk;
            loadMap = ms.loadMapRegionInto;
        }

        promise = loadMap(this.node(), mapFilePath, mapId, {
            countryFilters: cfilter,
            adjustScale: mapScale || 1,
            shading: ''
        });

        return promise;
    }

    // TODO: deprecated - the layout will tell us which map
    //   no longer stored in user preferences
    function currentMap() {
        return ps.getPrefs(
            'topo2_mapid',
            {
                mapid: 'usa',
                mapscale: 1,
                mapfilepath: '*continental_us',
                tint: 'off'
            },
            $loc.search()
        );
    }

    // TODO: deprecated - maps are defined per layout on the server side.
    function setMap(map) {
        ps.setPrefs('topo2_mapid', map);
        return setUpMap.bind(this)();
    }

    // TODO: deprecated - map selection does not make sense in Topo2
    function openMapSelection() {
        $log.warn('openMapSelection DISABLED');

        // MapSelectionDialog.prototype.currentMap = currentMap;
        //
        // new MapSelectionDialog({
        //     okHandler: function (preferences) {
        //         setMap(preferences);
        //     }
        // }).open();
    }

    function resetZoom() {
        zoomer.reset();
    }

    angular.module('ovTopo2')
    .factory('Topo2MapService', [
        '$log', '$location', 'Topo2ViewController', 'PrefsService',
        'MapService', 'FlashService', 'SvgUtilService', 'Topo2CountryFilters',
        'Topo2MapDialog', 'Topo2ZoomService',

        function (_$log_, _$loc_, ViewController, _ps_,
                  _ms_, _flash_, _sus_, _t2cf_,
                  _t2md_, _t2zs_) {

            $log = _$log_;
            $loc = _$loc_;
            ps = _ps_;
            ms = _ms_;
            flash = _flash_;
            sus = _sus_;
            countryFilters = _t2cf_;
            MapSelectionDialog = _t2md_;
            t2zs = _t2zs_;

            var MapLayer = ViewController.extend({

                id: 'topo2-map',
                displayName: 'Map',

                init: init,
                setMap: setMap,
                setUpMap: setUpMap,
                openMapSelection: openMapSelection,
                resetZoom: resetZoom
            });

            return instance || new MapLayer();
        }
    ]);
})();
