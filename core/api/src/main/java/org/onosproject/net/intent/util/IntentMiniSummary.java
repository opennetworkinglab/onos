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
package org.onosproject.net.intent.util;

import org.onlab.util.Tools;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.intent.IntentState;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.google.common.base.MoreObjects.firstNonNull;

/**
 * Lists the summary of intents and their states.
 */
public final class IntentMiniSummary {

    private String intentType;
    private int total = 0;
    private int installReq = 0;
    private int compiling = 0;
    private int installing = 0;
    private int installed = 0;
    private int recompiling = 0;
    private int withdrawReq = 0;
    private int withdrawing = 0;
    private int withdrawn = 0;
    private int failed = 0;
    private int unknownState = 0;

    IntentMiniSummary(Intent intent, IntentService intentService) {
        // remove "Intent" from intentType label
        this.intentType = intentType(intent);
        update(intentService.getIntentState(intent.key()));
    }

    IntentMiniSummary(String intentType) {
        // remove "Intent" from intentType label
        this.intentType = intentType;
    }

    public IntentMiniSummary() {

    }

    private static String intentType(Intent intent) {
        return intent.getClass().getSimpleName().replace("Intent", "");
    }

    /**
     * Returns intent Type.
     * @return intentType
     */
    public String getIntentType() {
        return intentType;
    }

    /**
     * Returns total intent count.
     * @return total
     */
    public int getTotal() {
        return total;
    }

    /**
     * Returns InstallReq intent count.
     * @return InstallReq
     */
    public int getInstallReq() {
        return installReq;
    }

    /**
     * Returns Compiling intent count.
     * @return Compiling
     */
    public int getCompiling() {
        return compiling;
    }

    /**
     * Returns Installing intent count.
     * @return Installing
     */
    public int getInstalling() {
        return installing;
    }

    /**
     * Returns Installed intent count.
     * @return Installed
     */
    public int getInstalled() {
        return installed;
    }

    /**
     * Returns Recompiling intent count.
     * @return Recompiling
     */
    public int getRecompiling() {
        return recompiling;
    }

    /**
     * Returns WithdrawReq intent count.
     * @return WithdrawReq
     */
    public int getWithdrawReq() {
        return withdrawReq;
    }

    /**
     * Returns Withdrawing intent count.
     * @return Withdrawing
     */
    public int getWithdrawing() {
        return withdrawing;
    }

    /**
     * Returns Withdrawn intent count.
     * @return Withdrawn
     */
    public int getWithdrawn() {
        return withdrawn;
    }

    /**
     * Returns Failed intent count.
     * @return Failed
     */
    public int getFailed() {
        return failed;
    }

    /**
     * Returns unknownState intent count.
     * @return unknownState
     */
    public int getUnknownState() {
        return unknownState;
    }

    /**
     * Updates the Intent Summary.
     *
     * @param intentState the state of the intent
     */
    public void update(IntentState intentState) {
        total++;
        switch (intentState) {
            case INSTALL_REQ:
                installReq++;
                break;
            case COMPILING:
                compiling++;
                break;
            case INSTALLING:
                installing++;
                break;
            case INSTALLED:
                installed++;
                break;
            case RECOMPILING:
                recompiling++;
                break;
            case WITHDRAW_REQ:
                withdrawReq++;
                break;
            case WITHDRAWING:
                withdrawing++;
                break;
            case WITHDRAWN:
                withdrawn++;
                break;
            case FAILED:
                failed++;
                break;
            default:
                unknownState++;
                break;
        }
    }

    /**
     * Build summary of intents per intent type.
     *
     * @param intents to summarize
     * @param intentService to get IntentState
     * @return summaries per Intent type
     */
    public Map<String, IntentMiniSummary> summarize(Iterable<Intent> intents, IntentService intentService) {
        Map<String, List<Intent>> perIntent = Tools.stream(intents)
                .collect(Collectors.groupingBy(IntentMiniSummary::intentType));

        List<IntentMiniSummary> collect = perIntent.values().stream()
                .map(il ->
                        il.stream()
                                .map(intent -> new IntentMiniSummary(intent, intentService))
                                .reduce(new IntentMiniSummary(), this::merge)
                ).collect(Collectors.toList());

        Map<String, IntentMiniSummary> summaries = new HashMap<>();

        // individual
        collect.forEach(is -> summaries.put(is.intentType, is));

        // all summarised
        summaries.put("All", collect.stream()
                .reduce(new IntentMiniSummary("All"), this::merge));
        return summaries;
    }

    /**
     * Merges 2 {@link IntentMiniSummary} together.
     *
     * @param a element to merge
     * @param b element to merge
     * @return merged {@link IntentMiniSummary}
     */
    IntentMiniSummary merge(IntentMiniSummary a, IntentMiniSummary b) {
        IntentMiniSummary m = new IntentMiniSummary(firstNonNull(a.getIntentType(), b.getIntentType()));
        m.total = a.total + b.total;
        m.installReq = a.installReq + b.installReq;
        m.compiling = a.compiling + b.compiling;
        m.installing = a.installing + b.installing;
        m.installed = a.installed + b.installed;
        m.recompiling = a.recompiling + b.recompiling;
        m.withdrawing = a.withdrawing + b.withdrawing;
        m.withdrawReq = a.withdrawReq + b.withdrawReq;
        m.withdrawn = a.withdrawn + b.withdrawn;
        m.failed = a.failed + b.failed;
        m.unknownState = a.unknownState + b.unknownState;
        return m;
    }
}
