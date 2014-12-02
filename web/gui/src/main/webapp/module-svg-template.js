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
 Module template file for SVG based view.

 @author Simon Hunt
 */

(function (onos) {
    'use strict';

    var svg,
        data = [ 60 ];

    // invoked only the first time the view is loaded
    //  - used to initialize the view contents
    function init(view, ctx, flags) {
        svg = view.$div.append('svg');
        resize(view);
        // ... further code to initialize the SVG view ...

    }

    // invoked just prior to loading the view
    //  - used to clear the view of stale data
    function reset(view, ctx, flags) {
        // e.g. clear svg of all objects...
        // svg.html('');

    }

    // invoked when the view is loaded
    //  - used to load data into the view,
    //     when the view is shown
    function load(view, ctx, flags) {
        var w = view.width(),
            h = view.height();

        // as an example...
        svg.selectAll('circle')
            .data(data)
            .enter()
            .append('circle')
            .attr({
                cx: w / 2,
                cy: h / 2,
                r: function (d) { return d; }
            })
            .style({
                fill: 'goldenrod',
                stroke: 'black',
                'stroke-width': 3.5,
            });
    }

    // invoked when the view is unloaded
    //  - used to clean up data that should be removed,
    //     when the view is hidden
    function unload(view, ctx, flags) {

    }

    // invoked when the view is resized
    //  - used to reconfigure elements to the new size of the view
    function resize(view, ctx, flags) {
        var w = view.width(),
            h = view.height();

        // resize svg layer to match new size of view
        svg.attr({
            width: w,
            height: h
        });

        // as an example...
        svg.selectAll('circle')
            .attr({
                cx: w / 2,
                cy: h / 2
            });

        // ... further code to handle resize of view ...

    }

    // invoked when the framework needs to alert the view of an error
    //  - (EXPERIMENTAL -- not currently used)
    function error(view, ctx, flags) {

    }

    // ================================================================
    // == register the view here, with links to lifecycle callbacks

    // A typical setup that initializes the view once, then reacts to
    // load and resize events would look like this:

    onos.ui.addView('mySvgViewId', {
        init: init,
        load: load,
        resize: resize
    });

    // A minimum setup that builds the view every time it is loaded
    // would look like this:
    //
    //  onos.ui.addView('myViewId', {
    //      reset: true,    // clear view contents on reset
    //      load: load
    //  });

    // The complete gamut of callbacks would look like this:
    //
    //  onos.ui.addView('myViewId', {
    //      init: init,
    //      reset: reset,
    //      load: load,
    //      unload: unload,
    //      resize: resize,
    //      error: error
    //  });

}(ONOS));
