package com.amazon.ata.advertising.service.targeting;

import com.amazon.ata.advertising.service.model.RequestContext;
import com.amazon.ata.advertising.service.targeting.predicate.TargetingPredicate;
import com.amazon.ata.advertising.service.targeting.predicate.TargetingPredicateResult;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Evaluates TargetingPredicates for a given RequestContext.
 */
public class TargetingEvaluator {
    public static final boolean IMPLEMENTED_STREAMS = true;
    public static final boolean IMPLEMENTED_CONCURRENCY = true;
    private final RequestContext requestContext;

    /**
     * Creates an evaluator for targeting predicates.
     * @param requestContext Context that can be used to evaluate the predicates.
     */

    public TargetingEvaluator(RequestContext requestContext) {
        this.requestContext = requestContext;
    }

    /**
     * Evaluate a TargetingGroup to determine if all of its TargetingPredicates are TRUE or not for the given
     * RequestContext.
     * @param targetingGroup Targeting group for an advertisement, including TargetingPredicates.
     * @return TRUE if all of the TargetingPredicates evaluate to TRUE against the RequestContext, FALSE otherwise.
     */
    public TargetingPredicateResult evaluate(TargetingGroup targetingGroup) {

        if (!requestContext.isRecognizedCustomer()) {
            return TargetingPredicateResult.FALSE;
        }

        List<TargetingPredicate> targetingPredicates = targetingGroup.getTargetingPredicates();

        ExecutorService executorService = Executors.newCachedThreadPool();
        List<Future<TargetingPredicateResult>> futureResults = new ArrayList<>();

        for (TargetingPredicate predicate : targetingPredicates) {
            futureResults.add(executorService.submit(() -> predicate.evaluate(requestContext)));
        }
        executorService.shutdown();

        for (Future<TargetingPredicateResult> resultFuture : futureResults) {
            try {
                if (!resultFuture.get().isTrue()) {
                    return TargetingPredicateResult.FALSE;
                }
            } catch (ExecutionException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        return TargetingPredicateResult.TRUE;
    }
}
