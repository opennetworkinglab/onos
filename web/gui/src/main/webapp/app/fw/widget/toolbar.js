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

    var toolBtnIds = {},
        toolbarPanel,
        toolbarDiv;

    var ids = [],
        tbarId,
        tbarPanel,
        tbarDiv;

    function init() {
        toolBtnIds = {};
        ids = [];
    }

    function addButton(btn) {
        // pass in button object and pass separate the pieces in this function,
        // to be passed to ButtonService
        var btndiv = toolbarDiv.append('div').attr('class', 'btn');
        // use ButtonService to create btn with other arguments and btndiv
    }

    function addToggle(tog) {
        var togdiv = toolbarDiv.append('div').attr('class', 'tog');
        // use ButtonService to create btn with other arguments and togdiv
    }

    function addRadio(rad) {
        var raddiv = toolbarDiv.append('div').attr('class', 'rad');
        // use ButtonService to create btn with other arguments and raddiv
    }

    function addSeparator() {
        toolbarDiv.append('div')
            .attr('class', 'sep')
            .style({'width': '2px',
                    'border-width': '1px',
                    'border-style': 'solid'});
    }

    function makeButton(id, gid, cb) {
        return {
            t: 'btn',
            id: id,
            gid: gid,
            cb: cb
        };
    }

    function makeToggle(id, gid, cb) {
        return {
            t: 'tog',
            id: id,
            gid: gid,
            cb: cb
        };
    }

    function makeRadio(id, gid, cb) {
        (id in toolBtnIds) ? toolBtnIds[id] += 1 : toolBtnIds[id] = 0;
        return {
            t: 'rad',
            id: id + '-' + toolBtnIds[id],
            rid: toolBtnIds[id] + '',
            gid: gid,
            cb: cb
        };
    }

    function separator() {
        return {
            t: 'sep'
        };
    }

    function validId(id, caller) {
        if (fs.inArray(id, ids) !== -1) {
            $log.warn(caller + ': ID already exists');
            return false;
        }
        return true;
    }

    function addButton1(id, gid, cb, tooltip) {
        var btnId = tbarId + '-' + id;
        if (!validId(btnId, 'addButton')) {
            return null;
        }
        ids.push(btnId);
        return bns.button(tbarDiv, btnId, gid, cb, tooltip);
    }

    function addToggle1(id, gid, initState, cb, tooltip) {
        var togId = tbarId + '-' + id;
        if (!validId(togId, 'addToggle')) {
            return null;
        }
        ids.push(togId);
        return bns.toggle(tbarDiv, togId, gid, initState, cb, tooltip);
    }

    function addRadioSet(id, rset) {
        var radId = tbarId + '-' + id;
        if (!validId(radId, 'addRadioSet')) {
            return null;
        }
        ids.push(radId);
        return bns.radioSet(tbarDiv, radId, rset);
    }

    // TODO: finish this and remove unneeded code
    function addSeparator1() {

    }

    function createToolbar1(id, settings) {
        if (!id) {
            $log.warn('createToolbar: no ID given');
            return null;
        }
        tbarId = 'tbar-' + id;
        var opts = fs.isO(settings) || {}; // default settings should be put here

        if (!validId(tbarId, 'createToolbar')) {
            return null;
        }
        ids.push(tbarId);

        tbarPanel = ps.createPanel(tbarId, opts);
        tbarDiv = tbarPanel.classed('toolbar', true);

        // TODO: change names of functions
        return {
            addButton1: addButton1,
            addToggle1: addToggle1,
            addRadioSet: addRadioSet,
            addSeparator1: addSeparator1
        }
    }

    // function currently not working
    function destroyToolbar(id) {
        ps.destroyPanel(id);
    }

    function createToolbar(tbarId, tools) {
        var api;

        if (!tbarId) {
            $log.warn('createToolbar: no ID given');
            return null;
        }
        if (!tools || tools.constructor !== Array || !tools.length) {
            $log.warn('createToolbar: no tools given');
            return null;
        }

        for (var i = 0; i < tools.length; i += 1) {
            if (tools[i].t === 'rad' || tools[i].t === 'sep') {
                continue;
            } else {
                if (tools[i].id in toolBtnIds) {
                    $log.warn('createToolbar: item with id '
                            + tools[i].id + ' already exists');
                    return null;
                }
                toolBtnIds[tools[i].id] = 1;
            }
        }

        // need to pass in an object with settings for where the toolbar will go
        toolbarPanel = ps.createPanel(tbarId, {});
        toolbarPanel.classed('toolbar', true);
        toolbarDiv = toolbarPanel.el();

        tools.forEach(function (tool) {
            switch (tool['t']) {
                case 'btn': addButton(tool); break;
                case 'tog': addToggle(tool); break;
                case 'rad': addRadio(tool); break;
                default: addSeparator(); break;
            }
        });

        // api functions to be returned defined here

        function size(arr) {
            return arr.length;
        }

        api = {
            size: size
        };

        return api;
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
                    makeButton: makeButton,
                    makeToggle: makeToggle,
                    makeRadio: makeRadio,
                    separator: separator,
                    createToolbar: createToolbar,
                    createToolbar1: createToolbar1,
                    destroyToolbar: destroyToolbar
                };
            }]);

}());