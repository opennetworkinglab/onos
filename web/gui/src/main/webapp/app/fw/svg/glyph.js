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
 ONOS GUI -- SVG -- Glyph Service

 @author Simon Hunt
 */
(function () {
    'use strict';

    var $log,
        fs,
        glyphs = d3.map(),
        msgGS = 'GlyphService.';

    // ----------------------------------------------------------------------
    // Base set of Glyphs...

    var birdViewBox = '352 224 113 112',

        birdData = {
            bird: "M427.7,300.4 c-6.9,0.6-13.1,5-19.2,7.1c-18.1,6.2-33.9," +
            "9.1-56.5,4.7c24.6,17.2,36.6,13,63.7,0.1c-0.5,0.6-0.7,1.3-1.3," +
            "1.9c1.4-0.4,2.4-1.7,3.4-2.2c-0.4,0.7-0.9,1.5-1.4,1.9c2.2-0.6," +
            "3.7-2.3,5.9-3.9c-2.4,2.1-4.2,5-6,8c-1.5,2.5-3.1,4.8-5.1,6.9c-1," +
            "1-1.9,1.9-2.9,2.9c-1.4,1.3-2.9,2.5-5.1,2.9c1.7,0.1,3.6-0.3,6.5" +
            "-1.9c-1.6,2.4-7.1,6.2-9.9,7.2c10.5-2.6,19.2-15.9,25.7-18c18.3" +
            "-5.9,13.8-3.4,27-14.2c1.6-1.3,3-1,5.1-0.8c1.1,0.1,2.1,0.3,3.2," +
            "0.5c0.8,0.2,1.4,0.4,2.2,0.8l1.8,0.9c-1.9-4.5-2.3-4.1-5.9-6c-2.3" +
            "-1.3-3.3-3.8-6.2-4.9c-7.1-2.6-11.9,11.7-11.7-5c0.1-8,4.2-14.4," +
            "6.4-22c1.1-3.8,2.3-7.6,2.4-11.5c0.1-2.3,0-4.7-0.4-7c-2-11.2-8.4" +
            "-21.5-19.7-24.8c-1-0.3-1.1-0.3-0.9,0c9.6,17.1,7.2,38.3,3.1,54.2" +
            "C429.9,285.5,426.7,293.2,427.7,300.4z"
        },

        glyphViewBox = '0 0 110 110',

        glyphData = {
            unknown: "M35,40a5,5,0,0,1,5-5h30a5,5,0,0,1,5,5v30a5,5,0,0,1-5,5" +
            "h-30a5,5,0,0,1-5-5z",

            node: "M15,100a5,5,0,0,1-5-5v-65a5,5,0,0,1,5-5h80a5,5,0,0,1,5,5" +
            "v65a5,5,0,0,1-5,5zM14,22.5l11-11a10,3,0,0,1,10-2h40a10,3,0,0,1," +
            "10,2l11,11zM16,35a5,5,0,0,1,10,0a5,5,0,0,1-10,0z",

            switch: "M10,20a10,10,0,0,1,10-10h70a10,10,0,0,1,10,10v70a10,10," +
            "0,0,1-10,10h-70a10,10,0,0,1-10-10zM60,26l12,0,0-8,18,13-18,13,0" +
            "-8-12,0zM60,60l12,0,0-8,18,13-18,13,0-8-12,0zM50,40l-12,0,0-8" +
            "-18,13,18,13,0-8,12,0zM50,74l-12,0,0-8-18,13,18,13,0-8,12,0z",

            roadm: "M10,35l25-25h40l25,25v40l-25,25h-40l-25-25zM58,26l12,0,0" +
            "-8,18,13-18,13,0-8-12,0zM58,60l12,0,0-8,18,13-18,13,0-8-12,0z" +
            "M52,40l-12,0,0-8-18,13,18,13,0-8,12,0zM52,74l-12,0,0-8-18,13," +
            "18,13,0-8,12,0z",

            endstation: "M10,15a5,5,0,0,1,5-5h65a5,5,0,0,1,5,5v80a5,5,0,0,1" +
            "-5,5h-65a5,5,0,0,1-5-5zM87.5,14l11,11a3,10,0,0,1,2,10v40a3,10," +
            "0,0,1,-2,10l-11,11zM17,19a2,2,0,0,1,2-2h56a2,2,0,0,1,2,2v26a2," +
            "2,0,0,1-2,2h-56a2,2,0,0,1-2-2zM20,20h54v10h-54zM20,33h54v10h" +
            "-54zM42,70a5,5,0,0,1,10,0a5,5,0,0,1-10,0z",

            router: "M10,55A45,45,0,0,1,100,55A45,45,0,0,1,10,55M20,50l12,0," +
            "0-8,18,13-18,13,0-8-12,0zM90,50l-12,0,0-8-18,13,18,13,0-8,12,0z" +
            "M50,47l0-12-8,0,13-18,13,18-8,0,0,12zM50,63l0,12-8,0,13,18,13" +
            "-18-8,0,0-12z",

            bgpSpeaker: "M10,40a45,35,0,0,1,90,0Q100,77,55,100Q10,77,10,40z" +
            "M50,29l12,0,0-8,18,13-18,13,0-8-12,0zM60,57l-12,0,0-8-18,13," +
            "18,13,0-8,12,0z",

            chain: "M60.4,77.6c-4.9,5.2-9.6,11.3-15.3,16.3c-8.6,7.5-20.4,6.8" +
            "-28-0.8c-7.7-7.7-8.4-19.6-0.8-28.4c6.5-7.4,13.5-14.4,20.9-20.9" +
            "c7.5-6.7,19.2-6.7,26.5-0.8c3.5,2.8,4.4,6.1,2.2,8.7c-2.7,3.1" +
            "-5.5,2.5-8.5,0.3c-4.7-3.4-9.7-3.2-14,0.9C37.1,58.7,31,64.8," +
            "25.2,71c-4.2,4.4-4.2,10.6-0.6,14.3c3.7,3.7,9.7,3.7,14.3-0.4" +
            "c2.9-2.5,5.3-5.5,8.3-8c1-0.9,3-1.1,4.4-0.9C54.8,76.3,57.9,77.1," +
            "60.4,77.6zM49.2,32.2c5-5.2,9.7-10.9,15.2-15.7c12.8-11,31.2" +
            "-4.9,34.3,11.2C100,34.2,98.3,40.2,94,45c-6.7,7.4-13.7,14.6" +
            "-21.2,21.2C65.1,73,53.2,72.7,46,66.5c-3.2-2.8-3.9-5.8-1.6-8.4" +
            "c2.6-2.9,5.3-2.4,8.2-0.3c5.2,3.7,10,3.3,14.7-1.1c5.8-5.6,11.6" +
            "-11.3,17.2-17.2c4.6-4.8,4.9-11.1,0.9-15c-3.9-3.9-10.1-3.4-15," +
            "1.2c-3.1,2.9-5.7,7.4-9.3,8.5C57.6,35.3,53,33,49.2,32.2z",

            crown: "M99.5,21.6c0,3-2.3,5.4-5.1,5.4c-0.3,0-0.7,0-1-0.1c-1.8," +
            "4-4.8,10-7.2,17.3c-3.4,10.6-0.9,26.2,2.7,27.3C90.4,72,91.3," +
            "75,88,75H22.7c-3.3,0-2.4-3-0.9-3.5c3.6-1.1,6.1-16.7,2.7-27.3" +
            "c-2.4-7.4-5.4-13.5-7.2-17.5c-0.5,0.2-1,0.3-1.6,0.3c-2.8,0" +
            "-5.1-2.4-5.1-5.4c0-3,2.3-5.4,5.1-5.4c2.8,0,5.1,2.4,5.1,5.4c0," +
            "1-0.2,1.9-0.7,2.7c0.7,0.8,1.4,1.6,2.4,2.6c8.8,8.9,11.9,12.7," +
            "18.1,11.7c6.5-1,11-8.2,13.3-14.1c-2-0.8-3.3-2.7-3.3-5.1c0-3," +
            "2.3-5.4,5.1-5.4c2.8,0,5.1,2.4,5.1,5.4c0,2.5-1.6,4.5-3.7,5.2" +
            "c2.3,5.9,6.8,13,13.2,14c6.2,1,9.3-2.7,18.1-11.7c0.7-0.7,1.4" +
            "-1.5,2-2.1c-0.6-0.9-1-2-1-3.1c0-3,2.3-5.4,5.1-5.4C97.2,16.2," +
            "99.5,18.6,99.5,21.6zM92,87.9c0,2.2-1.7,4.1-3.8,4.1H22.4c" +
            "-2.1,0-4.4-1.9-4.4-4.1v-3.3c0-2.2,2.3-4.5,4.4-4.5h65.8c2.1," +
            "0,3.8,2.3,3.8,4.5V87.9z"
        },

        badgeViewBox = '0 0 10 10',

        badgeData = {
            uiAttached: "M2,2.5a.5,.5,0,0,1,.5-.5h5a.5,.5,0,0,1,.5,.5v3" +
            "a.5,.5,0,0,1-.5,.5h-5a.5,.5,0,0,1-.5-.5zM2.5,2.8a.3,.3,0,0,1," +
            ".3-.3h4.4a.3,.3,0,0,1,.3,.3v2.4a.3,.3,0,0,1-.3,.3h-4.4" +
            "a.3,.3,0,0,1-.3-.3zM2,6.55h6l1,1.45h-8z"
        };

    // ----------------------------------------------------------------------

    angular.module('onosSvg')
        .factory('GlyphService', ['$log', 'FnService', function (_$log_, _fs_) {
            $log = _$log_;
            fs = _fs_;

            function clear() {
                // start with a fresh map
                glyphs = d3.map();
            }

            function init() {
                clear();
                register(birdViewBox, birdData);
                register(glyphViewBox, glyphData);
                register(badgeViewBox, badgeData);
            }

            function register(viewBox, data, overwrite) {
                var dmap = d3.map(data),
                    dups = [],
                    ok;

                dmap.forEach(function (key, value) {
                    if (!overwrite && glyphs.get(key)) {
                        dups.push(key);
                    } else {
                        glyphs.set(key, {id: key, vb: viewBox, d: value});
                    }
                });
                ok = (dups.length == 0);
                if (!ok) {
                    dups.forEach(function (id) {
                        $log.warn(msgGS + 'register(): ID collision: "'+id+'"');
                    });
                }
                return ok;
            }

            function ids() {
                return glyphs.keys();
            }

            function glyph(id) {
                return glyphs.get(id);
            }

            // Note: defs should be a D3 selection of a single <defs> element
            function loadDefs(defs, glyphIds) {
                var list = fs.isA(glyphIds) || ids();

                // remove all existing content
                defs.html(null);

                // load up the requested glyphs
                list.forEach(function (id) {
                    var g = glyph(id);
                    if (g) {
                        defs.append('symbol')
                            .attr({ id: g.id, viewBox: g.vb })
                            .append('path').attr('d', g.d);
                    }
                });
            }

            return {
                clear: clear,
                init: init,
                register: register,
                ids: ids,
                glyph: glyph,
                loadDefs: loadDefs
            };
        }]);

}());
