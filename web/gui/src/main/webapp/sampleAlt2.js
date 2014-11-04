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
 Sample module file to illustrate framework integration.

 @author Simon Hunt
 */

(function (onos) {
    'use strict';

    var svg;


    function sizeSvg(view) {
        svg.attr({
            width: view.width(),
            height: view.height()
        });
    }

    // NOTE: view is a view-token data structure:
    // {
    //     vid: 'view-id',
    //     nid: 'nav-id',
    //     $div: ...      // d3 selection of dom view div.
    // }

    // gets invoked only the first time the view is loaded
    function preload(view, ctx) {
        svg = view.$div.append('svg');
        sizeSvg(view);
    }

    function reset(view) {
        // clear our svg of all objects
        svg.html('');
    }

    function load(view, ctx) {
        var fill = 'blue',
            stroke = 'grey';

        svg.append('circle')
            .attr({
                cx: view.width() / 2,
                cy: view.height() / 2,
                r: 30
            })
            .style({
                fill: fill,
                stroke: stroke,
                'stroke-width': 3.5
            });
    }

    function resize(view, ctx) {
        sizeSvg(view);
        svg.selectAll('circle')
            .attr({
                cx: view.width() / 2,
                cy: view.height() / 2
            });
    }

    // == register views here, with links to lifecycle callbacks

    onos.ui.addView('sampleAlt', {
        preload: preload,
        reset: reset,
        load: load,
        resize: resize
    });


}(ONOS));
