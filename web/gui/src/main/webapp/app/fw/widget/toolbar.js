/*
 * Copyright 2015 Open Networking Laboratory
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
 ONOS GUI -- Widget -- Toolbar Service
 */
(function () {
    'use strict';

    var $log, fs, ps, bns, is;

    var ids = [],
        defaultSettings = {
            edge: 'left',
            width: 400
        },
        settings,
        arrowSize = 10,
        tbarId,
        tbarPanel,
        tbarDiv,
        tbarArrowDiv;

    // this function is only used in testing
    function init() {
        ids = [];
    }

    // === Helper functions --------------------------------------

    function validId(id, caller) {
        if (fs.inArray(id, ids) !== -1) {
            $log.warn(caller + ': ID already exists');
            return false;
        }
        return true;
    }

    // translate(0 50) looks good with arrowSize of 10
    function rotateArrowLeft() {
        tbarArrowDiv.select('g')
            .attr('transform', 'translate(0 50) rotate(-90)');
    }

    function rotateArrowRight() {
        tbarArrowDiv.select('g')
            .attr('transform', 'translate(0 50) rotate(90)');
    }

    function createArrow() {
        tbarArrowDiv = tbarDiv.append('div')
            .classed('tbarArrow', true)
            .style({'position': 'absolute',
                'top': '50%',
                'left': '98%',
                'margin-right': '-2%',
                'transform': 'translate(-50%, -50%)',
                'cursor': 'pointer'});
        is.loadEmbeddedIcon(tbarArrowDiv, 'tableColSortAsc', arrowSize);
        rotateArrowLeft();

        tbarArrowDiv.on('click', toggleTools);
    }

    // === Adding to toolbar functions ----------------------------

    function addButton(id, gid, cb, tooltip) {
        var btnId = tbarId + '-' + id;
        if (!validId(btnId, 'addButton')) { return null; }
        ids.push(btnId);
        return bns.button(tbarDiv, btnId, gid, cb, tooltip);
    }

    function addToggle(id, gid, initState, cb, tooltip) {
        var togId = tbarId + '-' + id;
        if (!validId(togId, 'addToggle')) { return null; }
        ids.push(togId);
        return bns.toggle(tbarDiv, togId, gid, initState, cb, tooltip);
    }

    function addRadioSet(id, rset) {
        var radId = tbarId + '-' + id;
        if (!validId(radId, 'addRadioSet')) { return null; }
        ids.push(radId);
        return bns.radioSet(tbarDiv, radId, rset);
    }

    function addSeparator() {
        if (!tbarDiv) {
            $log.warn('Separator cannot append to div');
            return null;
        }
        return tbarDiv.append('div')
            .classed('sep', true)
            .style('width', '2px');
    }

    // === Main toolbar API functions ----------------------------

    function createToolbar(id, opts) {
        if (!id) {
            $log.warn('createToolbar: no ID given');
            return null;
        }
        tbarId = 'tbar-' + id;
        settings = angular.extend({}, defaultSettings, fs.isO(opts));

        if (!validId(tbarId, 'createToolbar')) { return null; }
        ids.push(tbarId);

        tbarPanel = ps.createPanel(tbarId, settings);
        tbarDiv = tbarPanel.classed('toolbar', true)
            .style('position', 'relative');

        createArrow();

        return {
            addButton: addButton,
            addToggle: addToggle,
            addRadioSet: addRadioSet,
            addSeparator: addSeparator,

            show: show,
            hide: hide,
            toggleTools: toggleTools
        }
    }

    function destroyToolbar(id) {
        ps.destroyPanel(id);
        tbarDiv = null;
    }

    function show(cb) {
        tbarPanel.show(cb);
        rotateArrowLeft();
    }

    function hide(cb) {
        tbarPanel.hide(cb);
        //tbarPanel.style(opts.edge, (arrowSize + 4 + 'px'));
        rotateArrowRight();
    }

    function toggleTools(cb) {
        if (tbarPanel.isVisible()) { hide(cb); }
        else { show(cb) }
    }

    angular.module('onosWidget')
        .factory('ToolbarService', ['$log', 'FnService',
            'PanelService', 'ButtonService', 'IconService',
            function (_$log_, _fs_, _ps_, _bns_, _is_) {
                $log = _$log_;
                fs = _fs_;
                ps = _ps_;
                bns = _bns_;
                is = _is_;

                return {
                    init: init,
                    createToolbar: createToolbar,
                    destroyToolbar: destroyToolbar
                };
            }]);
}());
