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
 Sample view to illustrate key bindings.

 @author Simon Hunt
 */

(function (onos) {
    'use strict';

    var keyDispatch = {
        Z: keyUndo,
        X: keyCut,
        C: keyCopy,
        V: keyPaste,
        space: keySpace
    };

    function keyUndo(view) {
        note(view, 'Z = UNDO');
    }

    function keyCut(view) {
        note(view, 'X = CUT');
    }

    function keyCopy(view) {
        note(view, 'C = COPY');
    }

    function keyPaste(view) {
        note(view, 'V = PASTE');
    }

    function keySpace(view) {
        note(view, 'The SpaceBar');
    }

    function note(view, msg) {
        view.$div.append('p')
            .text(msg)
            .style({
                'font-size': '10pt',
                color: 'darkorange',
                padding: '0 20px',
                margin: 0
            });
    }

    function keyCallback(view, key, keyCode, event) {
        note(view, 'Key = ' + key + ' KeyCode = ' + keyCode);
    }

    function load(view, ctx) {
        // this maps specific keys to specific functions (1)
        view.setKeys(keyDispatch);
        // whereas, this installs a general key handler function (2)
        view.setKeys(keyCallback);

        // Note that (1) takes precedence over (2)

        view.$div.append('p')
            .text('Press a key or two (try Z,X,C,V and others) ...')
            .style('padding', '2px 8px');
    }

    // == register the view here, with links to lifecycle callbacks

    onos.ui.addView('sampleKeys', {
        reset: true,    // empty the div on reset
        load: load
    });

}(ONOS));
