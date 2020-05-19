package pl.reactivesoftware.backpressure;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Emitter;
import io.reactivex.Flowable;
import io.reactivex.functions.BiConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static pl.reactivesoftware.backpressure.Threads.runInBackground;

public class Emitters {
    public static Flowable<Integer> coldIntegerPublisher() {
        return Flowable.generate(
                () -> new AtomicInteger(0),
                (AtomicInteger init, Emitter<Integer> emitter) -> emitter.onNext(init.incrementAndGet())
        );
    }

    public static Flowable<Integer> brokenEmitter() {
        final Callable<AtomicInteger> initialState = () -> new AtomicInteger(0);
        final BiConsumer<AtomicInteger, Emitter<Integer>> generator = (AtomicInteger init, Emitter<Integer> emitter) -> {
            int value = init.incrementAndGet();
            if (value == 2) {
                logger.info("Nothing!");
            } else {
                logger.info("Emit value: {}", value);
                emitter.onNext(value);
            }
        };
        return Flowable.generate(
                initialState,
                generator
        );
    }

    public static <T> Flowable<String> brokenAsyncEmitter(T request) {
        return Flowable.create(emitter -> {
            final AsynClient asynClient = new AsynClient();
            final AsyncTask<String> asyncTask = asynClient.send(request);
            asyncTask.addCallback(value -> {
                logger.info("Got response {}", value);
                emitter.onNext(value);
                emitter.onComplete();
            });
        }, BackpressureStrategy.BUFFER);
    }

    public static <T> Flowable<String> cancelableAsyncEmitter(T request) {
        final AsynClient asynClient = new AsynClient();
        return Flowable.create(emitter -> {
            final AsyncTask<String> asyncTask = asynClient.send(request);
            asyncTask.addCallback(value -> {
                logger.info("Got response {}", value);
                emitter.onNext(value);
                emitter.onComplete();
            });
            emitter.setCancellable(asyncTask::cancel);
        }, BackpressureStrategy.BUFFER);
    }

    private static final Logger logger = LoggerFactory.getLogger(Emitters.class);

    static class AsyncTask<T> {
        private volatile Consumer<T> callback;

        private volatile Thread thread;

        void addCallback(Consumer<T> success) {
            this.callback = success;
        }

        void cancel() {
            thread.interrupt();
        }

        public void setThread(Thread thread) {
            this.thread = thread;
        }
    }

    static class AsynClient {
        <T> AsyncTask<String> send(T request) {
            final AsyncTask<String> asyncTask = new AsyncTask<>();
            asyncTask.setThread(runInBackground("async-client-" + request, () -> {
                        logger.info("Start processing request {}", request);
                        Sleeper.sleep(Duration.ofMillis(500), Duration.ofMillis(200));
                        asyncTask.callback.accept(request.toString().toUpperCase());
                    }
            ));
            return asyncTask;
        }
    }
}
