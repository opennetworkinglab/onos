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

export interface TrieC {
    p: any;
    s: string[];
}

export interface TrieT {
    k: any;
    p: any;
    q: any;
}

export enum TrieRemoved {
    REMOVED = 'removed',
    ABSENT = 'absent'
}

export enum TrieInsert {
    ADDED = 'added',
    UPDATED = 'updated'
}

/**
 * Combine TrieRemoved and TrieInsert in to a union type
 */
export type TrieActions = TrieRemoved | TrieInsert;

export enum TrieOp {
    PLUS = '+',
    MINUS = '-'
}


export class Trie {
    p: any;
    w: string;
    s: string[];
    c: TrieC;
    t: TrieT[];
    x: number;
    f1: (TrieC) => TrieC;
    f2: () => TrieActions;
    data: any;


    constructor(
        op: TrieOp,
        trie: any,
        word: string,
        data?: any
    ) {
        this.p = trie;
        this.w = word.toUpperCase();
        this.s = this.w.split('');
        this.c = { p: this.p, s: this.s },
        this.t = [];
        this.x = 0;
        this.f1 = op === TrieOp.PLUS ? this.add : this.probe;
        this.f2 = op === TrieOp.PLUS ? this.insert : this.remove;
        this.data = data;
        while (this.c.s.length) {
            this.c = this.f1(this.c);
        }
    }

    add(cAdded: TrieC): TrieC {
        const q = cAdded.s.shift();
        let np = cAdded.p[q];

        if (!np) {
            cAdded.p[q] = {};
            np = cAdded.p[q];
            this.x = 1;
        }
        return { p: np, s: cAdded.s };
    }

    probe(cProbed: TrieC): TrieC {
        const q = cProbed.s.shift();
        const k: number = Object.keys(cProbed.p).length;
        const np = cProbed.p[q];

        this.t.push({ q: q, k: k, p: cProbed.p });
        if (!np) {
            this.t = [];
            return { p: [], s: [] };
        }
        return { p: np, s: cProbed.s };
    }

    insert(): TrieInsert {
        this.c.p._data = this.data;
        return this.x ? TrieInsert.ADDED : TrieInsert.UPDATED;
    }

    remove(): TrieRemoved {
        if (this.t.length) {
            this.t = this.t.reverse();
            while (this.t.length) {
                const d = this.t.shift();
                delete d.p[d.q];
                if (d.k > 1) {
                    this.t = [];
                }
            }
            return TrieRemoved.REMOVED;
        }
        return TrieRemoved.ABSENT;
    }
}
