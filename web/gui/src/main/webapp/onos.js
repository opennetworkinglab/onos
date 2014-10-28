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
 ONOS UI Framework.

 @author Simon Hunt
 */

(function ($) {
    'use strict';
    var tsI = new Date().getTime(),         // initialize time stamp
        tsB;                                // build time stamp

    // attach our main function to the jQuery object
    $.onos = function (options) {
        // private namespaces
        var publicApi;             // public api

        // internal state
        var views = {},
            currentView = null,
            built = false;

        // DOM elements etc.
        var $mast;


        // various functions..................

        // throw an error
        function throwError(msg) {
            // todo: maybe add tracing later
            throw new Error(msg);
        }

        // define all the public api functions...
        publicApi = {
            printTime: function () {
                console.log("the time is " + new Date());
            },

            addView: function (vid, cb) {
                views[vid] = {
                    vid: vid,
                    cb: cb
                };
                // TODO: proper registration of views
                // for now, make the one (and only) view current..
                currentView = views[vid];
            }
        };

        // function to be called from index.html to build the ONOS UI
        function buildOnosUi() {
            tsB = new Date().getTime();
            tsI = tsB - tsI; // initialization duration

            console.log('ONOS UI initialized in ' + tsI + 'ms');

            if (built) {
                throwError("ONOS UI already built!");
            }
            built = true;

            // TODO: invoke hash navigation
            // --- report build errors ---

            // for now, invoke the one and only load function:

            currentView.cb.load();
        }


        // export the api and build-UI function
        return {
            api: publicApi,
            buildUi: buildOnosUi
        };
    };

}(jQuery));