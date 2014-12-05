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
            navApi,
            libApi,
            exported = {};

        var defaultOptions = {
            trace: false,
            theme: 'dark',
            startVid: defaultVid
        };

        // compute runtime settings
        var settings = $.extend({}, defaultOptions, options);

        // set the selected theme
        d3.select('body').classed(settings.theme, true);

        // internal state
        var views = {},
            fpanels = {},
            current = {
                view: null,
                ctx: '',
                flags: {},
                theme: settings.theme
            },
            built = false,
            buildErrors = [],
            keyHandler = {
                globalKeys: {},
                maskedKeys: {},
                viewKeys: {},
                viewFn: null,
                viewGestures: []
            },
            alerts = {
                open: false,
                count: 0
            };

        // DOM elements etc.
        // TODO: verify existence of following elements...
        var $view = d3.select('#view'),
            $floatPanels = d3.select('#floatPanels'),
            $alerts = d3.select('#alerts'),
            // note, following elements added programmatically...
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
                case 187: return 'equals';
                case 189: return 'dash';
                case 191: return 'slash';
                case 192: return 'backQuote';
                case 220: return 'backSlash';
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
            console.error(msg);
            doAlert(msg);
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
                doError('Unable to parse target hash: "' + hash + '"');
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
            // "vid,ctx?flag1,flag2" --> { vid:vid, ctx:ctx, flags:{...} }
            traceFn('parseHash', s);

            // look for use of flags, first
            var vidctx,
                vid,
                ctx,
                flags,
                flagMap,
                m;

            // RE that includes flags ('?flag1,flag2')
            m = /^[#]{0,1}(.+)\?(.+)$/.exec(s);
            if (m) {
                vidctx = m[1];
                flags = m[2];
                flagMap = {};
            } else {
                // no flags
                m = /^[#]{0,1}((.+)(,.+)*)$/.exec(s);
                if (m) {
                    vidctx = m[1];
                } else {
                    // bad hash
                    return null;
                }
            }

            vidctx = vidctx.split(',');
            vid = vidctx[0];
            ctx = vidctx[1];
            if (flags) {
                flags.split(',').forEach(function (f) {
                    flagMap[f.trim()] = true;
                });
            }

            return {
                vid: vid.trim(),
                ctx: ctx ? ctx.trim() : '',
                flags: flagMap
            };

        }

        function makeHash(t, ctx, flags) {
            traceFn('makeHash');
            // make a hash string from the given navigation coordinates,
            // and optional flags map.
            // if t is not an object, then it is a vid
            var h = t,
                c = ctx || '',
                f = $.isPlainObject(flags) ? flags : null;

            if ($.isPlainObject(t)) {
                h = t.vid;
                c = t.ctx || '';
                f = t.flags || null;
            }

            if (c) {
                h += ',' + c;
            }
            if (f) {
                h += '?' + d3.map(f).keys().join(',');
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

        function buildError(msg) {
            buildErrors.push(msg);
        }

        function reportBuildErrors() {
            traceFn('reportBuildErrors');
            var nerr = buildErrors.length,
                errmsg;
            if (!nerr) {
                console.log('(no build errors)');
            } else {
                errmsg = 'Build errors: ' + nerr + ' found...\n\n' +
                    buildErrors.join('\n');
                doAlert(errmsg);
                console.error(errmsg);
            }
        }

        // returns the reference if it is a function, null otherwise
        function isF(f) {
            return $.isFunction(f) ? f : null;
        }

        // returns the reference if it is an array, null otherwise
        function isA(a) {
            return $.isArray(a) ? a : null;
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

            // first, we'll start by closing the alerts pane, if open
            closeAlerts();

            // if there is a current view, and it is not the same as
            // the incoming view, then unload it...
            if (current.view && (current.view.vid !== view.vid)) {
                current.view.unload();

                // detach radio buttons, key handlers, etc.
                $('#mastRadio').children().detach();
                keyHandler.viewKeys = {};
                keyHandler.viewFn = null;
            }

            // cache new view and context
            current.view = view;
            current.ctx = t.ctx || '';
            current.flags = t.flags || {};

            // init is called only once, after the view is in the DOM
            if (!view.inited) {
                view.init(current.ctx, current.flags);
                view.inited = true;
            }

            // clear the view of stale data
            view.reset();

            // load the view
            view.load(current.ctx, current.flags);
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
                btnG,
                api = {};

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

                    btn.id = bid;
                    btnG.buttonDef[uid] = btn;

                    if (i === 0) {
                        button.classed('active', true);
                        btnG.selected = bid;
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
                            btnG.selected = btn.id;
                            if (isF(btn.cb)) {
                                btn.cb(view.token(), btn);
                            }
                        }
                    });

                view.radioButtons = btnG;

                api.selected = function () {
                    return btnG.selected;
                }
            }

            // attach the buttons to the masthead
            $mastRadio.node().appendChild(btnG.node());
            // return an api for interacting with the button set
            return api;
        }

        function setupGlobalKeys() {
            $.extend(keyHandler, {
                globalKeys: {
                    backSlash: [quickHelp, 'Show / hide Quick Help'],
                    slash: [quickHelp, 'Show / hide Quick Help'],
                    esc: [escapeKey, 'Dismiss dialog or cancel selections'],
                    T: [toggleTheme, "Toggle theme"]
                },
                globalFormat: ['backSlash', 'slash', 'esc', 'T'],

                // Masked keys are global key handlers that always return true.
                // That is, the view will never see the event for that key.
                maskedKeys: {
                    slash: true,
                    backSlash: true,
                    T: true
                }
            });
        }

        function quickHelp(view, key, code, ev) {
            libApi.quickHelp.show(keyHandler);
            return true;
        }

        function escapeKey(view, key, code, ev) {
            if (alerts.open) {
                closeAlerts();
                return true;
            }
            if (libApi.quickHelp.hide()) {
                return true;
            }
            return false;
        }

        function toggleTheme(view, key, code, ev) {
            var body = d3.select('body');
            current.theme = (current.theme === 'light') ? 'dark' : 'light';
            body.classed('light dark', false);
            body.classed(current.theme, true);
            theme(view);
            return true;
        }

        function setGestureNotes(g) {
            keyHandler.viewGestures = isA(g) || [];
        }

        function setKeyBindings(keyArg) {
            var viewKeys,
                masked = [];

            if ($.isFunction(keyArg)) {
                // set general key handler callback
                keyHandler.viewFn = keyArg;
            } else {
                // set specific key filter map
                viewKeys = d3.map(keyArg).keys();
                viewKeys.forEach(function (key) {
                    if (keyHandler.maskedKeys[key]) {
                        masked.push('  Key "' + key + '" is reserved');
                    }
                });

                if (masked.length) {
                    doAlert('WARNING...\n\nsetKeys():\n' + masked.join('\n'));
                }
                keyHandler.viewKeys = keyArg;
            }
        }

        function keyIn() {
            var event = d3.event,
                keyCode = event.keyCode,
                key = whatKey(keyCode),
                kh = keyHandler,
                gk = kh.globalKeys[key],
                gcb = isF(gk) || (isA(gk) && isF(gk[0])),
                vk = kh.viewKeys[key],
                vcb = isF(vk) || (isA(vk) && isF(vk[0])) || isF(kh.viewFn),
                token = current.view.token();

            // global callback?
            if (gcb && gcb(token, key, keyCode, event)) {
                // if the event was 'handled', we are done
                return;
            }
            // otherwise, let the view callback have a shot
            if (vcb) {
                vcb(token, key, keyCode, event);
            }
        }

        function createAlerts() {
            $alerts.style('display', 'block');
            $alerts.append('span')
                .attr('class', 'close')
                .text('X')
                .on('click', closeAlerts);
            $alerts.append('pre');
            $alerts.append('p').attr('class', 'footnote')
                .text('Press ESCAPE to close');
            alerts.open = true;
            alerts.count = 0;
        }

        function closeAlerts() {
            $alerts.style('display', 'none')
                .html('');
            alerts.open = false;
        }

        function addAlert(msg) {
            var lines,
                oldContent;

            if (alerts.count) {
                oldContent = $alerts.select('pre').html();
            }

            lines = msg.split('\n');
            lines[0] += '  '; // spacing so we don't crowd 'X'
            lines = lines.join('\n');

            if (oldContent) {
                lines += '\n----\n' + oldContent;
            }

            $alerts.select('pre').html(lines);
            alerts.count++;
        }

        function doAlert(msg) {
            if (!alerts.open) {
                createAlerts();
            }
            addAlert(msg);
        }

        function resize(e) {
            d3.selectAll('.onosView').call(setViewDimensions);
            // allow current view to react to resize event...
            if (current.view) {
                current.view.resize(current.ctx, current.flags);
            }
        }

        function theme() {
            // allow current view to react to theme event...
            if (current.view) {
                current.view.theme(current.ctx, current.flags);
            }
        }

        // ..........................................................
        // View class
        //   Captures state information about a view.

        // Constructor
        //      vid : view id
        //      nid : id of associated nav-item (optional)
        //      cb  : callbacks (init, reset, load, unload, resize, theme, error)
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
                    setGestures: this.setGestures,
                    dataLoadError: this.dataLoadError,
                    alert: this.alert,
                    flash: this.flash,
                    getTheme: this.getTheme
                }
            },

            // == Life-cycle functions
            // TODO: factor common code out of life-cycle
            init: function (ctx, flags) {
                var c = ctx || '',
                    fn = isF(this.cb.init);
                traceFn('View.init', this.vid + ', ' + c);
                if (fn) {
                    trace('INIT cb for ' + this.vid);
                    fn(this.token(), c, flags);
                }
            },

            reset: function (ctx, flags) {
                var c = ctx || '',
                    fn = isF(this.cb.reset);
                traceFn('View.reset', this.vid);
                if (fn) {
                    trace('RESET cb for ' + this.vid);
                    fn(this.token(), c, flags);
                } else if (this.cb.reset === true) {
                    // boolean true signifies "clear view"
                    trace('  [true] cleaing view...');
                    viewApi.empty();
                }
            },

            load: function (ctx, flags) {
                var c = ctx || '',
                    fn = isF(this.cb.load);
                traceFn('View.load', this.vid + ', ' + c);
                this.$div.classed('currentView', true);
                if (fn) {
                    trace('LOAD cb for ' + this.vid);
                    fn(this.token(), c, flags);
                }
            },

            unload: function (ctx, flags) {
                var c = ctx | '',
                    fn = isF(this.cb.unload);
                traceFn('View.unload', this.vid);
                this.$div.classed('currentView', false);
                if (fn) {
                    trace('UNLOAD cb for ' + this.vid);
                    fn(this.token(), c, flags);
                }
            },

            resize: function (ctx, flags) {
                var c = ctx || '',
                    fn = isF(this.cb.resize),
                    w = this.width(),
                    h = this.height();
                traceFn('View.resize', this.vid + '/' + c +
                        ' [' + w + 'x' + h + ']');
                if (fn) {
                    trace('RESIZE cb for ' + this.vid);
                    fn(this.token(), c, flags);
                }
            },

            theme: function (ctx, flags) {
                var c = ctx | '',
                    fn = isF(this.cb.theme);
                traceFn('View.theme', this.vid);
                if (fn) {
                    trace('THEME cb for ' + this.vid);
                    fn(this.token(), c, flags);
                }
            },

            error: function (ctx, flags) {
                var c = ctx || '',
                    fn = isF(this.cb.error);
                traceFn('View.error', this.vid + ', ' + c);
                if (fn) {
                    trace('ERROR cb for ' + this.vid);
                    fn(this.token(), c, flags);
                }
            },

            // == Token API functions
            width: function () {
                return $(this.$div.node()).width();
            },

            height: function () {
                return $(this.$div.node()).height();
            },

            setRadio: function (btnSet) {
                return setRadioButtons(this.vid, btnSet);
            },

            setKeys: function (keyArg) {
                setKeyBindings(keyArg);
            },

            setGestures: function (g) {
                setGestureNotes(g);
            },

            getTheme: function () {
                return current.theme;
            },

            uid: function (id) {
                return makeUid(this, id);
            },

            // TODO : add exportApi and importApi methods
            // TODO : implement custom dialogs

            // Consider enhancing alert mechanism to handle multiples
            // as individually closable.
            alert: function (msg) {
                doAlert(msg);
            },

            flash: function (msg) {
                libApi.feedback.flash(msg);
            },

            dataLoadError: function (err, url) {
                var msg = 'Data Load Error\n\n' +
                    err.status + ' -- ' + err.statusText + '\n\n' +
                    'relative-url: "' + url + '"\n\n' +
                    'complete-url: "' + err.responseURL + '"';
                this.alert(msg);
            }

            // TODO: consider schedule, clearTimer, etc.
        };

        // attach instance methods to the view prototype
        $.extend(View.prototype, viewInstanceMethods);

        // ..........................................................
        // UI API

        var fpConfig = {
            TR: {
                side: 'right'
            },
            TL: {
                side: 'left'
            }
        };

        uiApi = {
            addLib: function (libName, api) {
                // TODO: validation of args
                libApi[libName] = api;
            },

            // TODO: implement floating panel as a class
            // TODO: parameterize position (currently hard-coded to TopRight)
            /*
             * Creates div in floating panels block, with the given id.
             * Returns panel token used to interact with the panel
             */
            addFloatingPanel: function (id, position) {
                var pos = position || 'TR',
                    cfg = fpConfig[pos],
                    el,
                    fp,
                    on = false;

                if (fpanels[id]) {
                    buildError('Float panel with id "' + id + '" already exists.');
                    return null;
                }

                el = $floatPanels.append('div')
                    .attr('id', id)
                    .attr('class', 'fpanel')
                    .style('opacity', 0);

                // has to be called after el is set.
                el.style(cfg.side, pxHide());

                function pxShow() {
                    return '20px';
                }
                function pxHide() {
                    return (-20 - widthVal()) + 'px';
                }
                function noPx(what) {
                    return el.style(what).replace(/px$/, '');
                }
                function widthVal() {
                    return noPx('width');
                }
                function heightVal() {
                    return noPx('height');
                }

                function noop() {}

                fp = {
                    id: id,
                    el: el,
                    pos: pos,
                    isVisible: function () {
                        return on;
                    },

                    show: function (cb) {
                        var endCb = isF(cb) || noop;
                        on = true;
                        el.transition().duration(750)
                            .each('end', endCb)
                            .style(cfg.side, pxShow())
                            .style('opacity', 1);
                    },
                    hide: function (cb) {
                        var endCb = isF(cb) || noop;
                        on = false;
                        el.transition().duration(750)
                            .each('end', endCb)
                            .style(cfg.side, pxHide())
                            .style('opacity', 0);
                    },
                    empty: function () {
                        return el.html('');
                    },
                    append: function (what) {
                        return el.append(what);
                    },
                    width: function (w) {
                        if (w === undefined) {
                            return widthVal();
                        }
                        el.style('width', w + 'px');
                    },
                    height: function (h) {
                        if (h === undefined) {
                            return heightVal();
                        }
                        el.style('height', h + 'px');
                    }
                };
                fpanels[id] = fp;
                return fp;
            },

            // TODO: it remains to be seen whether we keep this style of docs
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
             * "init", "reset", "load", "unload", "resize", "theme", "error".
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

        // TODO: deprecated
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
        // Library API
        libApi = {

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

            $mastRadio = d3.select('#mastRadio');

            $(window).on('hashchange', hash);
            $(window).on('resize', resize);

            d3.select('body').on('keydown', keyIn);
            setupGlobalKeys();

            // Invoke hashchange callback to navigate to content
            // indicated by the window location hash.
            hash();

            // If there were any build errors, report them
            reportBuildErrors();
        }

        // export the api and build-UI function
        return {
            ui: uiApi,
            lib: libApi,
            //view: viewApi,
            nav: navApi,
            buildUi: buildOnosUi,
            exported: exported
        };
    };

}(jQuery));
