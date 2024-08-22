/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.search.deciders;

import org.opensearch.common.settings.ClusterSettings;
import org.opensearch.index.IndexSettings;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.search.internal.SearchContext;

/**
 * Implementation of the {@link ConcurrentSearchDecider} for making concurrent search decision within
 * opensearch core
 */
public class DefaultConcurrentSearchDecider extends ConcurrentSearchDecider {

    public DefaultConcurrentSearchDecider() {

    }

    /**
     * Returns a {@link ConcurrentSearchDecision} based on whether the query contains a
     * supported aggregation for concurrent search.
     */
    @Override
    public ConcurrentSearchDecision getConcurrentSearchDecisionFromOperationType(
        SearchContext searchContext,
        IndexSettings indexSettings,
        ClusterSettings clusterSettings,
        QueryBuilder queryBuilder
    ) {
        ConcurrentSearchDecision decision = ConcurrentSearchDecision.NOOP;
        decision.setDecisionReason("default noop");

        if (searchContext.aggregations() != null) {
            if (searchContext.canEnableConcurrentSearch()) {
                decision = ConcurrentSearchDecision.TRUE;
                decision.setDecisionReason("supported aggregation operation for concurrent search");
            } else {
                decision = ConcurrentSearchDecision.FALSE;
                decision.setDecisionReason("unsupported aggregation operation for concurrent search");
            }

        }
        return decision;
    }

    /**
     * Core decider does not opt out of decision-making
     */
    @Override
    public boolean optOutOfDecisionMakingForIndex(IndexSettings indexSettings) {
        return false;
    }

}
