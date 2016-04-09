/*
 * Copyright 2015-present Open Networking Laboratory
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

    // constants
    var tssid = 'TopoSpriteService: ',
        fontsize = 20;  // default font size 20pt.

    // internal state
    var spriteLayer, defsElement;


    function registerPathsAsGlyphs(paths) {
        var custom = {},
            ids = [];

        function mkd(d) {
            return fs.isA(d) ? d.join('') : d;
        }

        paths.forEach(function (path) {
            var tag = 'spr_' + path.tag;

            if (path.glyph) {
                // assumption is that we are using a built-in glyph
                return;
            }

            custom['_' + tag] = path.viewbox || '0 0 1000 1000';
            custom[tag] = mkd(path.d);
            ids.push(tag);
        });

        gs.registerGlyphs(custom);
        gs.loadDefs(defsElement, ids, true);
    }

    function applyStrokeStyle(s, use) {
        var style;
        if (s) {
            style = {};
            angular.forEach(s, function (value, key) {
                style['stroke-' + key] = value;
            });
            use.style(style);
        }
    }

    function applyFillClass(f, use) {
        use.classed('fill-' + f, true);
    }

    function doSprite(spr, def, pathmeta) {
        var pmeta = pathmeta[def.path],
            c = spr.class || 'gray1',
            p = spr.pos || [0,0],
            lab = spr.label,
            dim = def.dim || [40,40],
            w = dim[0],
            h = dim[1],
            dy = def.labelyoff || 1,
            sc = def.scale,
            xfm = sus.translate(p),
            g, attr, use;

        if (sc) {
            xfm += sus.scale(sc, sc);
        }

        g = spriteLayer.append('g')
            .classed(c, true)
            .attr('transform', xfm);

        attr = {
            width: w,
            height: h,
            'xlink:href': '#' + pmeta.u
        };

        use = g.append('use').attr(attr);
        applyStrokeStyle(pmeta.s, use);
        applyFillClass(def.fill, use);


        // add subpaths if they have been defined
        if (fs.isA(def.subpaths)) {
            def.subpaths.forEach(function (v) {
                pmeta = pathmeta[v.path];
                attr = {
                    width: w,
                    height: h,
                    'xlink:href': '#' + pmeta.u,
                    transform: sus.translate(v.pos)
                };
                use = g.append('use').attr(attr);
                applyStrokeStyle(pmeta.s, use);
                applyFillClass(def.subpathfill, use);
            });
        }

        if (lab) {
            g.append('text')
                .text(lab)
                .attr({ x: w / 2, y: h * dy });
        }
    }

    function doLabel(label) {
        var c = label.class || 'gray1',
            p = label.pos || [0,0],
            sz = label.size || 1.0,
            g = spriteLayer.append('g')
                .classed(c, true)
                .attr('transform', sus.translate(p))
                .append('text')
                .text(label.text)
                .style('font-size', (fontsize * sz)+'pt');
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
            name, desc, pfx, sprites, labels, alpha,
            paths, defn, load,
            pathmeta = {},
            defs = {},
            warn = [];

        if (!data) {
            $log.warn(tssid + 'No sprite data loaded.');
            return;
        }
        name = data.defn_name;
        desc = data.defn_desc;
        paths = data.paths;
        defn = data.defn;
        load = data.load;
        pfx = tssid + '[' + name + ']: ';

        $log.debug("Loading sprites...[" + name + "]", desc);

        function no(what) {
            warn.push(pfx + 'No ' + what + ' property defined');
        }

        if (!paths) no('paths');
        if (!defn) no('defn');
        if (!load) no('load');

        if (warn.length) {
            $log.error(warn.join('\n'));
            return;
        }

        // any custom paths need to be added to the glyph DB, and imported
        registerPathsAsGlyphs(paths);

        paths.forEach(function (p) {
            pathmeta[p.tag] = {
                s: p.stroke,
                u: p.glyph || 'spr_' + p.tag
            };
        });

        defn.forEach(function (d) {
            defs[d.id] = d;
        });

        // sprites, labels and alpha are each optional components of the load
        sprites = load.sprites;
        labels = load.labels;
        alpha = load.alpha;

        if (alpha) {
            spriteLayer.style('opacity', alpha);
        }

        if (sprites) {
            sprites.forEach(function (spr) {
                var def = defs[spr.id];
                doSprite(spr, def, pathmeta);
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
