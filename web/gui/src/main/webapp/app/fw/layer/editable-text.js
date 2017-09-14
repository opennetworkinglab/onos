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

    var ks, wss;

    var EditableText = function (el, options) {
      // constructor
        this.el = el;
        this.scope = options.scope;
        this.options = options;

        this.el.classed('editable clickable', true).on('click', this.onEdit.bind(this));
        this.editingName = false;
    };

    EditableText.prototype = {

        bindHandlers: function () {
            ks.keyBindings({
                'enter': this.save.bind(this),
                'esc': [this.cancel.bind(this), 'Close the details panel']
            });
        },

        unbindHandlers: function () {
            ks.unbindKeys();

            if (this.options.keyBindings) {
                // Reset to original bindings before editable text
                ks.keyBindings(this.options.keyBindings);
            }
        },

        addTextField: function () {
            return this.el.append('input').classed('name-input', true)
                .attr('type', 'text')
                .attr('value', this.scope.panelData.name)[0][0];
        },

        onEdit: function () {
            if (!this.editingName) {
                this.el.classed('editable clickable', false);
                this.el.text('');

                var el = this.addTextField();
                el.focus();
                el.select();
                this.editingName = true;

                this.bindHandlers();

                ks.enableGlobalKeys(false);
            }
        },

        exit: function (name) {
            this.el.text(name);
            this.el.classed('editable clickable', true);
            this.editingName = false;
            ks.enableGlobalKeys(true);
            this.unbindHandlers();
        },

        cancel: function (a, b, ev) {

            if (this.editingName) {
                this.exit(this.scope.panelData.name);
                return true;
            }

            return false;
        },

        save: function () {
            var id = this.scope.panelData.id,
                val,
                newVal;

            if (this.editingName) {
                val = this.el.select('input').property('value').trim();
                newVal = val || id;

                this.exit(newVal);
                this.scope.panelData.name = newVal;
                wss.sendEvent(this.options.nameChangeRequest, { id: id, name: val });
            }
        },
    };


    angular.module('onosLayer')
        .factory('EditableTextComponent', [

            'KeyService', 'WebSocketService',

            function (_ks_, _wss_) {
                ks = _ks_;
                wss = _wss_;

                return EditableText;
            },
        ]);

})();
