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
 Sample view to illustrate radio buttons.

 @author Simon Hunt
 */

(function (onos) {
    'use strict';

    var intro = [ 'Yo, radio button set...', 'Time to shine' ],
        btnSet = [
            { text: 'First Button', cb: cbRadio },
            { text: 'Second Button', cb: cbRadio },
            { text: 'Third Button', cb: cbRadio }
        ];

    // radio button callback
    function cbRadio(view, btn) {
        write(view, 'You pressed the ' + btn.text);
    }

    function write(view, msg) {
        view.$div.append('p')
            .text(msg)
            .style({
                'font-size': '10pt',
                color: 'green',
                padding: '0 20px',
                margin: '2px'
            });
    }

    // invoked when the view is loaded
    function load(view, ctx) {
        view.setRadio(btnSet);

        view.$div.selectAll('p')
            .data(intro)
            .enter()
            .append('p')
            .text(function (d) { return d; })
            .style('padding', '2px 8px');
    }

    // == register the view here, with links to lifecycle callbacks

    onos.ui.addView('sampleRadio', {
        reset: true,    // empty the div on reset
        load: load
    });

}(ONOS));
