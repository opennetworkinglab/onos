/*
 *  Copyright 2015 Open Networking Laboratory
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
 ONOS GUI -- Topology Dialog Module.
 Defines functions for manipulating a dialog box.
 */

(function () {
    'use strict';

    // injected refs
    var $log, $window, $rootScope, fs, ps, bns;

    // constants
    var pCls = 'topo-p dialog',
        idDialog = 'topo-p-dialog',
        panelOpts = {
            width: 300,
            edge: 'left'
        };

    // internal state
    var pApi, panel, dApi;

    // TODO: ESC key invokes Cancel callback
    // TODO: Enter invokes OK callback

    // create the dialog; return its API
    function createDialog() {
        var header, body, footer,
            p = ps.createPanel(idDialog, panelOpts);
        p.classed(pCls, true);
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
            ps.destroyPanel(idDialog);
        }

        return {
            reset: reset,
            appendHeader: hAppend,
            appendBody: bAppend,
            appendFooter: fAppend,
            destroy: destroy
        };
    }

    function makeButton(text, callback) {
        var cb = fs.isF(callback);

        function invoke() {
            cb && cb();
            panel.hide();
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

    function addButton(text, cb) {
        if (pApi) {
            pApi.appendFooter(makeButton(text, cb));
        }
        return dApi;
    }

    // opens the dialog (creates if necessary)
    function openDialog() {
        $log.debug('Open DIALOG');
        if (!pApi) {
            pApi = createDialog();
        }
        pApi.reset();
        panel.show();

        // return the dialog object API
        dApi = {
            setTitle: setTitle,
            addContent: addContent,
            addButton: addButton
        };
        return dApi;
    }

    // closes the dialog (destroying panel)
    function closeDialog() {
        $log.debug('Close DIALOG');
        if (pApi) {
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

    // ==========================

    angular.module('ovTopo')
    .factory('TopoDialogService',
        ['$log', '$window', '$rootScope', 'FnService', 'PanelService', 'ButtonService',

        function (_$log_, _$window_, _$rootScope_,
                  _fs_, _ps_, _bns_) {
            $log = _$log_;
            $window = _$window_;
            $rootScope = _$rootScope_;
            fs = _fs_;
            ps = _ps_;
            bns = _bns_;

            return {
                openDialog: openDialog,
                closeDialog: closeDialog,
                createDiv: createDiv
            };
        }]);
}());
