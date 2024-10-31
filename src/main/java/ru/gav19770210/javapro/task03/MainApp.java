package ru.gav19770210.javapro.task03;

import lombok.SneakyThrows;

public class MainApp {
    @SneakyThrows
    public static void main(String[] args) {
        CustomThreadPool customThreadPool = new CustomThreadPool(2);

        Runnable runnable = () -> {
            System.out.println(Thread.currentThread().getName() + " -> run");
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                System.out.println(Thread.currentThread().getName() + ": " + e.getMessage());
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                System.out.println(Thread.currentThread().getName() + e.getMessage());
            }
            System.out.println(Thread.currentThread().getName() + " -> done");
        };

        for (int i = 0; i < 8; i++) {
            customThreadPool.execute(runnable);
        }

        Thread.sleep(2500);

        System.out.println("shutdown");

        customThreadPool.shutdown();
        //customThreadPool.shutdownNow();

        customThreadPool.execute(runnable);

        System.out.println("isShutdown = " + customThreadPool.isShutdown());
        System.out.println("isTerminated = " + customThreadPool.isTerminated());
    }
}
