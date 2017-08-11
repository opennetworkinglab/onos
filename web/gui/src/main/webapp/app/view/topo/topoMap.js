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

    // injected refs
    var $log, wss, tds, delegate;

    // constants
    var mapRequest = 'mapSelectorRequest';

    // internal state
    var order, maps, map, mapItems, msgHandlers;

    // function to be replaced by the localization bundle function
    var topoLion = function (x) {
        return '#tmap#' + x + '#';
    };

    // === ---------------------------
    // === Main API functions

    function openMapSelection() {
        wss.sendEvent(mapRequest);
    }

    function closeMapSelection() {
        tds.closeDialog();
    }

    function start(d) {
        delegate = d;
        wss.bindHandlers(msgHandlers);
    }

    function stop() {
        wss.unbindHandlers(msgHandlers);
    }

    function dOk() {
        var p = {
            mapid: map.id,
            mapscale: map.scale,
            mapfilepath: map.filePath,
            tint: 'off',
            // tint: tintCheck.property('checked') ? 'on' : 'off'
        };
        setMap(p);
        // $log.debug('Dialog OK button clicked');
    }

    function dClose() {
        // $log.debug('Dialog Close button clicked (or Esc pressed)');
    }

    function selectMap() {
        map = maps[this.options[this.selectedIndex].value];
        $log.info('Selected map', map);
    }

    function createListContent() {
        var content = tds.createDiv('map-list'),
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
        tds.openDialog()
            .setTitle(topoLion('title_select_map'))
            .addContent(createListContent())
            .addOk(dOk, topoLion('ok'))
            .addCancel(dClose, topoLion('close'))
            .bindKeys();
    }

    function toggleMap() {
        delegate.toggleMap();
    }

    function currentMap() {
        return delegate.currentMap();
    }

    function setMap(map) {
        delegate.setMap(map);
    }

    // invoked after the localization bundle has been received from the server
    function setLionBundle(bundle) {
        topoLion = bundle;
    }
    // === -----------------------------------------------------
    // === MODULE DEFINITION ===

    angular.module('ovTopo')
    .factory('TopoMapService',
        ['$log', 'WebSocketService', 'TopoDialogService',

        function (_$log_, _wss_, _tds_) {
            $log = _$log_;
            wss = _wss_;
            tds = _tds_;

            msgHandlers = {
                mapSelectorResponse: handleMapResponse,
            };

            return {
                toggleMap: toggleMap,
                currentMap: currentMap,
                setMap: setMap,

                openMapSelection: openMapSelection,
                closeMapSelection: closeMapSelection,
                start: start,
                stop: stop,

                setLionBundle: setLionBundle,
            };
        }]);

}());
