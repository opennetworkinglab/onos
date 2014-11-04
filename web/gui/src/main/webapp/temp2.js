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
 Temporary module file to test the framework integration.

 @author Simon Hunt
 */

(function (onos) {
    'use strict';

    var api = onos.api;

    var vid,
        svg;

    // == define your functions here.....


    // NOTE: view is a data structure:
    // {
    //     id: 'view-id',
    //     el: ...      // d3 selection of dom view div.
    // }

    function load(view) {
        vid = view.id;
        svg = view.el.append('svg')
            .attr({
                width: 400,
                height: 300
            });

        var fill = (vid === 'temp1') ? 'red' : 'blue',
            stroke = (vid === 'temp2') ? 'yellow' : 'black';

        svg.append('circle')
            .attr({
                cx: 200,
                cy: 150,
                r: 30
            })
            .style({
                fill: fill,
                stroke: stroke,
                'stroke-width': 3.5
            });
    }

    // == register views here, with links to lifecycle callbacks

    api.addView('temp1', {
        load: load
    });

    api.addView('temp2', {
        load: load
    });


}(ONOS));
