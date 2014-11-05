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

    var data = [ 'Yo, radio button set...', 'Time to shine' ],
        btnSet = [
            { id: 'b1', text: 'First Button' },
            { id: 'b2', text: 'Second Button' },
            { id: 'b3', text: 'Third Button' }
        ],
        btnLookup = {};

    btnSet.forEach(function (b) {
        btnLookup[b.id] = b;
    });

    // invoked when the view is loaded
    function load(view, ctx) {
        view.setRadio(btnSet, doRadio);

        view.$div.selectAll('p')
            .data(data)
            .enter()
            .append('p')
            .text(function (d) { return d; })
            .style('padding', '2px 8px');
    }

    function doRadio(view, id) {
        view.$div.append('p')
            .text('You pressed the ' + btnLookup[id].text)
            .style({
                'font-size': '10pt',
                color: 'green',
                padding: '0 20px',
                margin: '2px'
            });
    }

    // == register the view here, with links to lifecycle callbacks

    onos.ui.addView('sampleRadio', {
        reset: true,    // empty the div on reset
        load: load
    });

}(ONOS));
