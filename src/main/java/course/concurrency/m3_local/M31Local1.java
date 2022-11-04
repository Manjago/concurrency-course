package course.concurrency.m3_local;

import java.util.HashSet;
import java.util.Set;

public class M31Local1 {
    Set<Integer> set = new HashSet<>();

    public void update() {
        synchronized (set) {
            System.out.println("Wanna sleep");
            try {
                Thread.sleep(10000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("Interrupted!");
            }
            System.out.println("Stop sleep");
        }
    }
    public void clear() {
        set.clear();
        System.out.println("Clear done");
    }

    public static void main(String[] args) {

        final var instance = new M31Local1();

        final var t1 = new Thread(instance::update);

        final var t2 = new Thread(instance::clear);

        t1.start();
        t2.start();
    }
}
