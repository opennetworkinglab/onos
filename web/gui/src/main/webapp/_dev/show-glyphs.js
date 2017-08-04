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
 ONOS GUI -- Display ONOS Glyphs (development module)
 */

(function () {
    'use strict';

    var $log, $window, fs, gs;

    var vspace = 60;

    // assuming the glyph is a square
    // div is a D3 selection of the <DIV> element into which icon should load
    // size is the size of the glyph
    // id is the symbol id
    // rer is the rectangle the glyph will be in's rounded corners
    // svgClass is the class name for your glyph
    function createGlyph(div, size, id, rer, svgClass) {
        var dim = size || 20,
            texty = 30,
            gid = id || 'unknown',
            rx = rer || 4,
            svgCls = svgClass || 'embeddedGlyph',
            svg, g;

        svg = div.append('svg').attr({
            'class': svgCls,
            width: dim,
            height: dim + texty,
            viewBox: '0 0 ' + dim + ' ' + dim
        });

        svg.on('mousemove', function () {
            showZoomedGlyph(id);
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

        g.append('text')
            .attr({
                'text-anchor': 'left',
                y: '1em',
                x: 0,
                transform: 'translate(0, ' + dim + ')scale(0.8)'
            })
            .style('fill', '#800')
            .text(id);
    }

    function showZoomedGlyph(id) {
        var s = d3.select('#zoomed').select('svg'),
            zd = zsize();

        s.selectAll('g').remove();

        var g = s.append('g').attr({
            'class': 'glyph'
        });
        g.append('use').attr({
            width: zd,
            height: zd,
            'class': 'glyph',
            'xlink:href': '#' + id
        });

        d3.select('#tag').text(id);
    }

    function boxSize() {
        return {
            w: $window.innerWidth,
            h: $window.innerHeight - vspace
        };
    }

    function zsize() {
            return boxSize().w - 430;
    }

    function updateLayout() {
        var box = boxSize(),
            zdim = box.w - 425,
            zsdim = zdim - 5;

        $log.debug('updateLayout()', box);

        var c = d3.select('#container')
            .style('width', box.w + 'px')
            .style('height', box.h + 'px');

        c.select('#showGlyphs').style('height', (box.h - 4) + 'px');
        var z = c.select('#zoomed').style('height', (box.h - 4) + 'px')
            .style('width', zdim + 'px');

        var zsvg = z.select('svg');
        if (zsvg.empty()) {
            zsvg = z.append('svg');
        }
        zsvg.style({
            width: zsdim + 'px',
            height: zsdim + 'px'
        });
        zsvg.selectAll('g').remove();
        d3.select('#tag').text('');
    }

    function watchWindow() {
        var w = angular.element($window);
        w.bind('resize', function () {
            updateLayout();
        });
    }


    angular.module('showGlyphs', ['onosUtil', 'onosSvg'])
        .controller('OvShowGlyphs',
        ['$log', '$window', 'FnService', 'GlyphService',

        function (_$log_, _$window_, _fs_, _gs_) {
            var defs = d3.select('defs'),
                gDiv = d3.select('#showGlyphs'),
                cols = 6,
                k = 0;

            $log = _$log_;
            $window = _$window_;
            fs = _fs_;
            gs = _gs_;

            updateLayout();
            watchWindow();

            gs.init();
            gs.loadDefs(defs, null, true);

            gs.ids().forEach(function (id) {
                createGlyph(gDiv, 50, id);
                k += 1;
                if (k % cols > 0) {
                    gDiv.append('span').style('padding-left', '15px');
                } else {
                    gDiv.append('br');
                }
            });
        }]);
}());
