/*
 *  Copyright 2016-present Open Networking Laboratory
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

/*
 ONOS GUI -- Test code for displaying custom glyphs
 */

(function () {
    'use strict';

    // assuming the glyph is a square
    // div is a D3 selection of the <DIV> element into which icon should load
    // size is the size of the glyph
    // id is the symbol id
    function createGlyph(div, size, id) {
        var dim = size || 20,
            texty = 30,
            gid = id || 'unknown',
            rx = 4,
            svgCls = 'embeddedGlyph',
            svg, g;

        svg = div.append('svg').attr({
            'class': svgCls,
            width: dim,
            height: dim + texty,
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

        g.append('text')
            .attr({
                'text-anchor': 'left',
                y: '1em',
                x: 0,
                transform: 'translate(0, ' + dim + ')'
            })
            .style('fill', '#800')
            .text(id);
    }

    var clock = 'M92.9,61.3a39,39,0,1,1-39-39,39,39,0,0,1,39,39h0Z' +
        'M44,19.3c-4.4-7.4-14.8-9.3-23.2-4.2S9.1,30.2,13.5,37.6m80.8,0' +
        'c4.4-7.4,1.2-17.5-7.3-22.5s-18.8-3.2-23.3,4.2m-8.4,1.8V16.5h4.4' +
        'V11.9H48.2v4.6h4.6v4.6M51.6,56.4H51.5' +
        'a5.4,5.4,0,0,0,2.4,10.3,4.7,4.7,0,0,0,4.9-3.1H74.5' +
        'a2.2,2.2,0,0,0,2.4-2.2,2.4,2.4,0,0,0-2.4-2.3H58.8' +
        'a5.3,5.3,0,0,0-2.5-2.6H56.2V32.9' +
        'a2.3,2.3,0,0,0-.6-1.7,2.2,2.2,0,0,0-1.6-.7,2.4,2.4,0,0,0-2.4,2.4' +
        'h0V56.4M82.2,91.1l-7.1,5.3-0.2.2-1.2,2.1a0.6,0.6,0,0,0,.2.8' +
        'h0.2c2.6,0.4,10.7.9,10.3-1.2m-60.8,0c-0.4,2.1,7.7,1.6,10.3,1.2' +
        'h0.2a0.6,0.6,0,0,0,.2-0.8l-1.2-2.1-0.2-.2-7.1-5.3';

    var clocks = "M65.7,26.6A34.7,34.7,0,0,0,31,61.5c0,19,16,35.2,35.5,34.8" +
        "a35.1,35.1,0,0,0,34-35.1A34.7,34.7,0,0,0,65.7,26.6Z" +
        "m8.6-2.7,27.4,16.4a31.3,31.3,0,0,0,1.2-3.1c1.3-5,0-9.4-3.2-13.2" +
        "a16.9,16.9,0,0,0-16-6.2A12.8,12.8,0,0,0,74.3,23.9Z" +
        "M57,23.9L56.4,23a12.9,12.9,0,0,0-10.7-5.5,16.9,16.9,0,0,0-15.1,8," +
        "14.1,14.1,0,0,0-2.3,11.2,10.4,10.4,0,0,0,1.4,3.5Z" +
        "M26.9,31.6A33.7,33.7,0,0,0,9.7,59.3C9.2,72.8,14.9,83.1,26,90.7l1-1.9" +
        "a0.6,0.6,0,0,0-.2-1A34.2,34.2,0,0,1,15.5,50.4" +
        "a33.8,33.8,0,0,1,10.8-16,1.2,1.2,0,0,0,.5-0.6" +
        "C26.9,33.1,26.9,32.4,26.9,31.6Z" +
        "m1,8.1C14.6,55.9,19.2,81,37.6,91.1l1-2.3-2.8-2.4" +
        "C26.4,77.6,22.9,66.7,25.1,54" +
        "a31.6,31.6,0,0,1,4.2-10.8,0.8,0.8,0,0,0,.1-0.6Z" +
        "M12,38.4a11.2,11.2,0,0,1-1.4-5.8A13.7,13.7,0,0,1,14.3,24" +
        "a17.3,17.3,0,0,1,10.5-5.7h0.4C19,18,13.7,20,9.9,25.2" +
        "a14.5,14.5,0,0,0-3,11,11.2,11.2,0,0,0,1.6,4.3Z" +
        "m5.5-2.7L21,33" +
        "a1,1,0,0,0,.3-0.7,14,14,0,0,1,3.9-8.6,17.3,17.3,0,0,1,10.2-5.4" +
        "l0.6-.2C24.4,17.3,16.4,27,17.4,35.7Z" +
        "M70.9,17.2H60.7v4.1h4v4.2H67V21.3h3.9V17.2Z" +
        "M90.9,87.9l-0.5.3L86,91.5a7.9,7.9,0,0,0-2.6,3.1" +
        "c-0.3.6-.2,0.8,0.5,0.9a27.9,27.9,0,0,0,6.8.2l1.6-.4" +
        "a0.8,0.8,0,0,0,.6-1.2l-0.4-1.4Z" +
        "m-50.2,0-1.8,6c-0.3,1.1-.1,1.4.9,1.7h0.7a26.3,26.3,0,0,0,7.2-.1" +
        "c0.8-.1.9-0.4,0.5-1.1a8.2,8.2,0,0,0-2.7-3.1Z" +
        "m-10.8-.4-0.2.6L28,93.5a0.9,0.9,0,0,0,.7,1.3,7.7,7.7,0,0,0,2.2.4" +
        "l5.9-.2c0.5,0,.7-0.3.5-0.8a7.6,7.6,0,0,0-2.2-2.9Z" +
        "m-11.3,0-0.2.7-1.6,5.4c-0.2.8-.1,1.2,0.7,1.4a8,8,0,0,0,2.2.4l6-.2" +
        "c0.4,0,.7-0.3.5-0.6a7.1,7.1,0,0,0-1.9-2.7l-2.8-2.1Z" +
        "M65.7,26.6m-2,30.3a4.4,4.4,0,0,0-2.2,2,4.8,4.8,0,0,0,4,7.2," +
        "4.1,4.1,0,0,0,4.3-2.3,0.8,0.8,0,0,1,.8-0.5H84.1" +
        "a1.9,1.9,0,0,0,2-1.7,2.1,2.1,0,0,0-1.7-2.2H70.8" +
        "a1,1,0,0,1-1-.5,6.4,6.4,0,0,0-1.5-1.6,1.1,1.1,0,0,1-.5-1" +
        "q0-10.1,0-20.3a1.9,1.9,0,0,0-2.2-2.1,2.1,2.1,0,0,0-1.8,2.2" +
        "q0,6.1,0,12.2C63.7,51.2,63.7,54,63.7,56.9Z";

    var smiley = "M97,20.3A9.3,9.3,0,0,0,87.7,11H24.3A9.3,9.3,0,0,0,15,20.3" +
        "V64.7A9.3,9.3,0,0,0,24.3,74H87.7A9.3,9.3,0,0,0,97,64.7V20.3Z" +
        "M54.5,19.1A24.3,24.3,0,1,1,30.2,43.3,24.3,24.3,0,0,1,54.5,19.1Z" +
        "M104.7,94.9L97.6,82.8c-0.9-1.6-3.7-2.8-6.1-2.8H18.9" +
        "c-2.5,0-5.2,1.3-6.1,2.8L5.6,94.9C4.3,97.1,5.7,99,8.9,99h92.6" +
        "C104.6,99,106.1,97.1,104.7,94.9ZM54.5,56.5" +
        "c-7.3,0-17.2-8.6-13.3-7.4,13,3.9,13.7,3.9,26.5,0" +
        "C71.7,48,61.9,56.6,54.5,56.6Z" +
        "M38,33.9C38,32,40.2,31,42.1,31h7.3" +
        "a3.2,3.2,0,0,1,3.1,1.7,13.1,13.1,0,0,1,2.1-.3,12.9,12.9,0,0,1,2.1.4" +
        "A3.3,3.3,0,0,1,59.7,31H67c1.9,0,4,1,4,2.9v3.2A4.4,4.4,0,0,1,67,41" +
        "H59.7A4,4,0,0,1,56,37.1V33.9h0a4.4,4.4,0,0,0-1.6-.2l-1.5.2H53v3.2" +
        "A4,4,0,0,1,49.4,41H42.1A4.4,4.4,0,0,1,38,37.1V33.9Z";

    var glyphData = {
        _viewbox: "0 0 110 110",
        alarm: clock,
        alarms: clocks,
        smiley: smiley
    };

    angular.module('showGlyph', ['onosSvg'])
    .controller('OvShowGlyph', ['$log', 'GlyphService',

        function ($log, gs) {
            var gDiv = d3.select('#showGlyphs'),
                defs = d3.select('defs');

            // register out-of-the-box glyphs
            gs.init();

            // register our custom glyphs
            gs.registerGlyphSet(glyphData);

            // load all defined glyphs into our <defs> element
            gs.loadDefs(defs);

            // choose a glyph to render
            createGlyph(gDiv, 400, 'switch');
        }]);
}());
