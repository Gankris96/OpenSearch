/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

package org.opensearch.search.deciders;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.search.BooleanClause;
import org.opensearch.common.settings.ClusterSettings;
import org.opensearch.index.IndexSettings;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilderVisitor;
import org.opensearch.search.internal.SearchContext;

import java.util.ArrayList;
import java.util.Collection;

/**
 * A composite {@link ConcurrentSearchDecider} combining the concurrent search "decision" from multiple
 * {@link ConcurrentSearchDecider} implementations into a boolean decision
 * <br/>
 * Contains the {@link ConcurrentSearchDecisionVisitor} class that implements the {@link QueryBuilderVisitor} pattern
 * to traverse the query tree and collect {@link ConcurrentSearchDecision} from registered deciders.
 * All collected decisions are combined by the
 * {@link ConcurrentSearchDeciders#getConcurrentSearchDecisionForSearchRequest()} method
 */
public class ConcurrentSearchDeciders {
    private static final Logger logger = LogManager.getLogger(ConcurrentSearchDeciders.class);

    private Collection<ConcurrentSearchDecision> concurrentSearchDecisions = new ArrayList<>();;

    private Collection<ConcurrentSearchDecider> concurrentSearchDeciders;

    private ConcurrentSearchDecisionVisitor concurrentSearchDecisionVisitor;

    private SearchContext searchContext;
    private ClusterSettings clusterSettings;
    private IndexSettings indexSettings;

    public ConcurrentSearchDeciders(
        Collection<ConcurrentSearchDecider> concurrentSearchDeciders,
        SearchContext searchContext,
        ClusterSettings clusterSettings,
        IndexSettings indexSettings
    ) {
        this.concurrentSearchDeciders = concurrentSearchDeciders;
        this.concurrentSearchDecisionVisitor = new ConcurrentSearchDecisionVisitor();
        this.searchContext = searchContext;
        this.indexSettings = indexSettings;
        this.clusterSettings = clusterSettings;
    }

    /**
     * Returns the boolean result combining the results of all registered {@link ConcurrentSearchDecider} implementations
     * using the {@link ConcurrentSearchDecision#getCompositeDecision} method
     * <br/>
     * Decision is collected in the following fashion -<br/>
     * 1. Filter out deciders that opt-out of the decision-making process<br/>
     * 2. Collect the decision from the {@link DefaultConcurrentSearchDecider}<br/>
     * 3. Collect the decision for any other remaining deciders.<br/>
     *
     */
    public boolean getConcurrentSearchDecisionForSearchRequest() {

        Collection<ConcurrentSearchDecider> decidersToConsider = new ArrayList<>(concurrentSearchDeciders);

        // if the search request is on an index that certain deciders don't care about,
        // those deciders can opt out of decision-making process. We can filter out such deciders
        concurrentSearchDeciders.forEach(concurrentSearchDecider -> {
            if (concurrentSearchDecider.optOutOfDecisionMakingForIndex(indexSettings)) {
                if (logger.isDebugEnabled()) {
                    logger.info("{} decider opt out of decision making", concurrentSearchDecider.getClass());
                }
                decidersToConsider.remove(concurrentSearchDecider);
            }
            if (concurrentSearchDecider instanceof DefaultConcurrentSearchDecider) {
                ConcurrentSearchDecision decision = concurrentSearchDecider.getConcurrentSearchDecisionFromOperationType(
                    searchContext,
                    indexSettings,
                    clusterSettings,
                    null
                );
                concurrentSearchDecisions.add(decision);
                decidersToConsider.remove(concurrentSearchDecider);
            }
        });

        // only need to register the deciders that are interested in query type
        this.concurrentSearchDecisionVisitor.setConcurrentSearchDeciders(decidersToConsider);

        // we know that the DefaultConcurrentSearchDecider in core will always be part of the decidersToConsider,
        // but we need to parse the query only if plugin deciders are involved
        if (decidersToConsider.size() > 0) {
            if (searchContext.request().source() != null && searchContext.request().source().query() != null) {
                QueryBuilder queryBuilder = searchContext.request().source().query();
                queryBuilder.visit(this.concurrentSearchDecisionVisitor);
            }

        }

        return ConcurrentSearchDecision.getCompositeDecision(concurrentSearchDecisions) == ConcurrentSearchDecision.TRUE ? true : false;
    }

    /**
     * Class to traverse the QueryBuilder tree and capture
     * {@link ConcurrentSearchDecision} at each node of the query tree
     */
    class ConcurrentSearchDecisionVisitor implements QueryBuilderVisitor {

        Collection<ConcurrentSearchDecider> concurrentSearchDeciders;

        public ConcurrentSearchDecisionVisitor() {

        }

        /**
         * Set the required {@link Collection<ConcurrentSearchDecider>} for decision-making as part of traversal
         */
        public void setConcurrentSearchDeciders(Collection<ConcurrentSearchDecider> concurrentSearchDeciders) {
            this.concurrentSearchDeciders = concurrentSearchDeciders;
        }

        @Override
        public void accept(QueryBuilder qb) {
            // if we already have a false in our decider, stop calling external decision makers
            if (concurrentSearchDecisions.contains(ConcurrentSearchDecision.FALSE)) {
                return;
            }
            if (concurrentSearchDeciders != null) {
                concurrentSearchDeciders.forEach(concurrentSearchDecider -> {
                    ConcurrentSearchDecision decision = concurrentSearchDecider.getConcurrentSearchDecisionFromOperationType(
                        searchContext,
                        indexSettings,
                        clusterSettings,
                        qb
                    );
                    if (logger.isDebugEnabled()) {
                        logger.debug("{} decider returned decision {}", concurrentSearchDecider.getClass(), decision.toString());
                    }
                    concurrentSearchDecisions.add(decision);
                });
            }
        }

        @Override
        public QueryBuilderVisitor getChildVisitor(BooleanClause.Occur occur) {
            return this;
        }
    }

}
