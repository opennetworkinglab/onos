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
        active: 'checkMark',
        inactive: 'xMark',

        plus: 'plus',
        minus: 'minus',
        play: 'play',
        stop: 'stop',

        refresh: 'refresh',
        garbage: 'garbage',

        upArrow: 'triangleUp',
        downArrow: 'triangleDown',

        loading: 'loading',

        appInactive: 'unknown',

        devIcon_SWITCH: 'switch',
        devIcon_ROADM: 'roadm',
        flowTable: 'flowTable',
        portTable: 'portTable',
        groupTable: 'groupTable',

        hostIcon_endstation: 'endstation',
        hostIcon_router: 'router',
        hostIcon_bgpSpeaker: 'bgpSpeaker',

        nav_apps: 'bird',
        nav_settings: 'chain',
        nav_cluster: 'node',
        nav_topo: 'topo',
        nav_devs: 'switch',
        nav_links: 'ports',
        nav_hosts: 'endstation',
        nav_intents: 'relatedIntents',
        nav_processors: 'allTraffic'
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
    // glyphId identifies the glyph to use
    // size is dimension of icon in pixels. Defaults to 20.
    // installGlyph, if truthy, will cause the glyph to be added to
    //      well-known defs element. Defaults to false.
    // svgClass is the CSS class used to identify the SVG layer.
    //      Defaults to 'embeddedIcon'.
    function loadIcon(div, glyphId, size, installGlyph, svgClass) {
        var dim = size || 20,
            svgCls = svgClass || 'embeddedIcon',
            gid = glyphId || 'unknown',
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
            'class': 'icon'
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

    // div is a D3 selection of the <DIV> element into which icon should load
    // iconCls is the CSS class used to identify the icon
    // size is dimension of icon in pixels. Defaults to 20.
    // installGlyph, if truthy, will cause the glyph to be added to
    //      well-known defs element. Defaults to false.
    // svgClass is the CSS class used to identify the SVG layer.
    //      Defaults to 'embeddedIcon'.
    function loadIconByClass(div, iconCls, size, installGlyph, svgClass) {
        loadIcon(div, glyphMapping[iconCls], size, installGlyph, svgClass);
        div.select('svg g').classed(iconCls, true);
    }

    function loadEmbeddedIcon(div, iconCls, size) {
        loadIconByClass(div, iconCls, size, true);
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
            gid = gs.glyphDefined(glyphId) ? glyphId : 'query',
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
            'xlink:href': '#' + gid,
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

    function sortIcons() {
        function sortAsc(div) {
            div.style('display', 'inline-block');
            loadEmbeddedIcon(div, 'upArrow', 10);
            div.classed('tableColSort', true);
        }

        function sortDesc(div) {
            div.style('display', 'inline-block');
            loadEmbeddedIcon(div, 'downArrow', 10);
            div.classed('tableColSort', true);
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
        .directive('icon', ['IconService', function (is) {
            return {
                restrict: 'A',
                link: function (scope, element, attrs) {
                    attrs.$observe('iconId', function () {
                        var div = d3.select(element[0]);
                        div.selectAll('*').remove();
                        is.loadEmbeddedIcon(div, attrs.iconId, attrs.iconSize);
                    });
                }
            };
        }])

        .factory('IconService', ['$log', 'FnService', 'GlyphService',
            'SvgUtilService',

        function (_$log_, _fs_, _gs_, _sus_) {
            $log = _$log_;
            fs = _fs_;
            gs = _gs_;
            sus = _sus_;

            return {
                loadIcon: loadIcon,
                loadIconByClass: loadIconByClass,
                loadEmbeddedIcon: loadEmbeddedIcon,
                addDeviceIcon: addDeviceIcon,
                addHostIcon: addHostIcon,
                iconConfig: function () { return config; },
                sortIcons: sortIcons
            };
        }]);

}());
