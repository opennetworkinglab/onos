/*
 * Copyright 2014,2015 Open Networking Laboratory
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
 ONOS GUI -- Showing Icons Test Module
 */

(function () {
    'use strict';

    // assuming the glyph is a square
    // div is a D3 selection of the <DIV> element into which icon should load
    // size is the size of the glyph
    // id is the symbol id
    // rer is the rectangle the glyph will be in's rounded corners
    // svgClass is the class name for your glyph
    function createGlyph(div, size, id, rer, svgClass) {
        var dim = size || 20,
            gid = id || 'unknown',
            rx = rer || 4,
            svgCls = svgClass || 'embeddedGlyph',
            svg, g;

        svg = div.append('svg').attr({
            'class': svgCls,
            width: dim,
            height: dim,
            viewBox: '0 0 ' + dim + ' ' + dim
        });

        g = svg.append('g').attr({
            'class': 'glyph'
        });

        g.append('rect').attr({
            width: dim,
            height: dim,
            rx: rx
        });

        g.append('use').attr({
            width: dim,
            height: dim,
            'class': 'glyph',
            'xlink:href': '#' + gid
        });

}

    angular.module('showIconsTest', ['onosSvg'])

        .controller('OvShowIconsTest', ['$log', 'GlyphService', 'IconService',
            function ($log, gs, icns) {
                var self = this;

                gs.init();

                var div = d3.select('#showIcons');

                // show device online and offline icons
                icns.loadEmbeddedIcon(div, 'deviceOnline', 50);
                div.append('br');
                icns.loadEmbeddedIcon(div, 'deviceOffline', 50);

                var defs = d3.select('defs');

                // show all glyphs in glyph library
                gs.loadDefs(defs, null, true);
                var list = gs.ids(),
                    gDiv = d3.select('#showGlyphs'),
                    ctr = 0;
                list.forEach(function (id) {
                    createGlyph(gDiv, 50, id);
                    ctr += 1;
                    if(ctr/3 > 1) {
                        ctr = 0;
                        gDiv.append('p');
                    }
                });

                $log.log('OvShowIconsTest has been created');
            }]);
}());
