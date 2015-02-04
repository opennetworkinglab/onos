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
 ONOS GUI -- SVG -- Icon Service
 */
(function () {
    'use strict';

    var $log, fs, gs;

    var vboxSize = 50,
        cornerSize = vboxSize / 10,
        viewBox = '0 0 ' + vboxSize + ' ' + vboxSize;

    // maps icon id to the glyph id it uses.
    // note: icon id maps to a CSS class for styling that icon
    var glyphMapping = {
            deviceOnline: 'checkMark',
            deviceOffline: 'xMark',
            tableColSortAsc: 'triangleUp',
            tableColSortDesc: 'triangleDown',
            tableColSortNone: '-'
        };

    function ensureIconLibDefs() {
        var body = d3.select('body'),
            svg = body.select('svg#IconLibDefs'),
            defs;

        if (svg.empty()) {
            svg = body.append('svg').attr('id', 'IconLibDefs');
            defs = svg.append('defs');
        }
        return svg.select('defs');
    }

    angular.module('onosSvg')
        .factory('IconService', ['$log', 'FnService', 'GlyphService',
        function (_$log_, _fs_, _gs_) {
            $log = _$log_;
            fs = _fs_;
            gs = _gs_;

            // div is a D3 selection of the <DIV> element into which icon should load
            // iconCls is the CSS class used to identify the icon
            // size is dimension of icon in pixels. Defaults to 20.
            // installGlyph, if truthy, will cause the glyph to be added to
            //      well-known defs element. Defaults to false.
            // svgClass is the CSS class used to identify the SVG layer.
            //      Defaults to 'embeddedIcon'.
            function loadIcon(div, iconCls, size, installGlyph, svgClass) {
                var dim = size || 20,
                    svgCls = svgClass || 'embeddedIcon',
                    gid = glyphMapping[iconCls] || 'unknown',
                    svg, g;

                if (installGlyph) {
                    gs.loadDefs(ensureIconLibDefs(), [gid], true);
                }

                svg = div.append('svg').attr({
                        'class': svgCls,
                        width: dim,
                        height: dim,
                        viewBox: viewBox
                    });

                g = svg.append('g').attr({
                    'class': 'icon ' + iconCls
                });

                g.append('rect').attr({
                    width: vboxSize,
                    height: vboxSize,
                    rx: cornerSize
                });

                if (gid !== '-') {
                    g.append('use').attr({
                        width: vboxSize,
                        height: vboxSize,
                        'class': 'glyph',
                        'xlink:href': '#' + gid
                    });
                }
            }

            function loadEmbeddedIcon(div, iconCls, size) {
                loadIcon(div, iconCls, size, true);
            }

            return {
                loadIcon: loadIcon,
                loadEmbeddedIcon: loadEmbeddedIcon
            };
        }]);

}());
