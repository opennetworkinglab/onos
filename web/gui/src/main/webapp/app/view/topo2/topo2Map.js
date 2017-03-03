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
    var $log, $loc, ps, ms, flash, sus, countryFilters;

    // Injected Classes
    var MapSelectionDialog;

    // internal state
    var instance, mapG, zoomLayer, zoomer;

    function init(_zoomLayer_, _zoomer_) {
        zoomLayer = _zoomLayer_;
        zoomer = _zoomer_;
        // This function no longer returns a promise.
        //  TODO: call setUpMap() when we know which map we want (not from here)
        // return setUpMap.bind(this)();
    }

    // TODO: to be re-worked: map-id, filePath, scale/pan to be passed as params
    function setUpMap() {
        var prefs = currentMap(),
            mapId = prefs.mapid,
            mapFilePath = prefs.mapfilepath,
            mapScale = prefs.mapscale || 1,
            loadMap = ms.loadMapInto,
            promise, cfilter;

        mapG = d3.select('#topo2-map');

        if (mapG.empty()) {
            mapG = zoomLayer.append('g').attr('id', 'topo2-map');
        } else {
            mapG.each(function () {
                d3.selectAll(this.childNodes).remove();
            });
        }

        if (!ps.getPrefs('topo2_prefs')[this.prefs.visible]) {
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
        'Topo2MapDialog',

        function (_$log_, _$loc_, ViewController, _ps_,
                  _ms_, _flash_, _sus_, _t2cf_,
                  _t2md_) {

            $log = _$log_;
            $loc = _$loc_;
            ps = _ps_;
            ms = _ms_;
            flash = _flash_;
            sus = _sus_;
            countryFilters = _t2cf_;
            MapSelectionDialog = _t2md_;

            var MapLayer = ViewController.extend({

                id: 'topo2-map',
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
