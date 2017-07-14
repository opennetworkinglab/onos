/*
 * Copyright 2015-present Open Networking Foundation
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
 ONOS GUI -- SVG -- Glyph Service
 */
(function () {
    'use strict';

    // injected references
    var $log, fs, sus, gd;

    // internal state
    var glyphs = d3.map(),
        api;

    // constants
    var msgGS = 'GlyphService.',
        rg = 'registerGlyphs(): ',
        rgs = 'registerGlyphSet(): ';

    // ----------------------------------------------------------------------

    function warn(msg) {
        $log.warn(msgGS + msg);
    }

    function addToMap(key, value, vbox, overwrite, dups) {
        if (!overwrite && glyphs.get(key)) {
            dups.push(key);
        } else {
            glyphs.set(key, { id: key, vb: vbox, d: value });
        }
    }

    function reportDups(dups, which) {
        var ok = (dups.length == 0),
            msg = 'ID collision: ';

        if (!ok) {
            dups.forEach(function (id) {
                warn(which + msg + '"' + id + '"');
            });
        }
        return ok;
    }

    function reportMissVb(missing, which) {
        var ok = (missing.length == 0),
            msg = 'Missing viewbox property: ';

        if (!ok) {
            missing.forEach(function (vbk) {
                warn(which + msg + '"' + vbk + '"');
            });
        }
        return ok;
    }

    // ----------------------------------------------------------------------
    // === API functions ===

    function clear() {
        // start with a fresh map
        glyphs = d3.map();
    }

    function init() {
        gd.registerCoreGlyphs(api);
    }

    function registerGlyphs(data, overwrite) {
        var dups = [],
            missvb = [];

        angular.forEach(data, function (value, key) {
            var vbk = '_' + key,
                vb = data[vbk];

            if (key[0] !== '_') {
                if (!vb) {
                    missvb.push(vbk);
                    return;
                }
                addToMap(key, value, vb, overwrite, dups);
            }
        });
        return reportDups(dups, rg) && reportMissVb(missvb, rg);
    }

    function registerGlyphSet(data, overwrite) {
        var dups = [],
            vb = data._viewbox;

        if (!vb) {
            warn(rgs + 'no "_viewbox" property found');
            return false;
        }

        angular.forEach(data, function (value, key) {
            if (key[0] !== '_') {
                addToMap(key, value, vb, overwrite, dups);
            }
        });
        return reportDups(dups, rgs);
    }

    function ids() {
        return glyphs.keys();
    }

    function glyph(id) {
        return glyphs.get(id);
    }

    function glyphDefined(id) {
        return glyphs.has(id);
    }

    // Note: defs should be a D3 selection of a single <defs> element
    function loadDefs(defs, glyphIds, noClear) {
        var list = fs.isA(glyphIds) || ids(),
            clearCache = !noClear;

        if (clearCache) {
            // remove all existing content
            defs.html(null);
        }

        // load up the requested glyphs
        list.forEach(function (id) {
            var g = glyph(id);
            if (g) {
                if (noClear) {
                    // quick exit if symbol is already present
                    if (defs.select('symbol#' + g.id).size() > 0) {
                        return;
                    }
                }
                defs.append('symbol')
                    .attr({ id: g.id, viewBox: g.vb })
                    .append('path').attr('d', g.d);
            }
        });
    }

    // trans can specify translation [x,y]
    function addGlyph(elem, glyphId, size, overlay, trans) {
        var sz = size || 40,
            ovr = !!overlay,
            xns = fs.isA(trans),
            atr = {
                width: sz,
                height: sz,
                'class': 'glyph',
                'xlink:href': '#' + glyphId,
            };

        if (xns) {
            atr.transform = sus.translate(trans);
        }
        return elem.append('use').attr(atr).classed('overlay', ovr);
    }

    // ----------------------------------------------------------------------

    angular.module('onosSvg')
    .factory('GlyphService',
        ['$log', 'FnService', 'SvgUtilService', 'GlyphDataService',

        function (_$log_, _fs_, _sus_, _gd_) {
            $log = _$log_;
            fs = _fs_;
            sus = _sus_;
            gd = _gd_;

            api = {
                clear: clear,
                init: init,
                registerGlyphs: registerGlyphs,
                registerGlyphSet: registerGlyphSet,
                ids: ids,
                glyph: glyph,
                glyphDefined: glyphDefined,
                loadDefs: loadDefs,
                addGlyph: addGlyph,
            };
            return api;
        }]
    )
    .run(['$log', function ($log) {
        $log.debug('Clearing glyph cache');
        clear();
    }]);

}());
