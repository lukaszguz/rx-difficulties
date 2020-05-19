package pl.reactivesoftware;

import io.reactivex.*;
import org.junit.Assert;
import org.junit.Test;

import java.util.NoSuchElementException;

public class TypesTest {

    @Test
    public void completable_return_success() {
        Completable.fromAction(() -> intValidator(9))
                .test()
                .assertComplete();
    }

    @Test
    public void completable_return_failure() {
        Completable.fromAction(() -> intValidator(11))
                .test()
                .assertError(IllegalStateException.class);
    }

    private void intValidator(Integer number) {
        if (number > 10) throw new IllegalStateException();
    }


    @Test
    public void single_return_success() {
        Single.just("event")
                .test()
                .assertValue("event")
                .assertComplete();
    }

    @Test
    public void single_return_failure() {
        Single.error(new IllegalStateException())
                .test()
                .assertError(IllegalStateException.class);
    }


    @Test
    public void maybe_return_success() {
        Maybe.just("event")
                .test()
                .assertValue("event")
                .assertComplete();
    }

    @Test
    public void maybe_return_failure() {
        Maybe.error(new IllegalStateException())
                .test()
                .assertError(IllegalStateException.class);
    }

    @Test
    public void maybe_return_empty() {
        Maybe.empty()
                .test()
                .assertComplete();

        Object result = Maybe.empty().blockingGet();
        Assert.assertNull(result);
    }

    @Test
    public void observable_return_success() {
        Observable.just("event1", "event2")
                .test()
                .assertValues("event1", "event2")
                .assertComplete();
    }

    @Test
    public void observable_return_failure() {
        Observable.error(new IllegalStateException())
                .test()
                .assertError(IllegalStateException.class);
    }

    @Test
    public void observable_return_empty() {
        Observable.empty()
                .test()
                .assertComplete();
        try {
            Observable.empty().blockingFirst();
        } catch (NoSuchElementException e) {
            System.out.println("Catch exception");
        }
    }

    @Test
    public void flowable_return_success() {
        Flowable.just("event1", "event2")
                .test()
                .assertValues("event1", "event2")
                .assertComplete();
    }

    @Test
    public void flowable_return_failure() {
        Flowable.error(new IllegalStateException())
                .test()
                .assertError(IllegalStateException.class);
    }

    @Test
    public void flowable_return_empty() {
        Flowable.empty()
                .test()
                .assertComplete();
        try {
            Flowable.empty().blockingFirst();
        } catch (NoSuchElementException e) {
            System.out.println("Catch exception");
        }
    }
}
