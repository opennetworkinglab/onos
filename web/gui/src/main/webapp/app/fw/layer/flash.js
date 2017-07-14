/*
 * Copyright 2015-present Open Networking Foundation
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
 ONOS GUI -- Layer -- Flash Service

 Provides a mechanism to flash short informational messages to the screen
 to alert the user of something, e.g. "Hosts visible" or "Hosts hidden".
 */
(function () {
    'use strict';

    // injected references
    var $timeout;

    // configuration
    var defaultSettings = {
            fade: 200,
            showFor: 1200,
        },
        w = '100%',
        h = 200,
        xpad = 20,
        ypad = 10,
        rx = 10,
        vbox = '-200 -' + (h/2) + ' 400 ' + h;

    // internal state
    var settings,
        timer = null,
        data = [],
        enabled;

    // DOM elements
    var flashDiv, svg;


    function computeBox(el) {
        var text = el.select('text'),
            box = text.node().getBBox();

        // center
        box.x = -box.width / 2;
        box.y = -box.height / 2;

        // add some padding
        box.x -= xpad;
        box.width += xpad * 2;
        box.y -= ypad;
        box.height += ypad * 2;

        return box;
    }

    function updateFlash() {
        if (!svg) {
            svg = flashDiv.append('svg').attr({
                width: w,
                height: h,
                viewBox: vbox,
            });
        }

        var items = svg.selectAll('.flashItem')
            .data(data);

        // this is when there is an existing item
        items.each(function (msg) {
            var el = d3.select(this),
                box;

            el.select('text').text(msg);
            box = computeBox(el);
            el.select('rect').attr(box);
        });


        // this is when there is no existing item
        var entering = items.enter()
            .append('g')
            .attr({
                class: 'flashItem',
                opacity: 0,
            })
            .transition()
            .duration(settings.fade)
            .attr('opacity', 1);

        entering.each(function (msg) {
            var el = d3.select(this),
                box;

            el.append('rect').attr('rx', rx);
            el.append('text').text(msg);
            box = computeBox(el);
            el.select('rect').attr(box);
        });

        items.exit()
            .transition()
            .duration(settings.fade)
            .attr('opacity', 0)
            .remove();

        if (svg && data.length === 0) {
            svg.transition()
                .delay(settings.fade + 10)
                .remove();
            svg = null;
        }
    }

    function flash(msg) {
        if (!enabled) return;

        if (timer) {
            $timeout.cancel(timer);
        }

        timer = $timeout(function () {
            data = [];
            updateFlash();
        }, settings.showFor);

        data = [msg];
        updateFlash();
    }

    function enable(b) {
        enabled = !!b;
    }

    function tempDiv(ms) {
        var div = d3.select('body').append('div').classed('centered', true),
            delay = (ms === undefined || ms < 100) ? 3000 : ms;
        $timeout(function () { div.remove(); }, delay);
        return div;
    }

    angular.module('onosLayer')
        .factory('FlashService', ['$timeout',
        function (_$timeout_) {
            $timeout = _$timeout_;

            function initFlash(opts) {
                settings = angular.extend({}, defaultSettings, opts);
                flashDiv = d3.select('#flash');
                enabled = true;
            }

            return {
                initFlash: initFlash,
                flash: flash,
                enable: enable,
                tempDiv: tempDiv,
            };
        }]);

}());
