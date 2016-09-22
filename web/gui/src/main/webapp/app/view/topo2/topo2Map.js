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

    var $log, $loc, wss, ps, ms, t2ds, sus;

    // internal state
    var mapG, order, maps, map, mapItems, tintCheck, messageHandlers;

    // constants
    var mapRequest = 'mapSelectorRequest';

    var countryFilters = {
        s_america: function (c) {
            return c.properties.continent === 'South America';
        },

        ns_america: function (c) {
            return c.properties.custom === 'US-cont' ||
                c.properties.subregion === 'Central America' ||
                c.properties.continent === 'South America';
        },

        japan: function (c) {
            return c.properties.geounit === 'Japan';
        },

        europe: function (c) {
            return c.properties.continent === 'Europe';
        },

        italy: function (c) {
            return c.properties.geounit === 'Italy';
        },

        uk: function (c) {
            // technically, Ireland is not part of the United Kingdom,
            // but the map looks weird without it showing.
            return c.properties.adm0_a3 === 'GBR' ||
                c.properties.adm0_a3 === 'IRL';
        },

        s_korea: function (c) {
            return c.properties.adm0_a3 === 'KOR';
        },

        australia: function (c) {
            return c.properties.adm0_a3 === 'AUS';
        }
    };

    function init(zoomLayer) {

        start();
        return setUpMap(zoomLayer);
    }

    function setUpMap(zoomLayer) {
        var prefs = currentMap(),
            mapId = prefs.mapid,
            mapFilePath = prefs.mapfilepath,
            mapScale = prefs.mapscale,
            tint = prefs.tint,
            promise, cfilter;

        mapG = d3.select('#topo-map');

        if (mapG.empty()) {
            mapG = zoomLayer.append('g').attr('id', 'topo-map');
        } else {
            mapG.each(function (d, i) {
                d3.selectAll(this.childNodes).remove();
            });
        }

        cfilter = countryFilters[mapId] || countryFilters.uk;

        if (mapFilePath === '*countries') {

            cfilter = countryFilters[mapId] || countryFilters.uk;

            promise = ms.loadMapRegionInto(mapG, {
                countryFilter: cfilter,
                adjustScale: mapScale,
                shading: ''
            });
        } else {

            promise = ms.loadMapInto(mapG, mapFilePath, mapId, {
                adjustScale: mapScale,
                shading: ''
            });
        }

        return promise;
    }

    function start() {
        wss.bindHandlers(messageHandlers);
    }

    function stop() {
        wss.unbindHandlers(messageHandlers);
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

    function openMapSelection() {
        wss.sendEvent(mapRequest);
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

    function dOk() {
        var p = {
            mapid: map.id,
            mapscale: map.scale,
            mapfilepath: map.filePath,
            tint: 'off'
            // tint: tintCheck.property('checked') ? 'on' : 'off'
        };
        setMap(p);
        $log.debug('Dialog OK button clicked');
    }

    function dClose() {
        $log.debug('Dialog Close button clicked (or Esc pressed)');
    }

    function selectMap() {
        map = maps[this.options[this.selectedIndex].value];
        $log.info('Selected map', map);
    }

    function createListContent() {
        var content = t2ds.createDiv('map-list'),
            form = content.append('form'),
            current = currentMap();
        map = maps[current.mapid];
        mapItems = form.append('select').on('change', selectMap);
        order.forEach(function (id) {
            var m = maps[id];
            mapItems.append('option')
                .attr('value', m.id)
                .attr('selected', m.id === current.mapid ? true : null)
                .text(m.description);
        });

        return content;
    }

    function handleMapResponse(data) {
        $log.info('Got response', data);
        order = data.order;
        maps = data.maps;
        t2ds.openDialog()
            .setTitle('Select Map')
            .addContent(createListContent())
            .addOk(dOk, 'OK')
            .addCancel(dClose, 'Close')
            .bindKeys();
    }

    // TODO: -- START -- Move to dedicated module
    var prefsState = {};

    function updatePrefsState(what, b) {
        prefsState[what] = b ? 1 : 0;
        ps.setPrefs('topo_prefs', prefsState);
    }

    function _togSvgLayer(x, G, tag, what) {
        var on = (x === 'keyev') ? !sus.visible(G) : !!x,
            verb = on ? 'Show' : 'Hide';
        sus.visible(G, on);
        updatePrefsState(tag, on);
        // flash.flash(verb + ' ' + what);
    }
    // TODO: -- END -- Move to dedicated module

    function toggle(x) {
        _togSvgLayer(x, mapG, 'bg', 'background map');
    }

    angular.module('ovTopo2')
    .factory('Topo2MapService',
        ['$log', '$location', 'WebSocketService', 'PrefsService', 'MapService',
            'SvgUtilService', 'Topo2DialogService',
            function (_$log_, _$loc_, _wss_, _ps_, _ms_, _sus_, _t2ds_) {

                $log = _$log_;
                $loc = _$loc_;
                wss = _wss_;
                ps = _ps_;
                ms = _ms_;
                sus = _sus_;
                t2ds = _t2ds_;

                messageHandlers = {
                    mapSelectorResponse: handleMapResponse
                };

                return {
                    init: init,
                    openMapSelection: openMapSelection,
                    toggle: toggle,
                    stop: stop
                };
            }
        ]);

})();
