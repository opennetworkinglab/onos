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
 ONOS GUI -- Base Framework

 @author Simon Hunt
 */

(function ($) {
    'use strict';
    var tsI = new Date().getTime(),         // initialize time stamp
        tsB,                                // build time stamp
        defaultHash = 'temp1';


    // attach our main function to the jQuery object
    $.onos = function (options) {
        var publicApi;             // public api

        // internal state
        var views = {},
            current = {
                view: null,
                ctx: ''
            },
            built = false,
            errorCount = 0;

        // DOM elements etc.
        var $view;


        // ..........................................................
        // Internal functions

        // throw an error
        function throwError(msg) {
            // separate function, as we might add tracing here too, later
            throw new Error(msg);
        }

        function doError(msg) {
            errorCount++;
            console.warn(msg);
        }

        // hash navigation
        function hash() {
            var hash = window.location.hash,
                redo = false,
                view,
                t;

            if (!hash) {
                hash = defaultHash;
                redo = true;
            }

            t = parseHash(hash);
            if (!t || !t.vid) {
                doError('Unable to parse target hash: ' + hash);
            }

            view = views[t.vid];
            if (!view) {
                doError('No view defined with id: ' + t.vid);
            }

            if (redo) {
                window.location.hash = makeHash(t);
                // the above will result in a hashchange event, invoking
                // this function again
            } else {
                // hash was not modified... navigate to where we need to be
                navigate(hash, view, t);
            }

        }

        function parseHash(s) {
            // extract navigation coordinates from the supplied string
            // "vid,ctx" --> { vid:vid, ctx:ctx }

            var m = /^[#]{0,1}(\S+),(\S*)$/.exec(s);
            if (m) {
                return { vid: m[1], ctx: m[2] };
            }

            m = /^[#]{0,1}(\S+)$/.exec(s);
            return m ? { vid: m[1] } : null;
        }

        function makeHash(t, ctx) {
            // make a hash string from the given navigation coordinates.
            // if t is not an object, then it is a vid
            var h = t,
                c = ctx || '';

            if ($.isPlainObject(t)) {
                h = t.vid;
                c = t.ctx || '';
            }

            if (c) {
                h += ',' + c;
            }
            return h;
        }

        function navigate(hash, view, t) {
            // closePanes()     // flyouts etc.
            // updateNav()      // accordion / selected nav item
            createView(view);
            setView(view, hash, t);
        }

        function reportBuildErrors() {
            // TODO: validate registered views / nav-item linkage etc.
            console.log('(no build errors)');
        }

        // ..........................................................
        // View life-cycle functions

        function createView(view) {
            var $d;
            // lazy initialization of the view
            if (view && !view.$div) {
                $d = $view.append('div')
                        .attr({
                            id: view.vid
                         });
                view.$div = $d;     // cache a reference to the selected div
            }
        }

        function setView(view, hash, t) {
            // set the specified view as current, while invoking the
            // appropriate life-cycle callbacks

            // if there is a current view, and it is not the same as
            // the incoming view, then unload it...
            if (current.view && !(current.view.vid !== view.vid)) {
                current.view.unload();
            }

            // cache new view and context
            current.view = view;
            current.ctx = t.ctx || '';

            // TODO: clear radio button set (store on view?)

            // preload is called only once, after the view is in the DOM
            if (!view.preloaded) {
                view.preload(t.ctx);
            }

            // clear the view of stale data
            view.reset();

            // load the view
            view.load(t.ctx);
        }

        function resizeView() {
            if (current.view) {
                current.view.resize();
            }
        }

        // ..........................................................
        // View class
        //   Captures state information about a view.

        // Constructor
        //      vid : view id
        //      nid : id of associated nav-item (optional)
        //      cb  : callbacks (preload, reset, load, resize, unload, error)
        //      data: custom data object (optional)
        function View(vid) {
            var av = 'addView(): ',
                args = Array.prototype.slice.call(arguments),
                nid,
                cb,
                data;

            args.shift();   // first arg is always vid
            if (typeof args[0] === 'string') {  // nid specified
                nid = args.shift();
            }
            cb = args.shift();
            data = args.shift();

            this.vid = vid;

            if (validateViewArgs(vid)) {
                this.nid = nid;     // explicit navitem id (can be null)
                this.cb = $.isPlainObject(cb) ? cb : {};    // callbacks
                this.data = data;   // custom data (can be null)
                this.$div = null;   // view not yet added to DOM
                this.ok = true;     // valid view
            }

        }

        function validateViewArgs(vid) {
            var ok = false;
            if (typeof vid !== 'string' || !vid) {
                doError(av + 'vid required');
            } else if (views[vid]) {
                doError(av + 'View ID "' + vid + '" already exists');
            } else {
                ok = true;
            }
            return ok;
        }

        var viewInstanceMethods = {
            toString: function () {
                return '[View: id="' + this.vid + '"]';
            },

            token: function() {
                return {
                    vid: this.vid,
                    nid: this.nid,
                    data: this.data
                }
            }
            // TODO: create, preload, reset, load, error, resize, unload
        };

        // attach instance methods to the view prototype
        $.extend(View.prototype, viewInstanceMethods);

        // ..........................................................
        // Exported API

        publicApi = {
            printTime: function () {
                console.log("the time is " + new Date());
            },

            addView: function (vid, nid, cb, data) {
                var view = new View(vid, nid, cb, data),
                    token;
                if (view.ok) {
                    views[vid] = view;
                    token = view.token();
                } else {
                    token = { vid: view.vid, bad: true };
                }
                return token;
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

            $view = d3.select('#view');

            $(window).on('hashchange', hash);

            // Invoke hashchange callback to navigate to content
            // indicated by the window location hash.
            hash();

            // If there were any build errors, report them
            reportBuildErrors();
        }


        // export the api and build-UI function
        return {
            api: publicApi,
            buildUi: buildOnosUi
        };
    };

}(jQuery));