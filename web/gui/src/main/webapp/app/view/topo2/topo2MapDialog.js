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
  ONOS GUI -- Topology Map Dialog
  Display of the dialog window to select a background map for the current topology view
  NOTE: This will be deprecated in the future
  */

(function () {
    'use strict';

    // Injected
    var $log, wss, t2ds;

    // Constants
    var mapRequest = 'mapSelectorRequest';

    // State
    var order, maps, map, mapItems;

    var Dialog = function (options) {

        this.okHandlerCallback = options.okHandler;
        this.closeHandlerCallback = options.closeHandler;

        wss.bindHandlers({
            mapSelectorResponse: this.handleMapResponse.bind(this)
        });
    };

    Dialog.prototype = {
        handleMapResponse: function (data) {
            $log.info('Got response', data);
            order = data.order;
            maps = data.maps;

            t2ds.openDialog()
                .setTitle('Select Map')
                .addContent(this.render.bind(this)())
                .addOk(this.okHandler.bind(this), 'OK')
                .addCancel(this.closeHandler.bind(this), 'Close')
                .bindKeys();
        },

        open: function () {
            wss.sendEvent(mapRequest);
        },
        close: function () {
            wss.unbindHandlers({
                mapSelectorResponse: this.handleMapResponse.bind(this)
            });
        },

        selectedMap: function () {
            map = maps[this.options[this.selectedIndex].value];
            $log.info('Selected map', map);
        },

        okHandler: function () {

            var p = {
                mapid: map.id,
                mapscale: map.scale,
                mapfilepath: map.filePath,
                tint: 'off'
                // tint: tintCheck.property('checked') ? 'on' : 'off'
            };

            if (this.okHandlerCallback) {
                this.okHandlerCallback(p);
            }

            this.close();
        },
        closeHandler: function () {

            if (this.closeHandlerCallback) {
                this.closeHandlerCallback();
            }

            this.close();
        },

        render: function () {

            var content = t2ds.createDiv('map-list'),
                form = content.append('form'),
                current = this.currentMap();

            map = maps[current.mapid];
            mapItems = form.append('select').on('change', this.selectedMap);

            order.forEach(function (id) {
                var m = maps[id];
                mapItems.append('option')
                    .attr('value', m.id)
                    .attr('selected', m.id === current.mapid ? true : null)
                    .text(m.description);
            });

            return content;
        }
    };

    angular.module('ovTopo2')
    .factory('Topo2MapDialog', [
        '$log', 'WebSocketService', 'Topo2DialogService',
        function (_$log_, _wss_, _t2ds_) {

            $log = _$log_;
            wss = _wss_;
            t2ds = _t2ds_;

            return Dialog;
        }
    ]);
})();
