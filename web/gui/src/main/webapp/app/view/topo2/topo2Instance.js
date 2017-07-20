(function () {
    'use strict';

    // injected refs
    var $log, ps, sus, gs, flash, ts, t2mss;

    // api from topo
    var api;

    // configuration
    var showLogicErrors = true,
        idIns = 'topo2-p-instance',
        instOpts = {
            edge: 'left',
            width: 20
        };

    // internal state
    var onosInstances,
        onosOrder,
        oiBox;

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

    // ==========================

    function clickInst(d) {
        var el = d3.select(this),
            aff = el.classed('affinity');
        if (aff) {
            cancelAffinity();
        } else {
            setAffinity(el, d);
        }
    }

    function setAffinity(el, d) {
        d3.selectAll('.onosInst')
            .classed('mastership', true)
            .classed('affinity', false);
        el.classed('affinity', true);

        t2mss.setMastership(d.id);
    }

    function cancelAffinity() {
        d3.selectAll('.onosInst')
            .classed('mastership affinity', false);

        t2mss.setMastership(null);
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
                viewBox: '0 0 170 85'
            },
            headRect = {
                x: rox,
                y: roy,
                width: rw,
                height: rhh
            },
            bodyRect = {
                x: rox,
                y: roy + rhh,
                width: rw,
                height: rbh
            },
            titleAttr = {
                class: 'instTitle',
                x: tx,
                y: 27
            };

        var onoses = oiBox.el().selectAll('.onosInst')
                .data(onosOrder, function (d) { return d.id; });

        function nSw(n) {
            return 'Devices: ' + n;
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
                svg.select('text.instLabel.' + id).text(value);
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
                    y: ty
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
            $log.warn('Topo2InstService: ' + msg);
        }
    }

    function initInst(_api_) {
        api = _api_;
        oiBox = ps.createPanel(idIns, instOpts);
        oiBox.show();

        onosInstances = {};
        onosOrder = [];

        // we want to update the instances, each time the theme changes
        ts.addListener(updateInstances);
    }

    function allInstances(data) {
        $log.debug('Update all instances', data);

        var members = data.members;

        members.forEach(function (member) {
            addInstance(member);
        });
    }

    function toggle(x) {
        var kev = (x === 'keyev'),
            on,
            verb;

        if (kev) {
            on = oiBox.toggle();
        } else {
            on = Boolean(x);
            if (on) {
                oiBox.show();
            } else {
                oiBox.hide();
            }
        }
        verb = on ? 'Show' : 'Hide';
        flash.flash(verb + ' instances panel');
        return on;
    }

    function destroy() {
        ts.removeListener(updateInstances);

        ps.destroyPanel(idIns);
        oiBox = null;

        onosInstances = {};
        onosOrder = [];
    }

    angular.module('ovTopo2')
        .factory('Topo2InstanceService', [
            '$log', 'PanelService', 'SvgUtilService', 'GlyphService',
            'FlashService', 'ThemeService', 'Topo2MastershipService',

            function (_$log_, _ps_, _sus_, _gs_, _flash_, _ts_, _t2mss_) {
                $log = _$log_;
                ps = _ps_;
                sus = _sus_;
                gs = _gs_;
                flash = _flash_;
                ts = _ts_;
                t2mss = _t2mss_;

                return {
                    initInst: initInst,
                    allInstances: allInstances,
                    destroy: destroy,
                    toggle: toggle,
                    isVisible: function () { return oiBox.isVisible(); }
                };
            }
        ]);

})();
