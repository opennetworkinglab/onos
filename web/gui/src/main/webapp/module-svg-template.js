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

    var svg;

    // set the size of the SVG layer to match that of the view
    function sizeSvg(view) {
        svg.attr({
            width: view.width(),
            height: view.height()
        });
    }

    // invoked only the first time the view is loaded
    function preload(view, ctx) {
        // NOTE: view.$div is a D3 selection of the view's div
        svg = view.$div.append('svg');
        sizeSvg(view);
        // ... further code to initialize the SVG view ...

    }

    // invoked just prior to loading the view
    function reset(view) {

    }

    // invoked when the view is loaded
    function load(view, ctx) {

    }

    // invoked when the view is resized
    function resize(view, ctx) {
        sizeSvg(view);
        // ... further code to handle resize of view ...

    }

    // == register the view here, with links to lifecycle callbacks

    onos.ui.addView('myViewId', {
        preload: preload,
        reset: reset,
        load: load,
        // unload: unload,
        // error: error
        resize: resize
    });

}(ONOS));
