/*
 * Copyright 2017-present Open Networking Foundation
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

(function () {

    var ps, fs, mast, wss, is, EditableTextComponent;

    var panel,
        pStartY,
        wSize,
        wssHandlers = {},
        options;

    // Constants
    var topPdg = 28,
        defaultLabelWidth = 110,
        defaultValueWidth = 80;

    // Elements
    var container,
        top,
        bottom;

    function createDetailsPanel(name, _options) {
        options = _options;
        scope = options.scope;

        panel = ps.createPanel(name, options);

        calculatePositions();

        panel.el().style({
            position: 'absolute',
            top: pStartY + 'px',
        });

        hide();

        return panel;
    }

    function calculatePositions() {
        pStartY = fs.noPxStyle(d3.select('.tabular-header'), 'height')
            + mast.mastHeight() + topPdg;
        wSize = fs.windowSize(pStartY);
        pHeight = wSize.height;
    }

    function hide() {
        panel.hide();
    }

    function setResponse(name, callback) {
        var additionalHandler = {};
        additionalHandler[name] = callback;

        wss.bindHandlers(additionalHandler);
        wssHandlers = _.extend({}, wssHandlers, additionalHandler);
    }

    function addContainers() {
        container = panel.append('div').classed('container', true);
        top = container.append('div').classed('top', true);
        bottom = container.append('div').classed('bottom', true);
    }

    function addCloseButton(onClose) {
        var closeBtn = top.append('div').classed('close-btn', true);

        is.loadEmbeddedIcon(closeBtn, 'close', 20);
        closeBtn.on('click', onClose || function () {});
    }

    function addHeading(icon, makeEditable) {
        top.append('div').classed('iconDiv ' + icon, true);

        if (makeEditable) {
            new EditableTextComponent(top.append('h2'), {
                scope: options.scope,
                nameChangeRequest: options.nameChangeRequest,
                keyBindings: options.keyBindings,
            });
        } else {
            top.append('h2');   // note: title is inserted later
        }
    }

    function addTable(parent, className) {
        return parent.append('div').classed(className, true).append('table');
    }

    function addProp(tbody, key, value) {
        var tr = tbody.append('tr');

        function addCell(cls, txt, width) {
            tr.append('td').attr('class', cls).attr('width', width).text(txt);
        }

        addCell('label', key + ' :', defaultLabelWidth);
        addCell('value', value, defaultValueWidth);
    }

    function addPropsList(el, props) {
        var tblDiv = el.append('div').classed('top-tables', true);
        var left = addTable(tblDiv, 'left').append('tbody');
        var right = addTable(tblDiv, 'right').append('tbody');

        var keys = _.keys(props);

        _.each(props, function (value, key) {
            var index = keys.indexOf(key);

            if (index < keys.length / 2) {
                addProp(left, key, value);
            } else {
                addProp(right, key, value);
            }
        });
    }

    function empty() {
        panel.empty();
    }

    function select(id) {
        return panel.el().select(id);
    }

    function destroy() {
        wss.unbindHandlers(wssHandlers);
    }

    angular.module('onosLayer')
        .factory('DetailsPanelService', [

            'PanelService', 'FnService', 'MastService', 'WebSocketService',
            'IconService', 'EditableTextComponent',

            function (_ps_, _fs_, _mast_, _wss_, _is_, _etc_) {

                ps = _ps_;
                fs = _fs_;
                mast = _mast_;
                wss = _wss_;
                is = _is_;
                EditableTextComponent = _etc_;

                return {
                    create: createDetailsPanel,
                    setResponse: setResponse,

                    addContainers: addContainers,
                    addCloseButton: addCloseButton,
                    addHeading: addHeading,
                    addPropsList: addPropsList,

                    // Elements
                    container: function () { return container; },
                    top: function () { return top; },
                    bottom: function () { return bottom; },
                    select: select,

                    empty: empty,
                    hide: hide,
                    destroy: destroy,
                };
            },
        ]);
})();
