/*
 * Copyright 2015-present Open Networking Laboratory
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
    var $log, $route, fs, ks, gs;

    var veil;

    function init() {
        var wSize = fs.windowSize(),
            ww = wSize.width,
            wh = wSize.height,
            shrink = wh * 0.3,
            birdDim = wh - shrink,
            birdCenter = (ww - birdDim) / 2,
            svg;

        veil = d3.select('#veil');

        svg = veil.select('svg').attr({
            width: ww,
            height: wh
        }).style('opacity', 0.2);

        gs.addGlyph(svg, 'bird', birdDim, false, [birdCenter, shrink/2]);
    }

    // msg should be an array of strings
    function show(msg) {
        var msgs = fs.isA(msg) || [msg],
            pdiv = veil.select('.msg');
        pdiv.selectAll('p').remove();

        msgs.forEach(function (line) {
            pdiv.append('p').text(line);
        });

        veil.style('display', 'block');
        ks.enableKeys(false);
    }

    function hide() {
        veil.style('display', 'none');
        ks.enableKeys(true);
    }

    // function that only invokes the veil if the caller is the current view
    // TODO: review - is this deprecated ?
    function lostServer(ctrlName, msg) {
        if ($route.current.$$route.controller === ctrlName) {
            $log.debug('VEIL-service: ', ctrlName);
            show(msg)
        } else {
            $log.debug('VEIL-service: IGNORING ', ctrlName);
        }
    }

    angular.module('onosLayer')
    .factory('VeilService',
        ['$log', '$route', 'FnService', 'KeyService', 'GlyphService', 'WebSocketService',

        function (_$log_, _$route_, _fs_, _ks_, _gs_, wss) {
            $log = _$log_;
            $route = _$route_;
            fs = _fs_;
            ks = _ks_;
            gs = _gs_;

            var self = {
                init: init,
                show: show,
                hide: hide,
                lostServer: lostServer
            };
            wss._setVeilDelegate(self);
            return self;
    }]);

}());
