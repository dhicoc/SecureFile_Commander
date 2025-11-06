package com.example.securefile;

import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class TaskManager {
    private final ExecutorService pool;
    private final Map<Integer, Future<?>> futures = new ConcurrentHashMap<>();
    private final Map<Integer, AbstractTask> tasks = new ConcurrentHashMap<>();
    private final AtomicInteger idGen = new AtomicInteger(1);

    public TaskManager() {
        pool = Executors.newFixedThreadPool(Math.max(2, Runtime.getRuntime().availableProcessors()));
    }

    public int submitTask(AbstractTask task) {
        int id = idGen.getAndIncrement();
        tasks.put(id, task);
        Future<?> f = pool.submit(() -> {
            LogService.log("TASK_START", id + ":" + task.getName());
            try {
                task.run();
                LogService.log("TASK_COMPLETE", id + ":" + task.getName());
            } catch (Exception e) {
                LogService.log("TASK_ERROR", id + ":" + e.getMessage());
            } finally {
                tasks.remove(id);
                futures.remove(id);
            }
        });
        futures.put(id, f);
        return id;
    }

    public boolean cancelTask(int id) {
        Future<?> f = futures.get(id);
        if (f == null) return false;
        boolean ok = f.cancel(true);
        tasks.remove(id);
        futures.remove(id);
        LogService.log("TASK_CANCEL", String.valueOf(id));
        return ok;
    }

    public void printTasks() {
        if (tasks.isEmpty()) {
            System.out.println("(no active tasks)");
            return;
        }
        System.out.println("Active tasks:");
        tasks.forEach((id, task) -> {
            Future<?> f = futures.get(id);
            String status = (f == null) ? "UNKNOWN" : (f.isDone() ? "DONE" : (f.isCancelled() ? "CANCELLED":"RUNNING"));
            System.out.println("  " + id + " [" + status + "] " + task.getName());
        });
    }

    public void shutdown() {
        pool.shutdownNow();
    }
}