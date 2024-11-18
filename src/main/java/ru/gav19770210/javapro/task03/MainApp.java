package ru.gav19770210.javapro.task03;

import lombok.SneakyThrows;

import java.util.concurrent.atomic.AtomicInteger;

public class MainApp {
    private static final AtomicInteger taskCounter = new AtomicInteger(1);

    public static Runnable newTask(long sleep) {
        return () -> {
            var taskNum = taskCounter.getAndIncrement();
            System.out.printf(Thread.currentThread().getName() + ": task %s -> run%n", taskNum);
            try {
                Thread.sleep(sleep);
            } catch (InterruptedException e) {
                System.out.printf(Thread.currentThread().getName() + ": task %s: %s%n", taskNum, e.getMessage());
            }
            System.out.printf(Thread.currentThread().getName() + ": task %s -> done%n", taskNum);
        };
    }

    @SneakyThrows
    public static void main(String[] args) {
        var customThreadPool = new CustomThreadPool(2);

        for (int i = 0; i < 10; i++) {
            customThreadPool.execute(newTask(1000L));
        }

        Thread.sleep(2500);

        System.out.println("shutdown");
        customThreadPool.shutdown();
        //System.out.println("shutdownNow");
        //customThreadPool.shutdownNow();

        System.out.println("isShutdown = " + customThreadPool.isShutdown());
        System.out.println("isTerminated = " + customThreadPool.isTerminated());

        customThreadPool.execute(newTask(1000L));
    }
}
