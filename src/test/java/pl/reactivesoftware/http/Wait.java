package pl.reactivesoftware.http;

import java.util.Objects;

public class Wait {
    public Wait() {
    }

    public Wait(Integer time) {
        this.time = time;
    }

    private Integer time;

    public Integer getTime() {
        return time;
    }

    public void setTime(Integer time) {
        this.time = time;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Wait wait = (Wait) o;
        return Objects.equals(time, wait.time);
    }

    @Override
    public int hashCode() {

        return Objects.hash(time);
    }

    @Override
    public String toString() {
        return "Wait{" +
                "time=" + time +
                '}';
    }
}
