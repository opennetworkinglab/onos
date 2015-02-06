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
 ONOS GUI -- Layer -- Veil Service

 Provides a mechanism to display an overlaying div with information.
 Used mainly for web socket connection interruption.
 */
(function () {
    'use strict';

    // injected references
    var $log, fs;

    var veil, svg;

    function show(msg) {
        veil.selectAll('p').remove();

        //veil.data(msg).enter().append('p').text(function (d) { return d; });

        msg.forEach(function (line) {
            veil.insert('p').text(line);
        });

        veil.style('display', 'block');

        // TODO: disable key bindings
    }

    function hide() {
        veil.style('display', 'none');
        // TODO: re-enable key bindings
    }

    angular.module('onosLayer')
        .factory('VeilService', ['$log', 'FnService', 'GlyphService',
            function (_$log_, _fs_, gs) {
                $log = _$log_;
                fs = _fs_;

                veil = d3.select('#veil');

                svg = veil.append('svg').attr({
                    width: 500,
                    height: 500,
                    viewBox: '0 0 500 500'
                });

                gs.addGlyph(svg, 'bird', 400);

                return {
                    show: show,
                    hide: hide
                };
        }]);

}());
