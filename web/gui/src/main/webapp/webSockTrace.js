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
 View that traces messages across the websocket.

 @author Simon Hunt
 */

(function (onos) {
    'use strict';

    var v,
        $d,
        tb,
        out,
        which = 'tx',
        keyDispatch = {
            space: function () {
                output(which, "Simon woz 'ere... " + which);
                which = (which === 'tx') ? 'rx' : 'tx';
            }
        };


    function addHeader() {
        tb = $d.append('div')
            .attr('class', 'toolbar');
        tb.append('span').text('Web Socket Trace');
    }

    function addOutput() {
        out = $d.append('div')
            .attr('class', 'output');
    }

    function subtitle(msg) {
        out.append('p').attr('class', 'subtitle').text(msg);
    }

    function output(rxtx, msg) {
        out.append('p').attr('class', rxtx).text(msg);
    }

    // invoked only the first time the view is loaded
    function preload(view, ctx, flags) {
        // NOTE: view.$div is a D3 selection of the view's div
        v = view;
        $d = v.$div;
        addHeader();
        addOutput();


        // hack for now, to allow topo access to our API
        // TODO: add 'exportApi' and 'importApi' to views.
        onos.exported.webSockTrace = {
            subtitle: subtitle,
            output: output
        };
    }

    // invoked just prior to loading the view
    function reset(view, ctx, flags) {

    }

    // invoked when the view is loaded
    function load(view, ctx, flags) {
        resize(view, ctx, flags);
        view.setKeys(keyDispatch);
        subtitle('Waiting for messages...');
    }

    // invoked when the view is resized
    function resize(view, ctx, flags) {
        var h = view.height();
        out.style('height', h + 'px');

    }

    // == register the view here, with links to lifecycle callbacks

    onos.ui.addView('webSockTrace', {
        preload: preload,
        load: load,
        resize: resize
    });

}(ONOS));
