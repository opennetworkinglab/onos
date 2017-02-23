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
    var $loc, ps, ms, flash, sus, countryFilters;

    // Injected Classes
    var MapSelectionDialog;

    // internal state
    var instance, mapG, zoomLayer, zoomer;

    function init(_zoomLayer_, _zoomer_) {
        zoomLayer = _zoomLayer_;
        zoomer = _zoomer_;
        return setUpMap.bind(this)();
    }

    function setUpMap() {
        var prefs = currentMap(),
            mapId = prefs.mapid,
            mapFilePath = prefs.mapfilepath,
            mapScale = prefs.mapscale || 1,
            loadMap = ms.loadMapInto,
            promise, cfilter;

        mapG = d3.select('#topo-map');

        if (mapG.empty()) {
            mapG = zoomLayer.append('g').attr('id', 'topo-map');
        } else {
            mapG.each(function () {
                d3.selectAll(this.childNodes).remove();
            });
        }

        if (!ps.getPrefs('topo_prefs')[this.prefs.visible]) {
            this.hide();
        }

        if (mapFilePath === '*countries') {
            cfilter = countryFilters[mapId] || countryFilters.uk;
            loadMap = ms.loadMapRegionInto;
        }

        promise = loadMap(mapG, mapFilePath, mapId, {
            countryFilters: cfilter,
            adjustScale: mapScale,
            shading: ''
        });

        return promise;
    }

    function currentMap() {
        return ps.getPrefs(
            'topo_mapid',
            {
                mapid: 'usa',
                mapscale: 1,
                mapfilepath: '*continental_us',
                tint: 'off'
            },
            $loc.search()
        );
    }

    function setMap(map) {
        ps.setPrefs('topo_mapid', map);
        return setUpMap.bind(this)();
    }

    function openMapSelection() {

        MapSelectionDialog.prototype.currentMap = currentMap;

        new MapSelectionDialog({
            okHandler: function (preferences) {
                setMap(preferences);
            }
        }).open();
    }

    function resetZoom() {
        zoomer.reset();
    }

    angular.module('ovTopo2')
    .factory('Topo2MapService', [
        '$location', 'Topo2ViewController', 'PrefsService', 'MapService', 'FlashService',
        'SvgUtilService', 'Topo2CountryFilters', 'Topo2MapDialog',

        function (_$loc_, ViewController, _ps_, _ms_, _flash_, _sus_, _t2cf_, _t2md_) {

            $loc = _$loc_;
            ps = _ps_;
            ms = _ms_;
            flash = _flash_;
            sus = _sus_;
            countryFilters = _t2cf_;
            MapSelectionDialog = _t2md_;

            var MapLayer = ViewController.extend({

                id: 'topo-map',
                displayName: 'Map',

                init: init,
                setMap: setMap,
                openMapSelection: openMapSelection,
                resetZoom: resetZoom
            });

            return instance || new MapLayer();
        }
    ]);
})();
