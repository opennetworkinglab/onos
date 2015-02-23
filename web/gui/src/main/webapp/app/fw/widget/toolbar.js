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

    var $log, fs, ps, bns;

    var ids = [],
        tbarId,
        tbarPanel,
        tbarDiv,
        tbarArrowDiv;

    function init() {
        ids = [];
    }

    function validId(id, caller) {
        if (fs.inArray(id, ids) !== -1) {
            $log.warn(caller + ': ID already exists');
            return false;
        }
        return true;
    }

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
        tbarArrowDiv = tbarDiv.append('div')
            .classed('sep', true)
            .style('width', '2px');
    }

    function createToolbar(id, settings) {
        if (!id) {
            $log.warn('createToolbar: no ID given');
            return null;
        }
        tbarId = 'tbar-' + id;
        var opts = fs.isO(settings) || {}; // default settings should be put here

        if (!validId(tbarId, 'createToolbar')) { return null; }
        ids.push(tbarId);

        tbarPanel = ps.createPanel(tbarId, opts);
        tbarDiv = tbarPanel.classed('toolbar', true);

        return {
            addButton: addButton,
            addToggle: addToggle,
            addRadioSet: addRadioSet,
            addSeparator: addSeparator
        }
    }

    //function currently not working
    function destroyToolbar(id) {
        //ps.destroyPanel(id);
    }

    angular.module('onosWidget')
        .factory('ToolbarService', ['$log', 'FnService',
            'PanelService', 'ButtonService',
            function (_$log_, _fs_, _ps_, _bns_) {
                $log = _$log_;
                fs = _fs_;
                ps = _ps_;
                bns = _bns_;

                return {
                    init: init,
                    createToolbar: createToolbar,
                    destroyToolbar: destroyToolbar
                };
            }]);
}());
