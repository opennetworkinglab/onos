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
 Module template file for DIV based view.

 @author Simon Hunt
 */

(function (onos) {
    'use strict';

    var list,
        data = [ 'foo', 'bar', 'baz' ];

    // invoked only the first time the view is loaded
    //  - used to initialize the view contents
    function init(view, ctx, flags) {
        // NOTE: view.$div is a D3 selection of the view's div
        list = view.$div.append('ul');
        // ... further code to initialize the SVG view ...

    }

    // invoked just prior to loading the view
    //  - used to clear the view of stale data
    function reset(view, ctx, flags) {

    }

    // invoked when the view is loaded
    //  - used to load data into the view,
    //     when the view is shown
    function load(view, ctx, flags) {
        list.selectAll('li')
            .data(data)
            .enter()
            .append('li')
            .text(function (d) { return d; })
    }

    // invoked when the view is unloaded
    //  - used to clean up data that should be removed,
    //     when the view is hidden
    function unload(view, ctx, flags) {

    }

    // invoked when the view is resized
    //  - used to reconfigure elements to the new view size
    function resize(view, ctx, flags) {
        var w = view.width(),
            h = view.height();

    }

    // invoked when the framework needs to alert the view of an error
    //  - (EXPERIMENTAL -- not currently used)
    function error(view, ctx, flags) {

    }

    // ================================================================
    // == register the view here, with links to lifecycle callbacks

    // A typical setup that initializes the view once, then reacts to
    // load and resize events would look like this:

    onos.ui.addView('myDivViewId', {
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
