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

    var $log, fs, gs, sus;

    var vboxSize = 50,
        cornerSize = vboxSize / 10,
        viewBox = '0 0 ' + vboxSize + ' ' + vboxSize;

    // Maps icon ID to the glyph ID it uses.
    // NOTE: icon ID maps to a CSS class for styling that icon
    var glyphMapping = {
        deviceOnline: 'checkMark',
        deviceOffline: 'xMark',
        devIcon_SWITCH: 'switch',

        tableColSortAsc: 'triangleUp',
        tableColSortDesc: 'triangleDown'
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

        g.append('use').attr({
            width: vboxSize,
            height: vboxSize,
            'class': 'glyph',
            'xlink:href': '#' + gid
        });
}

    function loadEmbeddedIcon(div, iconCls, size) {
        loadIcon(div, iconCls, size, true);
    }


    // configuration for device and host icons in the topology view
    var config = {
        device: {
            dim: 36,
            rx: 4
        },
        host: {
            radius: {
                noGlyph: 9,
                withGlyph: 14
            },
            glyphed: {
                endstation: 1,
                bgpSpeaker: 1,
                router: 1
            }
        }
    };


    // Adds a device icon to the specified element, using the given glyph.
    // Returns the D3 selection of the icon.
    function addDeviceIcon(elem, glyphId) {
        var cfg = config.device,
            g = elem.append('g')
                .attr('class', 'svgIcon deviceIcon');

        g.append('rect').attr({
            x: 0,
            y: 0,
            rx: cfg.rx,
            width: cfg.dim,
            height: cfg.dim
        });

        g.append('use').attr({
            'xlink:href': '#' + glyphId,
            width: cfg.dim,
            height: cfg.dim
        });

        g.dim = cfg.dim;
        return g;
    }

    function addHostIcon(elem, radius, glyphId) {
        var dim = radius * 1.5,
            xlate = -dim / 2,
            g = elem.append('g')
                .attr('class', 'svgIcon hostIcon');

        g.append('circle').attr('r', radius);

        g.append('use').attr({
            'xlink:href': '#' + glyphId,
            width: dim,
            height: dim,
            transform: sus.translate(xlate,xlate)
        });
        return g;
    }

    function createSortIcon() {
       function sortAsc(div) {
            div.style('display', 'inline-block');
            loadEmbeddedIcon(div, 'tableColSortAsc', 10);
        }

        function sortDesc(div) {
            div.style('display', 'inline-block');
            loadEmbeddedIcon(div, 'tableColSortDesc', 10);
        }

        function sortNone(div) {
            div.remove();
        }

        return {
            sortAsc: sortAsc,
            sortDesc: sortDesc,
            sortNone: sortNone
        };
    }


    // =========================
    // === DEFINE THE MODULE

    angular.module('onosSvg')
        .factory('IconService', ['$log', 'FnService', 'GlyphService',
            'SvgUtilService',

        function (_$log_, _fs_, _gs_, _sus_) {
            $log = _$log_;
            fs = _fs_;
            gs = _gs_;
            sus = _sus_;

            return {
                loadIcon: loadIcon,
                loadEmbeddedIcon: loadEmbeddedIcon,
                addDeviceIcon: addDeviceIcon,
                addHostIcon: addHostIcon,
                iconConfig: function () { return config; },
                createSortIcon: createSortIcon
            };
        }]);

}());
