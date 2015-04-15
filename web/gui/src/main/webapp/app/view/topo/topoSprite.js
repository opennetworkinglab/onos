/*
 * Copyright 2015 Open Networking Laboratory
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
 ONOS GUI -- Topology Sprite Module.
 Defines behavior for loading sprites.
 */

(function () {
    'use strict';

    // injected refs
    var $log, $http, fs, sus, wss;

    var tssid = 'TopoSpriteService: ';

    // internal state
    var spriteLayer;

    function doSprite(def, item) {
        var g;

        function xfm(x, y, s) {
            return sus.translate([x,y]) + sus.scale(s, s);
        }

        g = spriteLayer.append('g')
            .classed(def['class'], true)
            .attr('transform', xfm(item.x, item.y, def.scale));

        if (item.label) {
            g.append('text')
                .text(item.label)
                .attr({
                    x: def.width / 2,
                    y: def.height * def.textyoff
                });
        }

        g.append('use').attr({
            width: def.width,
            height: def.height,
            'xlink:href': '#' + def.use
        });
    }

    // ==========================
    // event handlers

    // Handles response from 'spriteListRequest' which lists all the
    // registered sprite definitions on the server.
    // (see onos-upload-sprites)
    function inList(payload) {
        $log.debug(tssid + 'Registered sprite definitions:', payload.names);
        // Some day, we will make this list available to the user in
        //  a dropdown selection box...
    }

    // Handles response from 'spriteDataRequest' which provides the
    //  data for the requested sprite definition.
    function inData(payload) {
        var data = payload.data,
            name = data && data.defn_name,
            desc = data && data.defn_desc,
            defs = {};

        if (!data) {
            $log.warn(tssid + 'No sprite data loaded.')
            return;
        }

        $log.debug("Loading sprites...[" + name + "]", desc);

        data.defn.forEach(function (d) {
            defs[d.id] = d;
        });

        data.load.forEach(function (item) {
            doSprite(defs[item.id], item);
        });
    }


    function loadSprites(layer, defname) {
        var name = defname || 'sprites';
        spriteLayer = layer;

        $log.info(tssid + 'Requesting sprite definition ['+name+']...');

        wss.sendEvent('spriteListRequest');
        wss.sendEvent('spriteDataRequest', {name: name});
    }

    // === -----------------------------------------------------
    // === MODULE DEFINITION ===

    angular.module('ovTopo')
    .factory('TopoSpriteService',
        ['$log', '$http', 'FnService', 'SvgUtilService', 'WebSocketService',

        function (_$log_, _$http_, _fs_, _sus_, _wss_) {
            $log = _$log_;
            $http = _$http_;
            fs = _fs_;
            sus = _sus_;
            wss = _wss_;

            return {
                loadSprites: loadSprites,
                spriteListResponse: inList,
                spriteDataResponse: inData
            };
        }]);

}());
