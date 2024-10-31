package ru.gav19770210.javapro.task03;

import lombok.Getter;

import java.util.*;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

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
    private static final int RUNNING = 0;
    private static final int SHUTDOWN = 1 << 1;
    private static final int STOP = 1 << 2;
    private static final int TERMINATED = 1 << 3;
    private final Deque<Runnable> workQueue = new LinkedList<>();
    private final HashSet<TaskWorker> taskWorkers = new HashSet<>();
    private final ReentrantLock mainLock = new ReentrantLock();
    private final Condition termination = mainLock.newCondition();
    private volatile int runState;
    @Getter
    private volatile ThreadFactory threadFactory = Executors.defaultThreadFactory();

    public CustomThreadPool(int corePoolSize) {
        if (corePoolSize < 0) throw new IllegalArgumentException();
        this.runState = RUNNING;
        for (int i = 0; i < corePoolSize; i++) {
            addTaskWorker();
        }
    }

    private boolean runStateOf(int c) {
        return (this.runState == c);
    }

    @Override
    public void execute(Runnable command) {
        if (runStateOf(RUNNING)) {
            final ReentrantLock mainLock = this.mainLock;
            mainLock.lock();
            try {
                workQueue.offer(command);
            } finally {
                mainLock.unlock();
            }
        } else {
            throw new IllegalStateException("Состояние пула не допускает добавление новых задач.");
        }
    }

    @Override
    public void shutdown() {
        this.runState = SHUTDOWN;

        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            for (TaskWorker taskWorker : taskWorkers) {
                Thread t = taskWorker.thread;
                if (!t.isInterrupted()) {
                    try {
                        //t.interrupt();
                        t.join();
                    } catch (InterruptedException | SecurityException e) {
                    }
                }
            }
        } finally {
            mainLock.unlock();
        }
    }

    @Override
    public List<Runnable> shutdownNow() {
        this.runState = STOP;

        for (TaskWorker taskWorker : taskWorkers)
            taskWorker.interruptIfStarted();

        return new ArrayList<>(taskWorkers);
    }

    @Override
    public boolean isShutdown() {
        return runStateOf(SHUTDOWN);
    }

    @Override
    public boolean isTerminated() {
        return runStateOf(TERMINATED);
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        long nanos = unit.toNanos(timeout);
        final ReentrantLock mainLock = this.mainLock;
        mainLock.lock();
        try {
            while (!runStateOf(TERMINATED)) {
                if (nanos <= 0L) return false;
                nanos = termination.awaitNanos(nanos);
            }
            return true;
        } finally {
            mainLock.unlock();
        }
    }

    private void addTaskWorker() {
        boolean workerStarted = false;
        boolean workerAdded = false;
        TaskWorker w = null;
        try {
            w = new TaskWorker();
            final Thread t = w.thread;
            if (t != null) {
                final ReentrantLock mainLock = this.mainLock;
                mainLock.lock();
                try {
                    taskWorkers.add(w);
                    workerAdded = true;
                } finally {
                    mainLock.unlock();
                }
                if (workerAdded) {
                    t.start();
                    workerStarted = true;
                }
            }
        } finally {
            if (!workerStarted)
                if (w != null)
                    taskWorkers.remove(w);
        }
    }

    private Runnable getTask() {
        Runnable r = null;
        if (!workQueue.isEmpty()) {
            final ReentrantLock mainLock = this.mainLock;
            mainLock.lock();
            try {
                r = workQueue.poll();
            } finally {
                mainLock.unlock();
            }
        }
        return r;
    }

    final void runTaskWorker(TaskWorker w) {
        Runnable task;
        while (runStateOf(RUNNING)) {
            try {
                task = getTask();
                if (task != null)
                    task.run();
            } finally {
                w.completedTasks++;
            }
        }
    }

    private final class TaskWorker implements Runnable {
        final Thread thread;

        volatile long completedTasks;

        TaskWorker() {
            this.thread = getThreadFactory().newThread(this);
        }

        public void run() {
            runTaskWorker(this);
        }

        void interruptIfStarted() {
            Thread t = thread;
            if (!t.isInterrupted()) {
                try {
                    t.interrupt();
                } catch (SecurityException e) {
                }
            }
        }
    }
}
