/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.search.deciders;

import java.util.Collection;

/**
 * This enumeration defines the possible types of decisions
 * that a {@link ConcurrentSearchDecider#getConcurrentSearchDecisionFromOperationType} can return.
 *
 */
public enum ConcurrentSearchDecision {
    FALSE(0),
    TRUE(1),
    NOOP(2);

    private final int id;
    private String decisionReason;

    ConcurrentSearchDecision(int id) {
        this.id = id;
    }

    ConcurrentSearchDecision(int id, String decisionReason) {
        this.id = id;
        this.decisionReason = decisionReason;
    }

    public String getDecisionReason() {
        return decisionReason;
    }

    public void setDecisionReason(String decisionReason) {
        this.decisionReason = decisionReason;
    }

    public int getId() {
        return id;
    }

    @Override
    public String toString() {
        return "ConcurrentSearchDecision{" + "id=" + id + ", decisionReason='" + decisionReason != null
            ? decisionReason
            : "not specified" + '\'' + '}';
    }

    public static ConcurrentSearchDecision getCompositeDecision(Collection<ConcurrentSearchDecision> allDecisions) {
        ConcurrentSearchDecision finalDecision = ConcurrentSearchDecision.NOOP;

        for (ConcurrentSearchDecision decision : allDecisions) {
            switch (decision) {
                case TRUE:
                    if (finalDecision == ConcurrentSearchDecision.FALSE) {
                        // TRUE AND FALSE = FALSE
                        return ConcurrentSearchDecision.FALSE;
                    }
                    finalDecision = ConcurrentSearchDecision.TRUE;
                    break;
                case FALSE:
                    if (finalDecision == ConcurrentSearchDecision.TRUE) {
                        // FALSE AND TRUE = FALSE
                        return ConcurrentSearchDecision.FALSE;
                    }
                    finalDecision = ConcurrentSearchDecision.FALSE;
                    break;
                case NOOP:
                    // NOOP doesn't change the final decision
                    break;
            }
        }
        // if at the end we still have final decision as noop, then final decision is false
        return finalDecision == ConcurrentSearchDecision.NOOP ? ConcurrentSearchDecision.FALSE : finalDecision;
    }

}
