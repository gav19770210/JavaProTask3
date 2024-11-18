package ru.gav19770210.javapro.task03;

import java.util.*;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/*
Попробуйте реализовать собственный пул потоков.
В качестве аргументов конструктора пулу передается его емкость (количество рабочих потоков).
Как только пул создан, он сразу инициализирует и запускает потоки.
Внутри пула очередь задач на исполнение организуется через LinkedList<Runnable>.
При выполнении у пула потоков метода execute(Runnable), указанная задача должна попасть в очередь исполнения,
и как только появится свободный поток – должна быть выполнена.
Также необходимо реализовать метод shutdown(), после выполнения которого новые задачи больше не принимаются пулом
(при попытке добавить задачу можно бросать IllegalStateException),
и все потоки для которых больше нет задач завершают свою работу.
Дополнительно можно добавить метод awaitTermination() без таймаута, работающий аналогично стандартным пулам потоков.
 */
public class CustomThreadPool extends AbstractExecutorService {
    private static final int RUNNING = 1;
    private static final int SHUTDOWN = 1 << 2;
    private static final int TERMINATED = 1 << 3;
    private final Deque<Runnable> workQueue = new LinkedList<>();
    private final HashSet<Thread> taskWorkers = new HashSet<>();
    private volatile int runState;

    public CustomThreadPool(int corePoolSize) {
        if (corePoolSize < 0)
            throw new IllegalArgumentException();
        this.runState = RUNNING;
        ThreadFactory threadFactory = Executors.defaultThreadFactory();
        for (int i = 0; i < corePoolSize; i++) {
            taskWorkers.add(threadFactory.newThread(new TaskWorker()));
        }
        taskWorkers.forEach(Thread::start);
    }

    private boolean runStateOf(int c) {
        return ((this.runState & c) == c);
    }

    @Override
    public void execute(Runnable command) {
        if (runStateOf(RUNNING)) {
            synchronized (workQueue) {
                workQueue.offer(command);
                workQueue.notifyAll();
            }
        } else {
            throw new IllegalStateException("Состояние пула не допускает добавление новых задач.");
        }
    }

    @Override
    public void shutdown() {
        this.runState = SHUTDOWN;
    }

    @Override
    public List<Runnable> shutdownNow() {
        this.runState = TERMINATED;
        taskWorkers.forEach(Thread::interrupt);

        return new ArrayList<>(workQueue);
    }

    @Override
    public boolean isShutdown() {
        return runStateOf(SHUTDOWN) | runStateOf(TERMINATED);
    }

    @Override
    public boolean isTerminated() {
        return runStateOf(TERMINATED);
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) {
        return false;
    }

    private Runnable getTask() {
        Runnable r = null;
        synchronized (workQueue) {
            if (!workQueue.isEmpty()) {
                r = workQueue.poll();
            } else {
                try {
                    workQueue.wait();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return r;
    }

    private final class TaskWorker implements Runnable {
        public void run() {
            Runnable task;
            while (runStateOf(RUNNING)) {
                task = getTask();
                if (task != null)
                    task.run();
            }
        }
    }
}
