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
 * {@link ConcurrentSearchDecider} is an abstract base class that allows to make decision
 * on whether to run a search request in concurrent fashion based on
 * the index and cluster settings along with search context and queryBuilder on a per shard search request basis.
 * Implementing deciders can also provide a way to opt out of decision-making
 * for certain requests based on index settings
 */
public abstract class ConcurrentSearchDecider {

    public ConcurrentSearchDecider() {

    }

    /**
     * Provide a decision whether concurrent search can be enabled for the search request based on operations in the
     * search request. The operation can be figured out from the provided search context and/or query builder
     * @return {@link ConcurrentSearchDecision}
     */
    public abstract ConcurrentSearchDecision getConcurrentSearchDecisionFromOperationType(
        SearchContext searchContext,
        IndexSettings indexSettings,
        ClusterSettings clusterSettings,
        QueryBuilder queryBuilder
    );

    /**
     * Provides a way for deciders to opt out of decision-making process for certain requests based on
     * index settings.
     */
    public abstract boolean optOutOfDecisionMakingForIndex(IndexSettings indexSettings);

}
