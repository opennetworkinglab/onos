/*
 *  Copyright 2016-present Open Networking Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

/*
 ONOS GUI -- Layer -- Dialog Service

 Builds on the panel service to provide dialog functionality.
 */
(function () {
    'use strict';

    // injected refs
    var $log, fs, ps, ks;

    // configuration
    var defaultSettings = {
            width: 300,
            edge: 'left',
        };

    // internal state
    var pApi, panel, dApi,
        keyBindings = {};

    // create the dialog; return its API
    function createDialog(id, opts) {
        var header, body, footer,
            settings = angular.extend({}, defaultSettings, opts),
            p = ps.createPanel(id, settings),
            cls = opts && opts.cssCls;

        p.classed('dialog', true);
        if (cls) {
            p.classed(cls, true);
        }
        panel = p;

        function reset() {
            p.empty();
            p.append('div').classed('header', true);
            p.append('div').classed('body', true);
            p.append('div').classed('footer', true);

            header = p.el().select('.header');
            body = p.el().select('.body');
            footer = p.el().select('.footer');
        }

        function hAppend(x) {
            if (typeof x === 'string') {
                return header.append(x);
            }
            header.node().appendChild(x.node());
            return header;
        }

        function bAppend(x) {
            if (typeof x === 'string') {
                return body.append(x);
            }
            body.node().appendChild(x.node());
            return body;
        }

        function fAppend(x) {
            if (typeof x === 'string') {
                return footer.append(x);
            }
            footer.node().appendChild(x.node());
            return footer;
        }

        function destroy() {
            ps.destroyPanel(id);
        }

        return {
            reset: reset,
            appendHeader: hAppend,
            appendBody: bAppend,
            appendFooter: fAppend,
            destroy: destroy,
        };
    }

    function makeButton(callback, text, keyName, chained) {
        var cb = fs.isF(callback),
            key = fs.isS(keyName);

        function invoke() {
            cb && cb();
            if (!chained) {
                clearBindings();
                panel.hide();
            }
        }

        if (key) {
            keyBindings[key] = invoke;
        }

        return createDiv('dialog-button')
            .text(text)
            .on('click', invoke);
    }

    function setTitle(title) {
        if (pApi) {
            pApi.appendHeader('h2').text(title);
        }
        return dApi;
    }

    function addContent(content) {
        if (pApi) {
            pApi.appendBody(content);
        }
        return dApi;
    }

    function addButton(cb, text, key, chained) {
        if (pApi) {
            pApi.appendFooter(makeButton(cb, text, key, chained));
        }
        return dApi;
    }

    function _addOk(cb, text, chained) {
        return addButton(cb, text || 'OK', 'enter', chained);
    }

    function addOk(cb, text) {
        return _addOk(cb, text, false);
    }

    function addOkChained(cb, text) {
        return _addOk(cb, text, true);
    }

    function addCancel(cb, text) {
        return addButton(cb, text || 'Cancel', 'esc');
    }

    function clearBindings() {
        keyBindings = {};
        ks.dialogKeys();
    }

    // opens the dialog (creates if necessary)
    function openDialog(id, opts) {
        $log.debug('Open DIALOG', id, opts);
        if (!pApi) {
            pApi = createDialog(id, opts);
        }
        pApi.reset();
        panel.show();

        // return the dialog object API
        dApi = {
            setTitle: setTitle,
            addContent: addContent,
            addButton: addButton,
            addOk: addOk,
            addOkChained: addOkChained,
            addCancel: addCancel,
            bindKeys: function () {
                ks.dialogKeys(keyBindings);
            },
        };
        return dApi;
    }

    // closes the dialog (destroying panel)
    function closeDialog() {
        $log.debug('Close DIALOG');
        if (pApi) {
            clearBindings();
            panel.hide();
            pApi.destroy();
            pApi = null;
            dApi = null;
        }
    }

    // creates a detached div, returning D3 selection
    // optional CSS class may be provided
    function createDiv(cls) {
        var div = d3.select(document.createElement('div'));
        if (cls) {
            div.classed(cls, true);
        }
        return div;
    }

    angular.module('onosLayer')
    .factory('DialogService',
        ['$log', 'FnService', 'PanelService', 'KeyService',

        // TODO: use $window to provide an option to center the
        // dialog on the window.

        function (_$log_, _fs_, _ps_, _ks_) {
            $log = _$log_;
            fs = _fs_;
            ps = _ps_;
            ks = _ks_;

            return {
                openDialog: openDialog,
                closeDialog: closeDialog,
                createDiv: createDiv,
            };
        }]);
}());
