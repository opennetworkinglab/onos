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
 ONOS GUI -- Topology Instances Module.
 Defines modeling of ONOS instances.
 */

(function () {
    'use strict';

    // injected refs
    var $log, ps, sus, gs, ts, fs, flash;

    // api from topo
    var api;
    /*
        showMastership( id )
     */

    // configuration
    var showLogicErrors = true,
        idIns = 'topo-p-instance',
        instOpts = {
            edge: 'left',
            width: 20,
        };

    // internal state
    var onosInstances,
        onosOrder,
        oiShowMaster,
        oiBox;

    // function to be replaced by the localization bundle function
    var topoLion = function (x) {
        return '#tis#' + x + '#';
    };


    function addInstance(data) {
        var id = data.id;

        if (onosInstances[id]) {
            updateInstance(data);
            return;
        }
        onosInstances[id] = data;
        onosOrder.push(data);
        updateInstances();
    }

    function updateInstance(data) {
        var id = data.id,
            d = onosInstances[id];
        if (d) {
            angular.extend(d, data);
            updateInstances();
        } else {
            logicError('updateInstance: lookup fail: ID = "' + id + '"');
        }
    }

    function removeInstance(data) {
        var id = data.id,
            d = onosInstances[id];
        if (d) {
            var idx = fs.find(id, onosOrder);
            if (idx >= 0) {
                onosOrder.splice(idx, 1);
            }
            delete onosInstances[id];
            updateInstances();
        } else {
            logicError('removeInstance lookup fail. ID = "' + id + '"');
        }
    }

    // ==========================

    function clickInst(d) {
        var el = d3.select(this),
            aff = el.classed('affinity');
        if (!aff) {
            setAffinity(el, d);
        } else {
            cancelAffinity();
        }
    }

    function setAffinity(el, d) {
        d3.selectAll('.onosInst')
            .classed('mastership', true)
            .classed('affinity', false);
        el.classed('affinity', true);

        // suppress all elements except nodes whose master is this instance
        api.showMastership(d.id);
        oiShowMaster = true;
    }

    function cancelAffinity() {
        d3.selectAll('.onosInst')
            .classed('mastership affinity', false);

        api.showMastership(null);
        oiShowMaster = false;
    }

    function attachUiBadge(svg) {
        gs.addGlyph(svg, 'uiAttached', 24, true, [14, 54])
            .classed('badgeIcon uiBadge', true);
    }

    function attachReadyBadge(svg) {
        gs.addGlyph(svg, 'checkMark', 16, true, [18, 40])
            .classed('badgeIcon readyBadge', true);
    }

    function instColor(id, online) {
        return sus.cat7().getColor(id, !online, ts.theme());
    }

    // ==============================

    function updateInstances() {
        var rox = 5,
            roy = 5,
            rw = 160,
            rhh = 30,
            rbh = 45,
            tx = 48,
            instSvg = {
                width: 170,
                height: 85,
                viewBox: '0 0 170 85',
            },
            headRect = {
                x: rox,
                y: roy,
                width: rw,
                height: rhh,
            },
            bodyRect = {
                x: rox,
                y: roy + rhh,
                width: rw,
                height: rbh,
            },
            titleAttr = {
                class: 'instTitle',
                x: tx,
                y: 27,
            };

        var onoses = oiBox.el().selectAll('.onosInst')
                .data(onosOrder, function (d) { return d.id; });

        function nSw(n) {
            return topoLion('devices') + ': ' + n;
        }

        // operate on existing onos instances if necessary
        onoses.each(function (d) {
            var el = d3.select(this),
                svg = el.select('svg');

            // update online state
            el.classed('online', d.online);
            el.classed('ready', d.ready);

            // update ui-attached state
            svg.select('use.uiBadge').remove();
            if (d.uiAttached) {
                attachUiBadge(svg);
            }

            function updAttr(id, value) {
                svg.select('text.instLabel.'+id).text(value);
            }

            updAttr('ip', d.ip);
            updAttr('ns', nSw(d.switches));
        });


        // operate on new onos instances
        var entering = onoses.enter()
            .append('div')
            .classed('onosInst', true)
            .classed('online', function (d) { return d.online; })
            .classed('ready', function (d) { return d.ready; })
            .on('click', clickInst);

        entering.each(function (d) {
            var el = d3.select(this),
                svg = el.append('svg').attr(instSvg);

            svg.append('rect').attr(headRect);
            svg.append('rect').attr(bodyRect);

            gs.addGlyph(svg, 'bird', 20, false, [15, 10])
                .classed('badgeIcon bird', true);

            attachReadyBadge(svg);

            if (d.uiAttached) {
                attachUiBadge(svg);
            }

            svg.append('text')
                .attr(titleAttr)
                .text(d.id);

            var ty = 55;
            function addAttr(id, label) {
                svg.append('text').attr({
                    class: 'instLabel ' + id,
                    x: tx,
                    y: ty,
                }).text(label);
                ty += 18;
            }

            addAttr('ip', d.ip);
            addAttr('ns', nSw(d.switches));
        });

        // operate on existing + new onoses here
        // set the affinity colors...
        onoses.each(function (d) {
            var el = d3.select(this),
                rect = el.select('svg').select('rect'),
                col = instColor(d.id, d.online);
            rect.style('fill', col);
        });

        // adjust the panel size appropriately...
        oiBox.width(instSvg.width * onosOrder.length);
        oiBox.height(instSvg.height);

        // remove any outgoing instances
        onoses.exit().remove();
    }


    // ==========================

    function logicError(msg) {
        if (showLogicErrors) {
            $log.warn('TopoInstService: ' + msg);
        }
    }

    function initInst(_api_) {
        api = _api_;
        oiBox = ps.createPanel(idIns, instOpts);
        oiBox.show();

        onosInstances = {};
        onosOrder = [];
        oiShowMaster = false;

        // we want to update the instances, each time the theme changes
        ts.addListener(updateInstances);
    }

    function destroyInst() {
        ts.removeListener(updateInstances);

        ps.destroyPanel(idIns);
        oiBox = null;

        onosInstances = {};
        onosOrder = [];
        oiShowMaster = false;
    }

    function showInsts() {
        oiBox.show();
    }

    function hideInsts() {
        oiBox.hide();
    }

    function toggleInsts(x) {
        var kev = (x === 'keyev'),
            on,
            verb;

        if (kev) {
            on = oiBox.toggle();
        } else {
            on = !!x;
            if (on) {
                showInsts();
            } else {
                hideInsts();
            }
        }
        verb = on ? topoLion('show') : topoLion('hide');
        flash.flash(verb + ' ' + topoLion('fl_panel_instances'));
        return on;
    }

    // ==========================

    angular.module('ovTopo')
    .factory('TopoInstService',
        ['$log', 'PanelService', 'SvgUtilService', 'GlyphService',
            'ThemeService', 'FnService', 'FlashService',

        function (_$log_, _ps_, _sus_, _gs_, _ts_, _fs_, _flash_) {
            $log = _$log_;
            ps = _ps_;
            sus = _sus_;
            gs = _gs_;
            ts = _ts_;
            fs = _fs_;
            flash = _flash_;

            return {
                initInst: initInst,
                destroyInst: destroyInst,

                addInstance: addInstance,
                updateInstance: updateInstance,
                removeInstance: removeInstance,

                cancelAffinity: cancelAffinity,

                isVisible: function () { return oiBox.isVisible(); },
                show: showInsts,
                hide: hideInsts,
                toggle: toggleInsts,
                showMaster: function () { return oiShowMaster; },
                setLionBundle: function (bundle) { topoLion = bundle; },
            };
        }]);
}());
