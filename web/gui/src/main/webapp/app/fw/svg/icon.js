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

 @author Simon Hunt
 */
(function () {
    'use strict';

    var $log, fs;

    var viewBoxDim = 50,
        viewBox = '0 0 ' + viewBoxDim + ' ' + viewBoxDim;

    // maps icon id to the glyph id it uses.
    // note: icon id maps to a CSS class for styling that icon
    var glyphMapping = {
            deviceOnline: 'checkMark',
            deviceOffline: 'xMark'
        };

    angular.module('onosSvg')
        .factory('IconService', ['$log', 'FnService', function (_$log_, _fs_) {
            $log = _$log_;
            fs = _fs_;

            // div is a D3 selection of the <DIV> element into which icon should load
            // iconCls is the CSS class used to identify the icon
            // size is dimension of icon in pixels. Defaults to 20.
            function loadIcon(div, iconCls, size) {
                var dim = size || 20,
                    gid = glyphMapping[iconCls] || 'unknown';

                var svg = div.append('svg').attr({
                        width: dim,
                        height: dim,
                        viewBox: viewBox
                    });
                var g = svg.append('g').attr({
                    'class': 'icon ' + iconCls
                });
                g.append('rect').attr({
                    width: viewBoxDim,
                    height: viewBoxDim,
                    rx: 4
                });
                g.append('use').attr({
                    width: viewBoxDim,
                    height: viewBoxDim,
                    'class': 'glyph',
                    'xlink:href': '#' + gid
                });
            }

            return {
                loadIcon: loadIcon
            };
        }]);

}());
