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
 Sample view to illustrate hash formats.

 @author Simon Hunt
 */

(function (onos) {
    'use strict';

    var intro = "Try using the following hashes in the address bar:",
        hashPrefix = '#sampleHash',
        suffixes = [
            '',
            ',one',
            ',two',
            ',context,ignored',
            ',context,ignored?a,b,c',
            ',two?foo',
            ',three?foo,bar'
        ],
        $d;

    function note(txt) {
        $d.append('p')
            .text(txt)
            .style({
                'font-size': '10pt',
                color: 'darkorange',
                padding: '0 20px',
                margin: 0
            });
    }

    function para(txt, color) {
        var c = color || 'black';
        $d.append('p')
            .text(txt)
            .style({
                padding: '2px 8px',
                color: c
            });
    }

    function load(view, ctx, flags) {
        var c = ctx || '(undefined)',
            f = flags ? d3.map(flags).keys() : [];

        $d = view.$div;

        para(intro);

        suffixes.forEach(function (s) {
            note(hashPrefix + s);
        });

        para('View ID: ' + view.vid, 'blue');
        para('Context: ' + c, 'blue');
        para('Flags: { ' + f.join(', ') + ' }', 'magenta');
    }

    // == register the view here, with links to lifecycle callbacks

    onos.ui.addView('sampleHash', {
        reset: true,    // empty the div on reset
        load: load
    });

}(ONOS));
