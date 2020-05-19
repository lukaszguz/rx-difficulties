package pl.reactivesoftware.backpressure;

import io.reactivex.Flowable;
import io.reactivex.internal.functions.Functions;
import org.junit.Test;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

import static pl.reactivesoftware.backpressure.Emitters.brokenEmitter;
import static pl.reactivesoftware.backpressure.Emitters.coldIntegerPublisher;

public class BrokenEmitterBackpressureTest {

    @Test
    public void exceptionShouldBreakFlow() {
        Flowable.just("a", "b")
                .flatMap(x -> ("a".equals(x)) ? Flowable.just(x) : Flowable.error(new RuntimeException()))
                .test()
                .assertValue("a")
                .assertError(RuntimeException.class);
    }

    @Test
    public void brokenAsyncEmitter() throws InterruptedException {
        Flowable.just("a", "b")
                .doOnNext(x -> logger.info("Send {}", x))
                .flatMap(Emitters::brokenAsyncEmitter)
                .flatMap(x -> Flowable.error(new RuntimeException()))
                .subscribe(o -> {
                }, throwable -> logger.error("Got exception {}", throwable.getClass().getName()));

        Sleeper.sleep(Duration.ofSeconds(2));
    }

    @Test
    public void cancelableAsyncEmitter() {
        Flowable.just("a", "b")
                .doOnNext(x -> logger.info("Send {}", x))
                .flatMap(Emitters::cancelableAsyncEmitter)
                .flatMap(x -> Flowable.error(new RuntimeException()))
                .subscribe(o -> {
                }, throwable -> logger.error("Got exception {}", throwable.getClass().getName()));

        Sleeper.sleep(Duration.ofSeconds(2));
    }

    @Test
    public void should_never_complete() {
        brokenEmitter()
                .doOnNext(x -> logger.info("Got {}", x))
                .blockingSubscribe(new Subscriber<Integer>() {
                    @Override
                    public void onSubscribe(Subscription subscription) {
                        subscription.request(3);
                    }

                    @Override
                    public void onNext(Integer integer) {
                    }

                    @Override
                    public void onError(Throwable throwable) {

                    }

                    @Override
                    public void onComplete() {
                        logger.info("Complete");
                    }
                });
    }

    @Test
    public void group_by_trap() {
        coldIntegerPublisher()
                .take(385)
                .doOnNext(number -> logger.info("Emit {}", number))
                .groupBy(Functions.identity())
                .flatMap(groupedFlow -> groupedFlow)
                .blockingSubscribe(number -> logger.info("Subscriber got: {}", number));
    }


    //                .groupBy(Functions.identity(), Functions.identity(), false, 10)
//                .flatMap(groupedFlow -> groupedFlow, false, 1, 128)
    //    Each flatMap will request for one more element
//    groupByBufferSize + (flatMapMaxConcurrency * 2)

    private static final Logger logger = LoggerFactory.getLogger(BrokenEmitterBackpressureTest.class);
}