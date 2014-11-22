/*
 * Copyright 2014 Open Networking Laboratory
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
 ONOS GUI -- Feedback layer

 Defines the feedback layer for the UI. Used to give user visual feedback
 of choices, typically responding to keystrokes.

 @author Simon Hunt
 */

(function (onos){
    'use strict';

    // API's
    var api = onos.api;

    // Config variables
    var w = '100%',
        h = 200,
        fade = 200,
        showFor = 1200,
        vb = '-200 -' + (h/2) + ' 400 ' + h,
        xpad = 20,
        ypad = 10;

    // State variables
    var timer = null,
        data = [];

    // DOM elements and the like
    var fb = d3.select('#feedback');

    var svg;

    //var svg = fb.append('svg').attr({
    //    width: w,
    //    height: h,
    //    viewBox: vb
    //});

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

    function updateFeedback() {
        if (!svg) {
            svg = fb.append('svg').attr({
                width: w,
                height: h,
                viewBox: vb
            });
        }

        var items = svg.selectAll('.feedbackItem')
            .data(data);

        var entering = items.enter()
            .append('g')
            .attr({
                class: 'feedbackItem',
                opacity: 0
            })
            .transition()
            .duration(fade)
            .attr('opacity', 1);

        entering.each(function (d) {
            var el = d3.select(this),
                box;

            d.el = el;
            el.append('rect').attr({ rx: 10, ry: 10});
            el.append('text').text(d.label);
            box = computeBox(el);
            el.select('rect').attr(box);
        });

        items.exit()
            .transition()
            .duration(fade)
            .attr({ opacity: 0})
            .remove();

        if (svg && data.length === 0) {
            svg.transition()
                .delay(fade + 10)
                .remove();
            svg = null;
        }
    }

    function clearFlash() {
        if (timer) {
            clearInterval(timer);
        }
        data = [];
        updateFeedback();
    }

    // for now, simply display some text feedback
    function flash(text) {
        // cancel old scheduled event if there was one
        if (timer) {
            clearInterval(timer);
        }
        timer = setInterval(function () {
            clearFlash();
        }, showFor);

        data = [{
            label: text
        }];
        updateFeedback();
    }

    onos.ui.addLib('feedback', {
        flash: flash
    });
}(ONOS));
