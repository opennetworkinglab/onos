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
 Defines behavior for loading sprites into the sprite layer.
 */

(function () {
    'use strict';

    // injected refs
    var $log, $http, fs, gs, sus, wss;

    var tssid = 'TopoSpriteService: ';

    // internal state
    var spriteLayer, defsElement;

    function registerPathsAsGlyphs(paths) {
        var custom = {},
            ids = [];

        function mkd(d) {
            return fs.isA(d) ? d.join('') : d;
        }

        if (paths) {
            paths.forEach(function (path) {
                var tag = 'spr_' + path.tag;
                custom['_' + tag] = path.viewbox || '0 0 1000 1000';
                custom[tag] = mkd(path.d);
                ids.push(tag);
            });

            gs.registerGlyphs(custom);
            gs.loadDefs(defsElement, ids, true);
        }
    }

    function labAttr(def) {
        var dim = def.dim || [1000,1000],
            w = dim[0],
            h = dim[1],
            dy = def.labelyoff || 1;

        return { x: w / 2, y: h * dy };
    }

    function doSprite(spr, def) {
        var c = spr.class || 'gray1',
            p = spr.pos || [0,0],
            lab = spr.label,
            dim = def.dim || [1000,1000],
            w = dim[0],
            h = dim[1],
            use = def.glyph || 'spr_' + def.path,
            g = spriteLayer.append('g')
                .classed(c, true)
                .attr('transform', sus.translate(p));

        g.append('use').attr({
            width: w,
            height: h,
            'xlink:href': '#' + use
        });

        if (lab) {
            g.append('text')
                .text(lab)
                .attr(labAttr(def));
        }
    }

    function doLabel(label) {
        var c = label.class || 'gray1',
            p = label.pos || [0,0];
        spriteLayer.append('text')
            .text(label.text)
            .attr('transform', sus.translate(p))
            .classed(c, true);
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
            name, desc, sprites, labels,
            paths = {},
            defs = {};

        if (!data) {
            $log.warn(tssid + 'No sprite data loaded.')
            return;
        }
        name = data.defn_name;
        desc = data.defn_desc;

        $log.debug("Loading sprites...[" + name + "]", desc);

        registerPathsAsGlyphs(data.paths);

        if (data.defn) {
            data.defn.forEach(function (d) {
                defs[d.id] = d;
            });
        }

        // pull out the sprite and label items
        if (data.load) {
            sprites = data.load.sprites;
            labels = data.load.labels;
        }

        if (sprites) {
            sprites.forEach(function (spr) {
               doSprite(spr, defs[spr.id]);
            });
        }

        if (labels) {
            labels.forEach(doLabel);
        }
    }


    function loadSprites(layer, defsElem, defname) {
        var name = defname || 'sprites';
        spriteLayer = layer;
        defsElement = defsElem;

        $log.info(tssid + 'Requesting sprite definition ['+name+']...');

        wss.sendEvent('spriteListRequest');
        wss.sendEvent('spriteDataRequest', {name: name});
    }

    // === -----------------------------------------------------
    // === MODULE DEFINITION ===

    angular.module('ovTopo')
    .factory('TopoSpriteService',
        ['$log', '$http', 'FnService', 'GlyphService',
            'SvgUtilService', 'WebSocketService',

        function (_$log_, _$http_, _fs_, _gs_, _sus_, _wss_) {
            $log = _$log_;
            $http = _$http_;
            fs = _fs_;
            gs = _gs_;
            sus = _sus_;
            wss = _wss_;

            return {
                loadSprites: loadSprites,
                spriteListResponse: inList,
                spriteDataResponse: inData
            };
        }]);

}());
