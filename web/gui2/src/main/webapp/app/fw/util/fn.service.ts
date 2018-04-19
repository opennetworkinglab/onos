/*
 * Copyright 2014-present Open Networking Foundation
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
import { ActivatedRoute, Router} from '@angular/router';
import { LogService } from '../../log.service';

// Angular>=2 workaround for missing definition
declare const InstallTrigger: any;


// TODO Move all this trie stuff to its own class
// Angular>=2 Tightened up on types to avoid compiler errors
interface TrieC {
    p: any,
    s: string[]
}
// trie operation
function _trieOp(op: string, trie, word: string, data) {
    var p = trie,
        w: string = word.toUpperCase(),
        s: Array<string> = w.split(''),
        c:TrieC = { p: p, s: s },
        t = [],
        x: number = 0,
        f1 = op === '+' ? add : probe,
        f2 = op === '+' ? insert : remove;

    function add(c):TrieC {
        var q = c.s.shift(),
            np = c.p[q];

        if (!np) {
            c.p[q] = {};
            np = c.p[q];
            x = 1;
        }
        return { p: np, s: c.s };
    }

    function probe(c):TrieC {
        var q = c.s.shift(),
            k:number = Object.keys(c.p).length,
            np = c.p[q];

        t.push({ q: q, k: k, p: c.p });
        if (!np) {
            t = [];
            return { p: [], s: [] };
        }
        return { p: np, s: c.s };
    }

    function insert() {
        c.p._data = data;
        return x ? 'added' : 'updated';
    }

    function remove() {
        if (t.length) {
            t = t.reverse();
            while (t.length) {
                let d = t.shift();
                delete d.p[d.q];
                if (d.k > 1) {
                    t = [];
                }
            }
            return 'removed';
        }
        return 'absent';
    }

    while (c.s.length) {
        c = f1(c);
    }
    return f2();
}

// add word to trie (word will be converted to uppercase)
// data associated with the word
// returns 'added' or 'updated'
function addToTrie(trie, word, data) {
    return _trieOp('+', trie, word, data);
}

// remove word from trie (word will be converted to uppercase)
// returns 'removed' or 'absent'
// Angular>=2 added in quotes for data. error TS2554: Expected 4 arguments, but got 3.
function removeFromTrie(trie, word) {
    return _trieOp('-', trie, word, '');
}

// lookup word (converted to uppercase) in trie
// returns:
//    undefined if the word is not in the trie
//    -1 for a partial match (word is a prefix to an existing word)
//    data for the word for an exact match
function trieLookup(trie, word) {
    var s = word.toUpperCase().split(''),
        p = trie,
        n;

    while (s.length) {
        n = s.shift();
        p = p[n];
        if (!p) {
            return undefined;
        }
    }
    if (p._data) {
        return p._data;
    }
    return -1;
}


/**
 * ONOS GUI -- Util -- General Purpose Functions
 */
@Injectable()
export class FnService {
    // internal state
    debugFlags = new Map<string, boolean>([
//        [ "LoadingService", true ]
    ]);

    constructor(
        private route: ActivatedRoute,
        private log: LogService
    ) {
        this.route.queryParams.subscribe(params => {
            let debugparam: string = params['debug'];
            this.parseDebugFlags(debugparam);
        });
        log.debug("FnService constructed");
    }

    isF(f) {
        return typeof f === 'function' ? f : null;
    }

    isA(a) {
    // NOTE: Array.isArray() is part of EMCAScript 5.1
        return Array.isArray(a) ? a : null;
    }

    isS(s) {
        return typeof s === 'string' ? s : null;
    }

    isO(o) {
        return (o && typeof o === 'object' && o.constructor === Object) ? o : null;
    }

//    contains: contains,
//    areFunctions: areFunctions,
//    areFunctionsNonStrict: areFunctionsNonStrict,
//    windowSize: windowSize,

    /**
     * Returns true if current browser determined to be a mobile device
     */
    isMobile() {
        var ua = window.navigator.userAgent,
            patt = /iPhone|iPod|iPad|Silk|Android|BlackBerry|Opera Mini|IEMobile/;
        return patt.test(ua);
    }

    /**
     * Returns true if the current browser determined to be Chrome
     */
    isChrome() {
        let isChromium = (window as any).chrome;
        let vendorName = window.navigator.vendor;

        let isOpera = window.navigator.userAgent.indexOf('OPR') > -1;
        return (isChromium !== null &&
        isChromium !== undefined &&
        vendorName === 'Google Inc.' &&
        isOpera == false);
    }

    /**
     * Returns true if the current browser determined to be Safari
     */
    isSafari() {
        return (window.navigator.userAgent.indexOf('Safari') !== -1 &&
        window.navigator.userAgent.indexOf('Chrome') === -1);
    }

    /**
     * Returns true if the current browser determined to be Firefox
     */
    isFirefox() {
        return typeof InstallTrigger !== 'undefined';
    }

    /**
     * Return the given string with the first character capitalized.
     */
    cap(s) {
        return s ? s[0].toUpperCase() + s.slice(1).toLowerCase() : s;
    }

    /**
     * output debug message to console, if debug tag set...
     * e.g. fs.debug('mytag', arg1, arg2, ...)
     */
    debug(tag, ...args) {
        if (this.debugFlags.get(tag)) {
            this.log.debug(tag, args.join());
        }
    }

    parseDebugFlags(dbgstr: string): void {
        let bits = dbgstr ? dbgstr.split(',') : [];
        bits.forEach(function (key) {
            this.debugFlags.set(key, true);
        });
        this.log.debug('Debug flags:', dbgstr);
    }

    /**
      * Return true if the given debug flag was specified in the query params
      */
    debugOn(tag: string): boolean {
        return this.debugFlags.get(tag);
    }

}
