package pl.reactivesoftware.multicast;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Scheduler;
import io.reactivex.Single;
import io.reactivex.flowables.ConnectableFlowable;
import io.reactivex.schedulers.Schedulers;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.reactivesoftware.backpressure.Sleeper;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.junit.Assert.assertEquals;
import static pl.reactivesoftware.backpressure.Emitters.coldIntegerPublisher;

public class MulticastTest {

    @Test
    public void should_start_publish_after_connect_operator() {
        var counter = new AtomicInteger();
        ConnectableFlowable<Integer> published = coldIntegerPublisher()
                .take(3)
                .doOnSubscribe(x -> {
                    logger.info("Subscribed");
                    counter.incrementAndGet();
                })
                .publish();

        published.subscribe(x -> logger.info("A: {}", x));
        published.subscribe(x -> logger.info("B: {}", x));

        logger.info("Subscribers are waiting for connect operator");

        published.connect();

        assertEquals(1, counter.get());
    }

    @Test
    public void refCount_is_hot_producer() throws InterruptedException {
        var counter = new AtomicInteger();

        Flowable<Integer> lazy = coldIntegerPublisher()
                .take(10)
                .concatMap(x -> Flowable.just(x).delay(200, MILLISECONDS))
                .doOnSubscribe(x -> {
                    logger.info("Subscribed");
                    counter.incrementAndGet();
                })
                .publish()
                .refCount(); // == share()

        Sleeper.sleep(Duration.ofMillis(200));
        lazy.subscribeOn(scheduler).take(3).subscribe(x -> logger.info("A: {}", x));

        Sleeper.sleep(Duration.ofMillis(500));
        lazy.subscribeOn(scheduler).take(5).subscribe(x -> logger.info("B: {}", x));

        lazy.test().await().assertComplete();
        assertEquals(1, counter.get());
    }

    @Test
    public void share_is_hot_producer() throws InterruptedException {
        AtomicInteger counter = new AtomicInteger();

        Flowable<Integer> lazy = coldIntegerPublisher()
                .take(10)
                .concatMap(x -> Flowable.just(x).delay(200, MILLISECONDS))
                .doOnSubscribe(x -> {
                    logger.info("Subscribed");
                    counter.incrementAndGet();
                })
                .share();

        Sleeper.sleep(Duration.ofMillis(200));
        lazy.subscribeOn(scheduler).take(3).subscribe(x -> logger.info("A: {}", x));

        Sleeper.sleep(Duration.ofMillis(500));
        lazy.subscribeOn(scheduler).take(5).subscribe(x -> logger.info("B: {}", x));

        lazy.test().await().assertComplete();
        assertEquals(1, counter.get());
    }

    @Test
    public void how_to_count_all_elements() {
        AtomicInteger counter = new AtomicInteger();

        final var lazy = coldIntegerPublisher()
                .doOnSubscribe(x -> {
                    logger.info("Subscribed");
                    counter.incrementAndGet();
                })
                .take(10)
                .publish(integerFlowable -> {
                    var strangeNumber = 0;
                    final Single<Long> count = integerFlowable.count();
                    final Completable consumeOnlyEven = consumeOnlyEven(integerFlowable);
                    return count.zipWith(consumeOnlyEven.toSingleDefault(strangeNumber), (c, integer) -> c).toFlowable();
                })
                .doOnNext(x -> logger.info("x = {}", x));

        lazy.test()
                .assertValue((long) 10)
                .assertComplete();
        assertEquals(1, counter.get());
    }

    private Completable consumeOnlyEven(Flowable<Integer> integerFlowable) {
        return Completable.fromPublisher(integerFlowable.filter(number -> number % 2 == 0)
                .doOnNext(x -> logger.info("Consume {}", x)))
                .doOnComplete(() -> logger.info("I ate everything"));
    }


    private final Scheduler scheduler = Schedulers.from(Executors.newFixedThreadPool(2, SimpleThreadFactory.of("thread")));
    private final Logger logger = LoggerFactory.getLogger(MulticastTest.class);
}