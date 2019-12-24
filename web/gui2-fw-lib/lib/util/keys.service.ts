/*
 * Copyright 2018-present Open Networking Foundation
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
import { Injectable } from '@angular/core';
import * as d3 from 'd3';
import { LogService } from '../log.service';
import { FnService } from '../util/fn.service';
import { LionService } from './lion.service';
import { NavService } from '../nav/nav.service';

export interface KeyHandler {
    globalKeys: Object;
    maskedKeys: Object;
    dialogKeys: Object;
    viewKeys: any;
    viewFn: any;
    viewGestures: string[][];
}

export enum KeysToken {
    KEYEV = 'keyev'
}

/**
 * ONOS GUI -- Keys Service Module.
 */
@Injectable({
    providedIn: 'root',
})
export class KeysService {
    enabled: boolean = true;
    globalEnabled: boolean = true;
    keyHandler: KeyHandler = <KeyHandler>{
        globalKeys: {},
        maskedKeys: {},
        dialogKeys: {},
        viewKeys: {},
        viewFn: null,
        viewGestures: [],
    };

    seq: any = {};
    matching: boolean = false;
    matched: string = '';
    lookup: any;
    textFieldDoesNotBlock: any = {
        enter: 1,
        esc: 1,
    };
    quickHelpShown: boolean = false;

    constructor(
        protected log: LogService,
        protected fs: FnService,
        protected ls: LionService,
        protected ns: NavService
    ) {
        this.log.debug('KeyService constructed');
    }

    installOn(elem) {
        this.log.debug('Installing keys handler');
        elem.on('keydown', () => { this.keyIn(); });
        this.setupGlobalKeys();
    }

    keyBindings(x) {
        if (x === undefined) {
            return this.getKeyBindings();
        } else {
            this.setKeyBindings(x);
        }
    }

    unbindKeys() {
        this.keyHandler.viewKeys = {};
        this.keyHandler.viewFn = null;
        this.keyHandler.viewGestures = [];
    }

    dialogKeys(x) {
        if (x === undefined) {
            this.unbindDialogKeys();
        } else {
            this.bindDialogKeys(x);
        }
    }

    addSeq(word, data) {
        this.fs.addToTrie(this.seq, word, data);
    }

    remSeq(word) {
        this.fs.removeFromTrie(this.seq, word);
    }

    gestureNotes(g?) {
        if (g === undefined) {
            return this.keyHandler.viewGestures;
        } else {
            this.keyHandler.viewGestures = this.fs.isA(g) || [];
        }
    }

    enableKeys(b) {
        this.enabled = b;
    }

    enableGlobalKeys(b) {
        this.globalEnabled = b;
    }

    checkNotGlobal(o) {
        const oops = [];
        if (this.fs.isO(o)) {
            o.forEach((val, key) => {
                if (this.keyHandler.globalKeys[key]) {
                    oops.push(key);
                }
            });
            if (oops.length) {
                this.log.warn('Ignoring reserved global key(s):', oops.join(','));
                oops.forEach((key) => {
                    delete o[key];
                });
            }
        }
    }

    protected matchSeq(key) {
        if (!this.matching && key === 'shift-shift') {
            this.matching = true;
            return true;
        }
        if (this.matching) {
            this.matched += key;
            this.lookup = this.fs.trieLookup(this.seq, this.matched);
            if (this.lookup === -1) {
                return true;
            }
            this.matching = false;
            this.matched = '';
            if (!this.lookup) {
                return;
            }
            // ee.cluck(lookup);
            return true;
        }
    }

    protected whatKey(code: number): string {
        switch (code) {
            case 8: return 'delete';
            case 9: return 'tab';
            case 13: return 'enter';
            case 16: return 'shift';
            case 27: return 'esc';
            case 32: return 'space';
            case 37: return 'leftArrow';
            case 38: return 'upArrow';
            case 39: return 'rightArrow';
            case 40: return 'downArrow';
            case 186: return 'semicolon';
            case 187: return 'equals';
            case 188: return 'comma';
            case 189: return 'dash';
            case 190: return 'dot';
            case 191: return 'slash';
            case 192: return 'backQuote';
            case 219: return 'openBracket';
            case 220: return 'backSlash';
            case 221: return 'closeBracket';
            case 222: return 'quote';
            default:
                if ((code >= 48 && code <= 57) ||
                    (code >= 65 && code <= 90)) {
                    return String.fromCharCode(code);
                } else if (code >= 112 && code <= 123) {
                    return 'F' + (code - 111);
                }
                return null;
        }
    }

    protected textFieldInput() {
        const t = d3.event.target.tagName.toLowerCase();
        return t === 'input' || t === 'textarea';
    }

