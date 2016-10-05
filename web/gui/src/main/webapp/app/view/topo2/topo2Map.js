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
    var $loc, ps, ms, sus, countryFilters;

    // Injected Classes
    var MapSelectionDialog;

    // internal state
    var mapG, zoomLayer, zoomer;

    function init(_zoomLayer_, _zoomer_) {
        zoomLayer = _zoomLayer_;
        zoomer = _zoomer_;
        return setUpMap();
    }

    function setUpMap() {
        var prefs = currentMap(),
            mapId = prefs.mapid,
            mapFilePath = prefs.mapfilepath,
            mapScale = prefs.mapscale,
            loadMap = ms.loadMapInto,
            promise, cfilter;

        mapG = d3.select('#topo-map');

        if (mapG.empty()) {
            mapG = zoomLayer.append('g').attr('id', 'topo-map');
        } else {
            mapG.each(function (d, i) {
                d3.selectAll(this.childNodes).remove();
            });
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

    function opacifyMap(b) {
        mapG.transition()
            .duration(1000)
            .attr('opacity', b ? 1 : 0);
    }

    function setMap(map) {
        ps.setPrefs('topo_mapid', map);
        setUpMap();
        opacifyMap(true);
    }

    // TODO: -- START -- Move to dedicated module
    var prefsState = {};

    function updatePrefsState(what, b) {
        prefsState[what] = b ? 1 : 0;
        ps.setPrefs('topo_prefs', prefsState);
    }

    function _togSvgLayer(x, G, tag, what) {
        var on = (x === 'keyev') ? !sus.visible(G) : Boolean(x);
        sus.visible(G, on);
        updatePrefsState(tag, on);
        // flash.flash(verb + ' ' + what);
    }
    // TODO: -- END -- Move to dedicated module

    function toggle(x) {
        _togSvgLayer(x, mapG, 'bg', 'background map');
    }

    function openMapSelection() {

        // TODO: Create a view class with extend method
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
    .factory('Topo2MapService',
        ['$location', 'PrefsService', 'MapService',
            'SvgUtilService', 'Topo2CountryFilters', 'Topo2MapDialog',
            function (_$loc_, _ps_, _ms_, _sus_, _t2cf_, _t2md_) {

                $loc = _$loc_;
                ps = _ps_;
                ms = _ms_;
                sus = _sus_;
                countryFilters = _t2cf_;
                MapSelectionDialog = _t2md_;

                return {
                    init: init,
                    openMapSelection: openMapSelection,
                    toggle: toggle,

                    resetZoom: resetZoom
                };
            }
        ]);

})();
