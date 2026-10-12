package com.computedeliver.orders.order;

import static org.assertj.core.api.Assertions.assertThat;

import com.computedeliver.orders.ApiTestSupport;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.support.TransactionTemplate;

class OrderConcurrencyTest extends ApiTestSupport {

    @Autowired
    OrderRepository orders;
    @Autowired
    TransactionTemplate tx;

    /** Cancel and advance both read a CONFIRMED order; whichever loses must get a 409, never a silent overwrite. */
    @RepeatedTest(5)
    void cancelRacingAdvanceYieldsOneSuccessAndOneConflict() throws Exception {
        long id = orderAt(ACME, "CONFIRMED");
        CountDownLatch go = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Callable<HttpStatus> cancel = () -> {
                go.await();
                return HttpStatus.valueOf(asClient(HttpMethod.POST, "/orders/" + id + "/cancel", null, ACME)
                        .getStatusCode().value());
            };
            Callable<HttpStatus> advance = () -> {
                go.await();
                return HttpStatus.valueOf(asOperator(HttpMethod.POST, "/orders/" + id + "/advance", null)
                        .getStatusCode().value());
            };
            Future<HttpStatus> c = pool.submit(cancel);
            Future<HttpStatus> a = pool.submit(advance);
            go.countDown();
            List<HttpStatus> results = new ArrayList<>(List.of(c.get(), a.get()));
            assertThat(results).containsExactlyInAnyOrder(HttpStatus.OK, HttpStatus.CONFLICT);

            String status = asOperator(HttpMethod.GET, "/orders/" + id, null).getBody().get("status").asText();
            assertThat(status).isEqualTo(c.get() == HttpStatus.OK ? "CANCELLED" : "PROCESSING");
            JsonNode history = asOperator(HttpMethod.GET, "/orders/" + id, null).getBody().get("history");
            assertThat(history).hasSize(3);
        } finally {
            pool.shutdownNow();
        }
    }

    /** Proves the @Version column itself, independent of the state checks: two stale copies cannot both commit. */
    @Test
    void staleWriteIsRejectedByOptimisticLocking() {
        long id = placeOrder(ACME);
        Order first = tx.execute(s -> detach(orders.findById(id).orElseThrow()));
        Order second = tx.execute(s -> detach(orders.findById(id).orElseThrow()));

        first.advanceTo(OrderStatus.CONFIRMED, java.time.Instant.now());
        tx.executeWithoutResult(s -> orders.saveAndFlush(first));

        second.cancel(java.time.Instant.now());
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> tx.executeWithoutResult(s -> orders.saveAndFlush(second)))
                .isInstanceOf(OptimisticLockingFailureException.class);
    }

    private Order detach(Order o) {
        o.getLines().size();
        o.getHistory().size();
        return o;
    }
}
