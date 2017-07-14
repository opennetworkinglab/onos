/*
 * Copyright 2015-present Open Networking Foundation
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
// TODO: Augment service to allow toolbars to exist on right edge of screen
// TODO: also - make toolbar more object aware (rows etc.)


(function () {
    'use strict';

    // injected refs
    var $log, fs, ps, bns, is;

    // configuration
    var arrowSize = 10,
        sepWidth = 6,
        defaultSettings = {
            edge: 'left',
            width: 20,
            margin: 0,
            hideMargin: -20,
            top: 'auto',
            bottom: '10px',
            fade: false,
            shown: false,
        };

    // internal state
    var tbars = {};


    // === Helper functions --------------------------------------

    // translate uses 50 because the svg viewbox is 50
    function rotateArrowLeft(adiv) {
        adiv.select('g')
            .attr('transform', 'translate(0 50) rotate(-90)');
    }
    function rotateArrowRight(adiv) {
        adiv.select('g')
            .attr('transform', 'translate(50 0) rotate(90)');
    }

    function createArrow(panel) {
        var arrowDiv = panel.append('div')
            .classed('tbar-arrow', true);
        is.loadIcon(arrowDiv, 'triangleUp', arrowSize, true);
        return arrowDiv;
    }

    function warn(msg, id) {
        $log.warn('createToolbar: ' + msg + ': [' + id + ']');
        return null;
    }

    // ==================================

    function createToolbar(id, opts) {
        if (!id) return warn('no ID given');
        if (tbars[id]) return warn('duplicate ID given', id);

        var settings = angular.extend({}, defaultSettings, fs.isO(opts)),
            items = {},
            tbid = 'toolbar-' + id,
            panel = ps.createPanel(tbid, settings),
            arrowDiv = createArrow(panel),
            currentRow = panel.append('div').classed('tbar-row', true),
            rowButtonIds = [], // for removable buttons
            tbWidth = arrowSize + 2, // empty toolbar width
            maxWidth = panel.width();

        arrowDiv.on('click', toggle);

        // add a descriptor for this toolbar
        tbars[id] = {
            settings: settings,
            items: items,
            panel: panel,
            panelId: tbid,
        };

        panel.classed('toolbar', true)
            .style('top', settings.top)
            .style('bottom', settings.bottom);

        // Helper functions

        function dupId(id, caller) {
            if (items[id]) {
                $log.warn(caller + ': duplicate ID:', id);
                return true;
            }
            return false;
        }

        function adjustWidth(btnWidth) {
            // 0.1 fudge for rounding error
            if (fs.noPxStyle(currentRow, 'width') + 0.1 >= maxWidth) {
                tbWidth += btnWidth;
                maxWidth = tbWidth;
            }
            panel.width(tbWidth);
        }

        // API functions

        function addButton(id, gid, cb, tooltip) {
            if (dupId(id, 'addButton')) return null;

            var bid = tbid + '-' + id,
                btn = bns.button(currentRow, bid, gid, cb, tooltip);

            items[id] = btn;
            adjustWidth(btn.width());
            return btn;
        }

        function addToggle(id, gid, initState, cb, tooltip) {
            if (dupId(id, 'addToggle')) return null;

            var tid = tbid + '-' + id,
                tog = bns.toggle(currentRow, tid, gid, initState, cb, tooltip);

            items[id] = tog;
            adjustWidth(tog.width());
            return tog;
        }

        function addRadioSet(id, rset) {
            if (dupId(id, 'addRadioSet')) return null;

            var rid = tbid + '-' + id,
                rad = bns.radioSet(currentRow, rid, rset);

            items[id] = rad;
            adjustWidth(rad.width());
            return rad;
        }

        function addSeparator() {
            currentRow.append('div')
                .classed('separator', true);
            tbWidth += sepWidth;
        }

        function addRow() {
            if (currentRow.select('div').empty()) {
                return null;
            } else {
                panel.append('br');
                currentRow = panel.append('div').classed('tbar-row', true);

                // return API to allow caller more access to the row
                return {
                    clear: rowClear,
                    setText: rowSetText,
                    addButton: rowAddButton,
                    classed: rowClassed,
                };
            }
        }

        function rowClear() {
            currentRow.selectAll('*').remove();
            rowButtonIds.forEach(function (bid) {
                delete items[bid];
            });
            rowButtonIds = [];
        }

        // installs a div with text into the button row
        function rowSetText(text) {
            rowClear();
            currentRow.append('div').classed('tbar-row-text', true)
                .text(text);
        }

        function rowAddButton(id, gid, cb, tooltip) {
            var b = addButton(id, gid, cb, tooltip);
            if (b) {
                rowButtonIds.push(id);
            }
        }

        function rowClassed(classes, bool) {
            currentRow.classed(classes, bool);
        }

        function show(cb) {
            rotateArrowLeft(arrowDiv);
            panel.show(cb);
        }

        function hide(cb) {
            rotateArrowRight(arrowDiv);
            panel.hide(cb);
        }

        function toggle(cb) {
            if (panel.isVisible()) {
                hide(cb);
            } else {
                show(cb);
            }
        }

        function isVisible() {
            return panel.isVisible();
        }

        return {
            addButton: addButton,
            addToggle: addToggle,
            addRadioSet: addRadioSet,
            addSeparator: addSeparator,
            addRow: addRow,

            show: show,
            hide: hide,
            toggle: toggle,
            isVisible: isVisible,
        };
    }

    function destroyToolbar(id) {
        var tb = tbars[id];
        delete tbars[id];

        if (tb) {
            ps.destroyPanel(tb.panelId);
        }
    }

    // === Module Definition ===

    angular.module('onosWidget')
    .factory('ToolbarService',
        ['$log', 'FnService', 'PanelService', 'ButtonService', 'IconService',

        function (_$log_, _fs_, _ps_, _bns_, _is_) {
            $log = _$log_;
            fs = _fs_;
            ps = _ps_;
            bns = _bns_;
            is = _is_;

            // this function is only used in testing
            function init() {
                tbars = {};
            }

            return {
                init: init,
                createToolbar: createToolbar,
                destroyToolbar: destroyToolbar,
            };
        }]);
}());
