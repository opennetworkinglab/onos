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
        mastHeight = 36,                    // see mast2.css
        defaultVid = 'sample';


    // attach our main function to the jQuery object
    $.onos = function (options) {
        var uiApi,
            viewApi,
            navApi;

        var defaultOptions = {
            trace: false,
            startVid: defaultVid
        };

        // compute runtime settings
        var settings = $.extend({}, defaultOptions, options);

        // internal state
        var views = {},
            current = {
                view: null,
                ctx: ''
            },
            built = false,
            errorCount = 0,
            keyHandler = {
                fn: null,
                map: {}
            };

        // DOM elements etc.
        var $view,
            $mastRadio;


        function whatKey(code) {
            switch (code) {
                case 13: return 'enter';
                case 16: return 'shift';
                case 17: return 'ctrl';
                case 18: return 'alt';
                case 27: return 'esc';
                case 32: return 'space';
                case 37: return 'leftArrow';
                case 38: return 'upArrow';
                case 39: return 'rightArrow';
                case 40: return 'downArrow';
                case 91: return 'cmdLeft';
                case 93: return 'cmdRight';
                default:
                    if ((code >= 48 && code <= 57) ||
                        (code >= 65 && code <= 90)) {
                        return String.fromCharCode(code);
                    } else if (code >= 112 && code <= 123) {
                        return 'F' + (code - 111);
                    }
                    return '.';
            }
        }


        // ..........................................................
        // Internal functions

        // throw an error
        function throwError(msg) {
            // separate function, as we might add tracing here too, later
            throw new Error(msg);
        }

        function doError(msg) {
            errorCount++;
            console.error(msg);
        }

        function trace(msg) {
            if (settings.trace) {
                console.log(msg);
            }
        }

        function traceFn(fn, params) {
            if (settings.trace) {
                console.log('*FN* ' + fn + '(...): ' + params);
            }
        }

        // hash navigation
        function hash() {
            var hash = window.location.hash,
                redo = false,
                view,
                t;

            traceFn('hash', hash);

            if (!hash) {
                hash = settings.startVid;
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
            traceFn('parseHash', s);

            var m = /^[#]{0,1}(\S+),(\S*)$/.exec(s);
            if (m) {
                return { vid: m[1], ctx: m[2] };
            }

            m = /^[#]{0,1}(\S+)$/.exec(s);
            return m ? { vid: m[1] } : null;
        }

        function makeHash(t, ctx) {
            traceFn('makeHash');
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
            trace('hash = "' + h + '"');
            return h;
        }

        function navigate(hash, view, t) {
            traceFn('navigate', view.vid);
            // closePanes()     // flyouts etc.
            // updateNav()      // accordion / selected nav item etc.
            createView(view);
            setView(view, hash, t);
        }

        function reportBuildErrors() {
            traceFn('reportBuildErrors');
            // TODO: validate registered views / nav-item linkage etc.
            console.log('(no build errors)');
        }

        // returns the reference if it is a function, null otherwise
        function isF(f) {
            return $.isFunction(f) ? f : null;
        }

        // ..........................................................
        // View life-cycle functions

        function setViewDimensions(sel) {
            var w = window.innerWidth,
                h = window.innerHeight - mastHeight;
            sel.each(function () {
                $(this)
                    .css('width', w + 'px')
                    .css('height', h + 'px')
            });
        }

        function createView(view) {
            var $d;

            // lazy initialization of the view
            if (view && !view.$div) {
                trace('creating view for ' + view.vid);
                $d = $view.append('div')
                        .attr({
                            id: view.vid,
                            class: 'onosView'
                         });
                setViewDimensions($d);
                view.$div = $d;   // cache a reference to the D3 selection
            }
        }

        function setView(view, hash, t) {
            traceFn('setView', view.vid);
            // set the specified view as current, while invoking the
            // appropriate life-cycle callbacks

            // if there is a current view, and it is not the same as
            // the incoming view, then unload it...
            if (current.view && (current.view.vid !== view.vid)) {
                current.view.unload();

                // detach radio buttons, key handlers, etc.
                $('#mastRadio').children().detach();
                keyHandler.fn = null;
                keyHandler.map = {};
            }

            // cache new view and context
            current.view = view;
            current.ctx = t.ctx || '';

            // preload is called only once, after the view is in the DOM
            if (!view.preloaded) {
                view.preload(current.ctx);
                view.preloaded = true;
            }

            // clear the view of stale data
            view.reset();

            // load the view
            view.load(current.ctx);
        }

        // generate 'unique' id by prefixing view id
        function makeUid(view, id) {
            return view.vid + '-' + id;
        }

        // restore id by removing view id prefix
        function unmakeUid(view, uid) {
            var re = new RegExp('^' + view.vid + '-');
            return uid.replace(re, '');
        }

        function setRadioButtons(vid, btnSet) {
            var view = views[vid],
                btnG;

            // lazily create the buttons...
            if (!(btnG = view.radioButtons)) {
                btnG = d3.select(document.createElement('div'));
                btnG.buttonDef = {};

                btnSet.forEach(function (btn, i) {
                    var bid = btn.id || 'b' + i,
                        txt = btn.text || 'Button #' + i,
                        uid = makeUid(view, bid),
                        button = btnG.append('span')
                        .attr({
                            id: uid,
                            class: 'radio'
                        })
                        .text(txt);

                    btnG.buttonDef[uid] = btn;

                    if (i === 0) {
                        button.classed('active', true);
                    }
                });

                btnG.selectAll('span')
                    .on('click', function (d) {
                        var button = d3.select(this),
                            uid = button.attr('id'),
                            btn = btnG.buttonDef[uid],
                            act = button.classed('active');

                        if (!act) {
                            btnG.selectAll('span').classed('active', false);
                            button.classed('active', true);
                            if (isF(btn.cb)) {
                                btn.cb(view.token(), btn);
                            }
                        }
                    });

                view.radioButtons = btnG;
            }

            // attach the buttons to the masthead
            $mastRadio.node().appendChild(btnG.node());
        }

        function setKeyBindings(keyArg) {
            if ($.isFunction(keyArg)) {
                // set general key handler callback
                keyHandler.fn = keyArg;
            } else {
                // set specific key filter map
                keyHandler.map = keyArg;
            }
        }

        function keyIn() {
            var event = d3.event,
                keyCode = event.keyCode,
                key = whatKey(keyCode),
                cb = isF(keyHandler.map[key]) || isF(keyHandler.fn);

            if (cb) {
                cb(current.view.token(), key, keyCode, event);
            }
        }

        function resize(e) {
            d3.selectAll('.onosView').call(setViewDimensions);
            // allow current view to react to resize event...
            if (current.view) {
                current.view.resize(current.ctx);
            }
        }

        // ..........................................................
        // View class
        //   Captures state information about a view.

        // Constructor
        //      vid : view id
        //      nid : id of associated nav-item (optional)
        //      cb  : callbacks (preload, reset, load, unload, resize, error)
        function View(vid) {
            var av = 'addView(): ',
                args = Array.prototype.slice.call(arguments),
                nid,
                cb;

            args.shift();   // first arg is always vid
            if (typeof args[0] === 'string') {  // nid specified
                nid = args.shift();
            }
            cb = args.shift();

            this.vid = vid;

            if (validateViewArgs(vid)) {
                this.nid = nid;     // explicit navitem id (can be null)
                this.cb = $.isPlainObject(cb) ? cb : {};    // callbacks
                this.$div = null;               // view not yet added to DOM
                this.radioButtons = null;       // no radio buttons yet
                this.ok = true;                 // valid view
            }
        }

        function validateViewArgs(vid) {
            var av = "ui.addView(...): ",
                ok = false;
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
            token: function () {
                return {
                    // attributes
                    vid: this.vid,
                    nid: this.nid,
                    $div: this.$div,

                    // functions
                    width: this.width,
                    height: this.height,
                    uid: this.uid,
                    setRadio: this.setRadio,
                    setKeys: this.setKeys,
                    dataLoadError: this.dataLoadError
                }
            },

            preload: function (ctx) {
                var c = ctx || '',
                    fn = isF(this.cb.preload);
                traceFn('View.preload', this.vid + ', ' + c);
                if (fn) {
                    trace('PRELOAD cb for ' + this.vid);
                    fn(this.token(), c);
                }
            },

            reset: function () {
                var fn = isF(this.cb.reset);
                traceFn('View.reset', this.vid);
                if (fn) {
                    trace('RESET cb for ' + this.vid);
                    fn(this.token());
                } else if (this.cb.reset === true) {
                    // boolean true signifies "clear view"
                    trace('  [true] cleaing view...');
                    viewApi.empty();
                }
            },

            load: function (ctx) {
                var c = ctx || '',
                    fn = isF(this.cb.load);
                traceFn('View.load', this.vid + ', ' + c);
                this.$div.classed('currentView', true);
                // TODO: add radio button set, if needed
                if (fn) {
                    trace('LOAD cb for ' + this.vid);
                    fn(this.token(), c);
                }
            },

            unload: function () {
                var fn = isF(this.cb.unload);
                traceFn('View.unload', this.vid);
                this.$div.classed('currentView', false);
                // TODO: remove radio button set, if needed
                if (fn) {
                    trace('UNLOAD cb for ' + this.vid);
                    fn(this.token());
                }
            },

            resize: function (ctx) {
                var c = ctx || '',
                    fn = isF(this.cb.resize),
                    w = this.width(),
                    h = this.height();
                traceFn('View.resize', this.vid + '/' + c +
                        ' [' + w + 'x' + h + ']');
                if (fn) {
                    trace('RESIZE cb for ' + this.vid);
                    fn(this.token(), c);
                }
            },

            error: function (ctx) {
                var c = ctx || '',
                    fn = isF(this.cb.error);
                traceFn('View.error', this.vid + ', ' + c);
                if (fn) {
                    trace('ERROR cb for ' + this.vid);
                    fn(this.token(), c);
                }
            },

            width: function () {
                return $(this.$div.node()).width();
            },

            height: function () {
                return $(this.$div.node()).height();
            },

            setRadio: function (btnSet) {
                setRadioButtons(this.vid, btnSet);
            },

            setKeys: function (keyArg) {
                setKeyBindings(keyArg);
            },

            uid: function (id) {
                return makeUid(this, id);
            },

            // TODO : implement custom dialogs (don't use alerts)

            dataLoadError: function (err, url) {
                var msg = 'Data Load Error\n\n' +
                    err.status + ' -- ' + err.statusText + '\n\n' +
                    'relative-url: "' + url + '"\n\n' +
                    'complete-url: "' + err.responseURL + '"';
                alert(msg);
            }

            // TODO: consider schedule, clearTimer, etc.
        };

        // attach instance methods to the view prototype
        $.extend(View.prototype, viewInstanceMethods);

        // ..........................................................
        // UI API

        uiApi = {
            /** @api ui addView( vid, nid, cb )
             * Adds a view to the UI.
             * <p>
             * Views are loaded/unloaded into the view content pane at
             * appropriate times, by the navigation framework. This method
             * adds a view to the UI and returns a token object representing
             * the view. A view's token is always passed as the first
             * argument to each of the view's life-cycle callback functions.
             * <p>
             * Note that if the view is directly referenced by a nav-item,
             * or in a group of views with one of those views referenced by
             * a nav-item, then the <i>nid</i> argument can be omitted as
             * the framework can infer it.
             * <p>
             * <i>cb</i> is a plain object containing callback functions:
             * "preload", "reset", "load", "unload", "resize", "error".
             * <pre>
             *     function myLoad(view, ctx) { ... }
             *     ...
             *     // short form...
             *     onos.ui.addView('viewId', {
             *         load: myLoad
             *     });
             * </pre>
             *
             * @param vid (string) [*] view ID (a unique DOM element id)
             * @param nid (string) nav-item ID (a unique DOM element id)
             * @param cb (object) [*] callbacks object
             * @return the view token
             */
            addView: function (vid, nid, cb) {
                traceFn('addView', vid);
                var view = new View(vid, nid, cb),
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

        // ..........................................................
        // View API

        viewApi = {
            /** @api view empty( )
             * Empties the current view.
             * <p>
             * More specifically, removes all DOM elements from the
             * current view's display div.
             */
            empty: function () {
                if (!current.view) {
                    return;
                }
                current.view.$div.html('');
            }
        };

        // ..........................................................
        // Nav API
        navApi = {

        };

        // ..........................................................
        // Exported API

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
            $mastRadio = d3.select('#mastRadio');

            $(window).on('hashchange', hash);
            $(window).on('resize', resize);

            d3.select('body').on('keydown', keyIn);

            // Invoke hashchange callback to navigate to content
            // indicated by the window location hash.
            hash();

            // If there were any build errors, report them
            reportBuildErrors();
        }

        // export the api and build-UI function
        return {
            ui: uiApi,
            view: viewApi,
            nav: navApi,
            buildUi: buildOnosUi
        };
    };

}(jQuery));