    protected keyIn() {
        const event = d3.event;
        // d3.events can set the keyCode, but unit tests based on KeyboardEvent
        // cannot set keyCode since the attribute has been deprecated
        const code = event.keyCode ? event.keyCode : event.code;
        const codeNum: number = parseInt(code, 10);
        let key = this.whatKey(codeNum);
        this.log.debug('Key detected', event, key, event.code, event.keyCode);
        const textBlockable = !this.textFieldDoesNotBlock[key];
        const modifiers = [];

        if (event.metaKey) {
            modifiers.push('cmd');
        }
        if (event.altKey) {
            modifiers.push('alt');
        }
        if (event.shiftKey) {
            modifiers.push('shift');
        }

        if (!key) {
            return;
        }

        modifiers.push(key);
        key = modifiers.join('-');

        if (textBlockable && this.textFieldInput()) {
            return;
        }

        const kh: KeyHandler = this.keyHandler;
        const gk = kh.globalKeys[key];
        const gcb = this.fs.isF(gk) || (this.fs.isA(gk) && this.fs.isF(gk[0]));
        const dk = kh.dialogKeys[key];
        const dcb = this.fs.isF(dk);
        const vk = kh.viewKeys[key];
        const kl = this.fs.isF(kh.viewKeys._keyListener);
        const vcb = this.fs.isF(vk) || (this.fs.isA(vk) && this.fs.isF(vk[0])) || this.fs.isF(kh.viewFn);
        const token: KeysToken = KeysToken.KEYEV; // indicate this was a key-pressed event

        event.stopPropagation();

        if (this.enabled) {
            if (this.matchSeq(key)) {
                return;
            }

            // global callback?
            if (gcb && gcb(token, key, code, event)) {
                // if the event was 'handled', we are done
                return;
            }
            // dialog callback?
            if (dcb) {
                dcb(token, key, code, event);
                // assume dialog handled the event
                return;
            }
            // otherwise, let the view callback have a shot
            if (vcb) {
                this.log.debug('Letting view callback have a shot', vcb, token, key, code, event );
                vcb(token, key, code, event);
            }
            if (kl) {
                kl(key);
            }
        }
    }

    // functions to obtain localized strings deferred from the setup of the
    //  global key data structures.
    protected qhlion() {
        return this.ls.bundle('core.fw.QuickHelp');
    }
    protected qhlionShowHide() {
        return this.qhlion()('qh_hint_show_hide_qh');
    }

    protected qhlionHintEsc() {
        return this.qhlion()('qh_hint_esc');
    }

    protected qhlionHintT() {
        return this.qhlion()('qh_hint_t');
    }

    protected setupGlobalKeys() {
        (<any>Object).assign(this.keyHandler, {
            globalKeys: {
                backSlash: [(view, key, code, ev) => this.quickHelp(view, key, code, ev), this.qhlionShowHide],
                slash: [(view, key, code, ev) => this.quickHelp(view, key, code, ev), this.qhlionShowHide],
                esc: [(view, key, code, ev) => this.escapeKey(view, key, code, ev), this.qhlionHintEsc],
                T: [(view, key, code, ev) => this.toggleTheme(view, key, code, ev), this.qhlionHintT],
            },
            globalFormat: ['backSlash', 'slash', 'esc', 'T'],

            // Masked keys are global key handlers that always return true.
            // That is, the view will never see the event for that key.
            maskedKeys: {
                slash: 1,
                backSlash: 1,
                T: 1,
            },
        });
    }

    protected quickHelp(view, key, code, ev) {
        if (!this.globalEnabled) {
            return false;
        }
        this.quickHelpShown = !this.quickHelpShown;
        return true;
    }

    // returns true if we 'consumed' the ESC keypress, false otherwise
    protected escapeKey(view, key, code, ev) {
        this.quickHelpShown = false;
        return this.ns.hideNav();
    }

    protected toggleTheme(view, key, code, ev) {
        if (!this.globalEnabled) {
            return false;
        }
        // ts.toggleTheme();
        return true;
    }

    protected filterMaskedKeys(map: any, caller: any, remove: boolean): any[] {
        const masked = [];
        const msgs = [];

        d3.map(map).keys().forEach((key) => {
            if (this.keyHandler.maskedKeys[key]) {
                masked.push(key);
                msgs.push(caller, ': Key "' + key + '" is reserved');
            }
        });

        if (msgs.length) {
            this.log.warn(msgs.join('\n'));
        }

        if (remove) {
            masked.forEach((k) => {
                delete map[k];
            });
        }
        return masked;
    }

    protected unexParam(fname, x) {
        this.log.warn(fname, ': unexpected parameter-- ', x);
    }

    protected setKeyBindings(keyArg) {
        const fname = 'setKeyBindings()';
        const kFunc = this.fs.isF(keyArg);
        const kMap = this.fs.isO(keyArg);

        if (kFunc) {
            // set general key handler callback
            this.keyHandler.viewFn = kFunc;
        } else if (kMap) {
            this.filterMaskedKeys(kMap, fname, true);
            this.keyHandler.viewKeys = kMap;
        } else {
            this.unexParam(fname, keyArg);
        }
    }

    getKeyBindings() {
        const gkeys = d3.map(this.keyHandler.globalKeys).keys();
        const masked = d3.map(this.keyHandler.maskedKeys).keys();
        const vkeys = d3.map(this.keyHandler.viewKeys).keys();
        const vfn = !!this.fs.isF(this.keyHandler.viewFn);

        return {
            globalKeys: gkeys,
            maskedKeys: masked,
            viewKeys: vkeys,
            viewFunction: vfn,
        };
    }

    protected bindDialogKeys(map) {
        const fname = 'bindDialogKeys()';
        const kMap = this.fs.isO(map);

        if (kMap) {
            this.filterMaskedKeys(map, fname, true);
            this.keyHandler.dialogKeys = kMap;
        } else {
            this.unexParam(fname, map);
        }
    }

    protected unbindDialogKeys() {
        this.keyHandler.dialogKeys = {};
    }

}
