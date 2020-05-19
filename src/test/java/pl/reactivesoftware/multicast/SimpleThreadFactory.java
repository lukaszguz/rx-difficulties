package pl.reactivesoftware.multicast;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class SimpleThreadFactory implements ThreadFactory {

    private final AtomicInteger counter = new AtomicInteger();
    private final String threadPoolName;

    private SimpleThreadFactory(String threadPoolName) {
        this.threadPoolName = threadPoolName;
    }

    public static ThreadFactory of(String threadPoolName) {
        return new SimpleThreadFactory(threadPoolName);
    }

    @Override
    public Thread newThread(@NotNull Runnable r) {
        return new Thread(r, threadPoolName + "-" + counter.incrementAndGet());
    }
}
