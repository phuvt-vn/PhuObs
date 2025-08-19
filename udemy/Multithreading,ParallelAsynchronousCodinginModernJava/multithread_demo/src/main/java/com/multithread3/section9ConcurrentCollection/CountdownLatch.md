# CountDownLatch - Hướng dẫn toàn diện

## 📚 Mục lục
1. [Khái niệm cơ bản](#khái-niệm-cơ-bản)
2. [Vấn đề khi không có CountDownLatch](#vấn-đề-khi-không-có-countdownlatch)
3. [Cách hoạt động của CountDownLatch](#cách-hoạt-động-của-countdownlatch)
4. [So sánh hiệu suất](#so-sánh-hiệu-suất)
5. [Best Practices](#best-practices)
6. [Use Cases thực tế](#use-cases-thực-tế)
7. [Advanced Patterns](#advanced-patterns)

---

## Khái niệm cơ bản

### CountDownLatch là gì?

**CountDownLatch** là một synchronization utility cho phép một hoặc nhiều thread chờ đợi cho đến khi một tập hợp các operations được thực hiện bởi các thread khác hoàn thành.

### Cách thức hoạt động:

```java
// 🎯 CƠ CHẾ HOẠT ĐỘNG CỦA COUNTDOWNLATCH

import java.util.concurrent.*;

public class CountDownLatchBasics {
    public static void main(String[] args) throws InterruptedException {
        
        // Bước 1: Tạo CountDownLatch với count = 3
        // Có nghĩa là cần 3 lần countDown() để mở "cổng"
        CountDownLatch latch = new CountDownLatch(3);
        
        System.out.println("=== MINH HỌA CƠ BẢN COUNTDOWNLATCH ===");
        System.out.println("Ban đầu count = " + latch.getCount());
        
        // Bước 2: Tạo 3 worker threads
        for (int i = 1; i <= 3; i++) {
            final int workerId = i;
            new Thread(() -> {
                try {
                    // Mô phỏng công việc mất thời gian
                    Thread.sleep(workerId * 1000);
                    System.out.println("Worker " + workerId + " hoàn thành công việc");
                    
                    // Bước 3: Báo hiệu hoàn thành bằng countDown()
                    latch.countDown();
                    System.out.println("Count sau khi worker " + workerId + " báo hiệu: " + latch.getCount());
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        }
        
        System.out.println("Main thread đang chờ tất cả workers hoàn thành...");
        
        // Bước 4: Main thread chờ đợi cho đến khi count = 0
        latch.await(); // BLOCK cho đến khi count = 0
        
        System.out.println("🎉 Tất cả workers đã hoàn thành! Main thread tiếp tục...");
        System.out.println("Count cuối cùng: " + latch.getCount());
    }
}
```

**Kết quả chạy:**
```
=== MINH HỌA CƠ BẢN COUNTDOWNLATCH ===
Ban đầu count = 3
Main thread đang chờ tất cả workers hoàn thành...
Worker 1 hoàn thành công việc
Count sau khi worker 1 báo hiệu: 2
Worker 2 hoàn thành công việc  
Count sau khi worker 2 báo hiệu: 1
Worker 3 hoàn thành công việc
Count sau khi worker 3 báo hiệu: 0
🎉 Tất cả workers đã hoàn thành! Main thread tiếp tục...
Count cuối cùng: 0
```

---

## Vấn đề khi không có CountDownLatch

### 1. Polling (Kiểm tra liên tục) - Không hiệu quả

```java
// ❌ CÁCH GIẢI QUYẾT KÔNG TỐT - POLLING

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class WithoutCountDownLatch {
    private static volatile AtomicInteger completedTasks = new AtomicInteger(0);
    private static final int TOTAL_TASKS = 5;
    
    public static void main(String[] args) throws InterruptedException {
        long startTime = System.currentTimeMillis();
        
        System.out.println("❌ CÁCH KHÔNG TỐT: Sử dụng polling");
        
        // Tạo 5 worker threads
        ExecutorService executor = Executors.newFixedThreadPool(5);
        
        for (int i = 1; i <= TOTAL_TASKS; i++) {
            final int taskId = i;
            executor.submit(() -> {
                try {
                    // Mô phỏng công việc
                    Thread.sleep(2000);
                    System.out.println("Task " + taskId + " hoàn thành");
                    
                    // Tăng counter
                    completedTasks.incrementAndGet();
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        
        // ❌ VẤN ĐỀ: Main thread phải liên tục kiểm tra (polling)
        System.out.println("Main thread bắt đầu polling...");
        while (completedTasks.get() < TOTAL_TASKS) {
            System.out.println("Đã hoàn thành: " + completedTasks.get() + "/" + TOTAL_TASKS);
            Thread.sleep(100); // ❌ Lãng phí CPU, không responsive
        }
        
        long endTime = System.currentTimeMillis();
        System.out.println("✅ Tất cả tasks hoàn thành!");
        System.out.println("⏱️ Thời gian chờ: " + (endTime - startTime) + "ms");
        System.out.println("🔥 VẤN ĐỀ: CPU bị lãng phí do polling liên tục!\n");
        
        executor.shutdown();
    }
}
```

### 2. Join() - Phức tạp và không linh hoạt

```java
// ❌ CÁCH KHÁC KHÔNG TỐT - SỬ DỤNG JOIN()

public class UsingJoinApproach {
    public static void main(String[] args) throws InterruptedException {
        long startTime = System.currentTimeMillis();
        
        System.out.println("❌ CÁCH KHÔNG TỐT: Sử dụng Thread.join()");
        
        // ❌ VẤN ĐỀ: Phải track tất cả threads manually
        Thread[] workers = new Thread[5];
        
        for (int i = 0; i < 5; i++) {
            final int taskId = i + 1;
            workers[i] = new Thread(() -> {
                try {
                    Thread.sleep(2000);
                    System.out.println("Task " + taskId + " hoàn thành");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            workers[i].start();
        }
        
        // ❌ VẤN ĐỀ: Phải join từng thread một
        System.out.println("Main thread đang join các worker threads...");
        for (Thread worker : workers) {
            worker.join(); // Block cho đến khi thread này hoàn thành
        }
        
        long endTime = System.currentTimeMillis();
        System.out.println("✅ Tất cả tasks hoàn thành!");
        System.out.println("⏱️ Thời gian chờ: " + (endTime - startTime) + "ms");
        System.out.println("🔥 VẤN ĐỀ: Phức tạp, không linh hoạt với ExecutorService!\n");
    }
}
```

### 3. Giải pháp tốt với CountDownLatch

```java
// ✅ CÁCH TỐT: SỬ DỤNG COUNTDOWNLATCH

public class WithCountDownLatch {
    public static void main(String[] args) throws InterruptedException {
        long startTime = System.currentTimeMillis();
        
        System.out.println("✅ CÁCH TỐT: Sử dụng CountDownLatch");
        
        // Tạo latch với count = 5 tasks
        CountDownLatch latch = new CountDownLatch(5);
        
        ExecutorService executor = Executors.newFixedThreadPool(5);
        
        for (int i = 1; i <= 5; i++) {
            final int taskId = i;
            executor.submit(() -> {
                try {
                    Thread.sleep(2000);
                    System.out.println("Task " + taskId + " hoàn thành");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    // ✅ QUAN TRỌNG: countDown() trong finally để đảm bảo luôn được gọi
                    latch.countDown();
                }
            });
        }
        
        System.out.println("Main thread chờ đợi với await()...");
        // ✅ HIỆU QUẢ: Block cho đến khi tất cả tasks hoàn thành
        latch.await();
        
        long endTime = System.currentTimeMillis();
        System.out.println("✅ Tất cả tasks hoàn thành!");
        System.out.println("⏱️ Thời gian chờ: " + (endTime - startTime) + "ms");
        System.out.println("🚀 ƯU ĐIỂM: Hiệu quả, sạch sẽ, không lãng phí CPU!");
        
        executor.shutdown();
    }
}
```

**So sánh kết quả:**
```
❌ Polling approach: 2000+ms (+ CPU overhead từ polling)
❌ Join approach: 2000+ms (+ complexity, không work với ExecutorService)  
✅ CountDownLatch: 2000ms (chính xác, hiệu quả, elegant)
```

---

## Cách hoạt động của CountDownLatch

### Anatomy của CountDownLatch

```java
// 🔍 PHÂN TÍCH SÂU VỀ COUNTDOWNLATCH

import java.util.concurrent.*;

public class CountDownLatchAnatomy {
    
    public static void main(String[] args) throws InterruptedException {
        demonstrateBasicOperations();
        demonstrateTimeoutAwait();
        demonstrateMultipleWaiters();
    }
    
    // 1. Các operations cơ bản
    private static void demonstrateBasicOperations() throws InterruptedException {
        System.out.println("=== 1. CÁC OPERATIONS CƠ BẢN ===");
        
        CountDownLatch latch = new CountDownLatch(3);
        
        // Kiểm tra count hiện tại
        System.out.println("Initial count: " + latch.getCount());
        
        // Giảm count
        latch.countDown();
        System.out.println("After 1st countDown: " + latch.getCount());
        
        latch.countDown();
        System.out.println("After 2nd countDown: " + latch.getCount());
        
        // ✅ QUAN TRỌNG: countDown() nhiều lần hơn không có tác dụng gì
        latch.countDown(); // count = 0
        latch.countDown(); // count vẫn = 0 (không âm)
        System.out.println("After multiple countDown: " + latch.getCount());
        
        // await() trả về ngay lập tức vì count = 0
        System.out.println("await() returns immediately since count = 0");
        latch.await();
        System.out.println("✅ await() completed\n");
    }
    
    // 2. Await với timeout
    private static void demonstrateTimeoutAwait() throws InterruptedException {
        System.out.println("=== 2. AWAIT VỚI TIMEOUT ===");
        
        CountDownLatch latch = new CountDownLatch(2);
        
        // Tạo thread làm việc chậm
        new Thread(() -> {
            try {
                Thread.sleep(3000); // 3 giây
                System.out.println("Slow worker finished");
                latch.countDown();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
        
        // ✅ TIMEOUT AWAIT: Chờ tối đa 1 giây
        System.out.println("Waiting with 1 second timeout...");
        boolean completed = latch.await(1, TimeUnit.SECONDS);
        
        if (completed) {
            System.out.println("✅ All tasks completed within timeout");
        } else {
            System.out.println("⏱️ Timeout! Some tasks still running. Count = " + latch.getCount());
        }
        
        // Cleanup: countDown để unblock potential waiters
        while (latch.getCount() > 0) {
            latch.countDown();
        }
        System.out.println();
    }
    
    // 3. Nhiều threads cùng chờ
    private static void demonstrateMultipleWaiters() throws InterruptedException {
        System.out.println("=== 3. NHIỀU THREADS CÙNG CHỜ ===");
        
        CountDownLatch latch = new CountDownLatch(1);
        
        // Tạo nhiều waiter threads
        for (int i = 1; i <= 3; i++) {
            final int waiterId = i;
            new Thread(() -> {
                try {
                    System.out.println("Waiter " + waiterId + " bắt đầu chờ...");
                    latch.await();
                    System.out.println("🚀 Waiter " + waiterId + " được giải phóng!");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        }
        
        // Cho các waiters thời gian để bắt đầu chờ
        Thread.sleep(1000);
        
        System.out.println("Triggering latch...");
        // ✅ MỘT LẦN countDown() sẽ giải phóng TẤT CẢ waiters
        latch.countDown();
        
        Thread.sleep(1000); // Cho thời gian để các waiters hoàn thành
        System.out.println();
    }
}
```

### Lifecycle của CountDownLatch

```java
// 📊 LIFECYCLE VÀ STATES CỦA COUNTDOWNLATCH

public class CountDownLatchLifecycle {
    
    public static void main(String[] args) throws InterruptedException {
        demonstrateLifecycle();
    }
    
    private static void demonstrateLifecycle() throws InterruptedException {
        System.out.println("=== LIFECYCLE CỦA COUNTDOWNLATCH ===");
        
        // State 1: INITIALIZED
        CountDownLatch latch = new CountDownLatch(3);
        System.out.println("📝 State: INITIALIZED - Count = " + latch.getCount());
        
        // State 2: COUNTING DOWN
        System.out.println("📝 State: COUNTING DOWN");
        
        ExecutorService executor = Executors.newFixedThreadPool(3);
        
        for (int i = 1; i <= 3; i++) {
            final int taskId = i;
            executor.submit(() -> {
                try {
                    // Simulate work
                    Thread.sleep(1000 * taskId);
                    System.out.println("Task " + taskId + " completed");
                    
                    // Decrement count
                    latch.countDown();
                    System.out.println("Count after task " + taskId + ": " + latch.getCount());
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        
        // Monitor state changes
        new Thread(() -> {
            try {
                while (latch.getCount() > 0) {
                    System.out.println("⏳ Monitoring - Current count: " + latch.getCount());
                    Thread.sleep(500);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
        
        // State 3: WAITING
        System.out.println("📝 State: WAITING - Main thread blocked on await()");
        latch.await();
        
        // State 4: RELEASED
        System.out.println("📝 State: RELEASED - Count = " + latch.getCount());
        System.out.println("✅ CountDownLatch lifecycle completed!");
        
        // ⚠️ QUAN TRỌNG: CountDownLatch KHÔNG THỂ reset
        System.out.println("⚠️ Latch cannot be reused - count stays at: " + latch.getCount());
        
        executor.shutdown();
    }
}
```

---

## So sánh hiệu suất

### Benchmark Test

```java
// 📊 BENCHMARK HIỆU SUẤT CÁC CÁCH SYNCHRONIZATION

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class SynchronizationBenchmark {
    
    private static final int TASK_COUNT = 10;
    private static final int TASK_DURATION_MS = 500;
    private static final int ITERATIONS = 5;
    
    public static void main(String[] args) throws InterruptedException {
        System.out.println("🏁 BENCHMARK: So sánh các cách đồng bộ hóa\n");
        
        // Warm up JVM
        warmUp();
        
        for (int i = 1; i <= ITERATIONS; i++) {
            System.out.println("=== ITERATION " + i + " ===");
            
            long pollingTime = benchmarkPollingApproach();
            long joinTime = benchmarkJoinApproach();
            long latchTime = benchmarkCountDownLatchApproach();
            long executorTime = benchmarkExecutorServiceApproach();
            
            System.out.printf("Kết quả iteration %d:\n", i);
            System.out.printf("  Polling:         %d ms (CPU intensive)\n", pollingTime);
            System.out.printf("  Thread.join():   %d ms (complex)\n", joinTime);
            System.out.printf("  CountDownLatch:  %d ms (efficient)\n", latchTime);
            System.out.printf("  ExecutorService: %d ms (high-level)\n", executorTime);
            System.out.println();
            
            Thread.sleep(1000); // Rest between iterations
        }
    }
    
    // ❌ Polling approach
    private static long benchmarkPollingApproach() throws InterruptedException {
        long startTime = System.currentTimeMillis();
        
        AtomicInteger completed = new AtomicInteger(0);
        ExecutorService executor = Executors.newFixedThreadPool(TASK_COUNT);
        
        // Submit tasks
        for (int i = 0; i < TASK_COUNT; i++) {
            executor.submit(() -> {
                try {
                    Thread.sleep(TASK_DURATION_MS);
                    completed.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        
        // ❌ Polling loop - waste CPU cycles
        while (completed.get() < TASK_COUNT) {
            Thread.sleep(10); // Check every 10ms
        }
        
        executor.shutdown();
        return System.currentTimeMillis() - startTime;
    }
    
    // ❌ Thread.join() approach  
    private static long benchmarkJoinApproach() throws InterruptedException {
        long startTime = System.currentTimeMillis();
        
        Thread[] threads = new Thread[TASK_COUNT];
        
        // Create and start threads
        for (int i = 0; i < TASK_COUNT; i++) {
            threads[i] = new Thread(() -> {
                try {
                    Thread.sleep(TASK_DURATION_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            threads[i].start();
        }
        
        // ❌ Join each thread
        for (Thread thread : threads) {
            thread.join();
        }
        
        return System.currentTimeMillis() - startTime;
    }
    
    // ✅ CountDownLatch approach
    private static long benchmarkCountDownLatchApproach() throws InterruptedException {
        long startTime = System.currentTimeMillis();
        
        CountDownLatch latch = new CountDownLatch(TASK_COUNT);
        ExecutorService executor = Executors.newFixedThreadPool(TASK_COUNT);
        
        // Submit tasks
        for (int i = 0; i < TASK_COUNT; i++) {
            executor.submit(() -> {
                try {
                    Thread.sleep(TASK_DURATION_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            });
        }
        
        // ✅ Efficient waiting
        latch.await();
        
        executor.shutdown();
        return System.currentTimeMillis() - startTime;
    }
    
    // 🚀 ExecutorService.awaitTermination() approach
    private static long benchmarkExecutorServiceApproach() throws InterruptedException {
        long startTime = System.currentTimeMillis();
        
        ExecutorService executor = Executors.newFixedThreadPool(TASK_COUNT);
        
        // Submit tasks
        for (int i = 0; i < TASK_COUNT; i++) {
            executor.submit(() -> {
                try {
                    Thread.sleep(TASK_DURATION_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        
        // Shutdown and wait
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);
        
        return System.currentTimeMillis() - startTime;
    }
    
    private static void warmUp() throws InterruptedException {
        // Warm up để JVM optimize code
        for (int i = 0; i < 3; i++) {
            CountDownLatch warmUpLatch = new CountDownLatch(5);
            for (int j = 0; j < 5; j++) {
                new Thread(() -> {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        warmUpLatch.countDown();
                    }
                }).start();
            }
            warmUpLatch.await();
        }
    }
}
```

**Kết quả benchmark điển hình:**
```
🏁 BENCHMARK: So sánh các cách đồng bộ hóa

=== ITERATION 1 ===
Kết quả iteration 1:
  Polling:         518 ms (CPU intensive)
  Thread.join():   502 ms (complex)
  CountDownLatch:  501 ms (efficient)
  ExecutorService: 503 ms (high-level)

=== ITERATION 5 ===
Kết quả iteration 5:
  Polling:         515 ms (CPU intensive)
  Thread.join():   500 ms (complex) 
  CountDownLatch:  500 ms (efficient)
  ExecutorService: 501 ms (high-level)
```

**Phân tích kết quả:**
- **Thời gian tương đương** nhưng **CPU usage khác nhau**
- **Polling**: Lãng phí CPU cycles do check liên tục
- **Thread.join()**: Phức tạp, không work với ExecutorService
- **CountDownLatch**: Hiệu quả, elegant, flexible
- **ExecutorService**: High-level nhưng ít control hơn

---

## Best Practices

### 1. Proper Usage Patterns

```java
// ✅ BEST PRACTICES CHO COUNTDOWNLATCH

import java.util.concurrent.*;

public class CountDownLatchBestPractices {
    
    // ✅ PATTERN 1: Always use try-finally
    public void properExceptionHandling() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(3);
        ExecutorService executor = Executors.newFixedThreadPool(3);
        
        for (int i = 0; i < 3; i++) {
            final int taskId = i;
            executor.submit(() -> {
                try {
                    // Công việc có thể throw exception
                    performRiskyTask(taskId);
                    System.out.println("Task " + taskId + " completed successfully");
                    
                } catch (Exception e) {
                    System.err.println("Task " + taskId + " failed: " + e.getMessage());
                    
                } finally {
                    // ✅ QUAN TRỌNG: Luôn countDown() trong finally
                    // Để đảm bảo main thread không bị block vĩnh viễn
                    latch.countDown();
                }
            });
        }
        
        latch.await();
        System.out.println("All tasks completed (success or failure)");
        executor.shutdown();
    }
    
    // ✅ PATTERN 2: Timeout để tránh deadlock
    public void useTimeoutToAvoidDeadlock() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(5);
        ExecutorService executor = Executors.newFixedThreadPool(5);
        
        for (int i = 0; i < 5; i++) {
            final int taskId = i;
            executor.submit(() -> {
                try {
                    // Task có thể bị stuck
                    performUnreliableTask(taskId);
                } finally {
                    latch.countDown();
                }
            });
        }
        
        // ✅ Sử dụng timeout để tránh chờ vĩnh viễn
        boolean completed = latch.await(30, TimeUnit.SECONDS);
        
        if (completed) {
            System.out.println("✅ All tasks completed within 30 seconds");
        } else {
            System.out.println("⚠️ Some tasks didn't complete within timeout");
            System.out.println("Remaining count: " + latch.getCount());
        }
        
        executor.shutdownNow(); // Force shutdown nếu timeout
    }
    
    // ✅ PATTERN 3: Validation và monitoring
    public void validationAndMonitoring() throws InterruptedException {
        int taskCount = 10;
        
        // ✅ Validate input
        if (taskCount <= 0) {
            throw new IllegalArgumentException("Task count must be positive");
        }
        
        CountDownLatch latch = new CountDownLatch(taskCount);
        ExecutorService executor = Executors.newFixedThreadPool(Math.min(taskCount, 5));
        
        // ✅ Monitor progress
        ScheduledExecutorService monitor = Executors.newScheduledThreadPool(1);
        monitor.scheduleAtFixedRate(() -> {
            long remaining = latch.getCount();
            long completed = taskCount - remaining;
            System.out.printf("Progress: %d/%d completed (%.1f%%)\n", 
                completed, taskCount, (completed * 100.0 / taskCount));
        }, 1, 1, TimeUnit.SECONDS);
        
        // Submit tasks
        for (int i = 0; i < taskCount; i++) {
            final int taskId = i;
            executor.submit(() -> {
                try {
                    Thread.sleep(2000 + (int)(Math.random() * 1000)); // Random duration
                    System.out.println("Task " + taskId + " completed");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await();
        monitor.shutdown();
        executor.shutdown();
        
        System.out.println("✅ All tasks completed with monitoring");
    }
    
    // ✅ PATTERN 4: Reusable coordination utility
    public static class TaskCoordinator {
        
        public static boolean awaitCompletion(Runnable... tasks) throws InterruptedException {
            return awaitCompletion(30, TimeUnit.SECONDS, tasks);
        }
        
        public static boolean awaitCompletion(long timeout, TimeUnit unit, Runnable... tasks) 
                throws InterruptedException {
            
            if (tasks.length == 0) return true;
            
            CountDownLatch latch = new CountDownLatch(tasks.length);
            ExecutorService executor = Executors.newFixedThreadPool(tasks.length);
            
            // Submit all tasks
            for (Runnable task : tasks) {
                executor.submit(() -> {
                    try {
                        task.run();
                    } finally {
                        latch.countDown();
                    }
                });
            }
            
            // Wait with timeout
            boolean completed = latch.await(timeout, unit);
            
            executor.shutdown();
            if (!completed) {
                executor.shutdownNow();
            }
            
            return completed;
        }
    }
    
    // ❌ ANTI-PATTERNS: Những điều cần tránh
    public void antiPatterns() {
        CountDownLatch latch = new CountDownLatch(3);
        
        // ❌ ANTI-PATTERN 1: Không handle InterruptedException
        /*
        try {
            latch.await();
        } catch (InterruptedException e) {
            // ❌ Bỏ qua exception
        }
        */
        
        // ❌ ANTI-PATTERN 2: countDown() trước khi task hoàn thành
        /*
        executor.submit(() -> {
            latch.countDown(); // ❌ countDown() quá sớm
            performLongRunningTask(); // Task thực tế ở đây
        });
        */
        
        // ❌ ANTI-PATTERN 3: Cố gắng reuse CountDownLatch
        /*
        latch.await(); // Lần đầu
        // latch.reset(); // ❌ Không có method reset()
        latch.await(); // ❌ Sẽ return ngay vì count = 0
        */
        
        // ❌ ANTI-PATTERN 4: Quên countDown() trong exception case
        /*
        executor.submit(() -> {
            try {
                riskyOperation();
                latch.countDown(); // ❌ Chỉ countDown() khi success
            } catch (Exception e) {
                // ❌ Không countDown() khi fail -> main thread bị block vĩnh viễn
            }
        });
        */
    }
    
    // Helper methods
    private void performRiskyTask(int taskId) throws Exception {
        Thread.sleep(1000);
        if (Math.random() < 0.3) { // 30% chance of failure
            throw new RuntimeException("Task " + taskId + " failed randomly");
        }
    }
    
    private void performUnreliableTask(int taskId) {
        try {
            Thread.sleep(1000);
            if (Math.random() < 0.1) { // 10% chance of getting stuck
                Thread.sleep(60000); // Stuck for 1 minute
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
```

### 2. Memory và Performance Optimization

```java
// 🚀 OPTIMIZATION TECHNIQUES

public class CountDownLatchOptimization {
    
    // ✅ Memory-efficient batch processing
    public void batchProcessingOptimization() throws InterruptedException {
        int totalTasks = 1000;
        int batchSize = 50; // Process in batches để tránh tạo quá nhiều threads
        
        for (int batch = 0; batch < totalTasks; batch += batchSize) {
            int currentBatchSize = Math.min(batchSize, totalTasks - batch);
            CountDownLatch batchLatch = new CountDownLatch(currentBatchSize);
            
            ExecutorService batchExecutor = Executors.newFixedThreadPool(
                Math.min(currentBatchSize, Runtime.getRuntime().availableProcessors())
            );
            
            for (int i = 0; i < currentBatchSize; i++) {
                final int taskId = batch + i;
                batchExecutor.submit(() -> {
                    try {
                        processTask(taskId);
                    } finally {
                        batchLatch.countDown();
                    }
                });
            }
            
            batchLatch.await();
            batchExecutor.shutdown();
            
            System.out.printf("Batch %d-%d completed\n", batch, batch + currentBatchSize - 1);
        }
        
        System.out.println("All batches completed");
    }
    
    // ✅ Resource pooling với CountDownLatch
    public static class ResourcePool<T> {
        private final BlockingQueue<T> resources;
        private final int capacity;
        
        public ResourcePool(int capacity) {
            this.capacity = capacity;
            this.resources = new ArrayBlockingQueue<>(capacity);
        }
        
        public void addResource(T resource) {
            resources.offer(resource);
        }
        
        public boolean waitForResourceAvailability(long timeout, TimeUnit unit) 
                throws InterruptedException {
            CountDownLatch latch = new CountDownLatch(1);
            
            // Monitor để resource availability
            Thread monitor = new Thread(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    if (!resources.isEmpty()) {
                        latch.countDown();
                        break;
                    }
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            });
            
            monitor.start();
            boolean available = latch.await(timeout, unit);
            monitor.interrupt();
            
            return available;
        }
        
        public T acquireResource() throws InterruptedException {
            return resources.take();
        }
        
        public void releaseResource(T resource) {
            resources.offer(resource);
        }
    }
    
    // ✅ Tiered synchronization với multiple latches
    public void tieredSynchronization() throws InterruptedException {
        System.out.println("=== TIERED SYNCHRONIZATION ===");
        
        // Phase 1: Initialization tasks
        CountDownLatch initLatch = new CountDownLatch(3);
        
        // Phase 2: Main processing tasks  
        CountDownLatch processLatch = new CountDownLatch(5);
        
        // Phase 3: Cleanup tasks
        CountDownLatch cleanupLatch = new CountDownLatch(2);
        
        ExecutorService executor = Executors.newFixedThreadPool(10);
        
        // Submit initialization tasks
        for (int i = 0; i < 3; i++) {
            final int taskId = i;
            executor.submit(() -> {
                try {
                    System.out.println("Init task " + taskId + " starting");
                    Thread.sleep(1000);
                    System.out.println("Init task " + taskId + " completed");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    initLatch.countDown();
                }
            });
        }
        
        // Wait for initialization to complete
        System.out.println("Waiting for initialization...");
        initLatch.await();
        System.out.println("✅ Initialization completed, starting main processing");
        
        // Submit main processing tasks
        for (int i = 0; i < 5; i++) {
            final int taskId = i;
            executor.submit(() -> {
                try {
                    System.out.println("Process task " + taskId + " starting");
                    Thread.sleep(2000);
                    System.out.println("Process task " + taskId + " completed");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    processLatch.countDown();
                }
            });
        }
        
        // Wait for main processing to complete
        System.out.println("Waiting for main processing...");
        processLatch.await();
        System.out.println("✅ Main processing completed, starting cleanup");
        
        // Submit cleanup tasks
        for (int i = 0; i < 2; i++) {
            final int taskId = i;
            executor.submit(() -> {
                try {
                    System.out.println("Cleanup task " + taskId + " starting");
                    Thread.sleep(500);
                    System.out.println("Cleanup task " + taskId + " completed");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    cleanupLatch.countDown();
                }
            });
        }
        
        // Wait for cleanup to complete
        System.out.println("Waiting for cleanup...");
        cleanupLatch.await();
        System.out.println("✅ All phases completed!");
        
        executor.shutdown();
    }
    
    private void processTask(int taskId) {
        try {
            Thread.sleep(100); // Simulate work
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
```

---

## Use Cases thực tế

### 1. Application Startup Coordination

```java
// 🚀 ĐIỀU PHỐI KHỞI ĐỘNG ỨNG DỤNG

@Service
public class ApplicationStartupCoordinator {
    
    private final Logger logger = LoggerFactory.getLogger(ApplicationStartupCoordinator.class);
    
    // Các service components cần khởi động
    private final List<StartupComponent> components;
    private final ExecutorService startupExecutor;
    
    public ApplicationStartupCoordinator() {
        this.components = Arrays.asList(
            new DatabaseConnectionComponent(),
            new CacheWarmupComponent(), 
            new MessageQueueComponent(),
            new SecurityComponent(),
            new MetricsComponent()
        );
        this.startupExecutor = Executors.newFixedThreadPool(components.size());
    }
    
    // ✅ Parallel startup với coordination
    public boolean startApplication(long timeoutSeconds) throws InterruptedException {
        logger.info("🚀 Starting application with {} components", components.size());
        
        // CountDownLatch để chờ tất cả components khởi động
        CountDownLatch startupLatch = new CountDownLatch(components.size());
        
        // Track failed components
        ConcurrentHashMap<String, Exception> failures = new ConcurrentHashMap<>();
        
        // Submit startup tasks
        for (StartupComponent component : components) {
            startupExecutor.submit(() -> {
                try {
                    logger.info("⏳ Starting component: {}", component.getName());
                    long startTime = System.currentTimeMillis();
                    
                    component.start();
                    
                    long duration = System.currentTimeMillis() - startTime;
                    logger.info("✅ Component {} started in {}ms", component.getName(), duration);
                    
                } catch (Exception e) {
                    logger.error("❌ Component {} failed to start: {}", component.getName(), e.getMessage());
                    failures.put(component.getName(), e);
                    
                } finally {
                    startupLatch.countDown();
                }
            });
        }
        
        // Monitor startup progress
        monitorStartupProgress(startupLatch);
        
        // Wait for all components với timeout
        boolean allStarted = startupLatch.await(timeoutSeconds, TimeUnit.SECONDS);
        
        if (allStarted && failures.isEmpty()) {
            logger.info("🎉 Application started successfully!");
            return true;
            
        } else if (!allStarted) {
            logger.error("⏱️ Application startup timeout! {} components still starting", 
                       startupLatch.getCount());
            return false;
            
        } else {
            logger.error("❌ Application startup failed! {} components failed:", failures.size());
            failures.forEach((name, ex) -> 
                logger.error("  - {}: {}", name, ex.getMessage()));
            return false;
        }
    }
    
    private void monitorStartupProgress(CountDownLatch latch) {
        Thread monitor = new Thread(() -> {
            int total = components.size();
            
            while (latch.getCount() > 0) {
                try {
                    int completed = total - (int) latch.getCount();
                    double percentage = (completed * 100.0) / total;
                    logger.info("📊 Startup progress: {}/{} components ({:.1f}%)", 
                              completed, total, percentage);
                    
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        
        monitor.setDaemon(true);
        monitor.start();
    }
    
    public void shutdown() {
        startupExecutor.shutdown();
        try {
            if (!startupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                startupExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            startupExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}

// Supporting interfaces và classes
interface StartupComponent {
    String getName();
    void start() throws Exception;
}

class DatabaseConnectionComponent implements StartupComponent {
    public String getName() { return "Database Connection"; }
    
    public void start() throws Exception {
        // Mô phỏng khởi động database connection
        Thread.sleep(3000);
        if (Math.random() < 0.1) { // 10% chance of failure
            throw new RuntimeException("Cannot connect to database");
        }
    }
}

class CacheWarmupComponent implements StartupComponent {
    public String getName() { return "Cache Warmup"; }
    
    public void start() throws Exception {
        // Mô phỏng cache warmup
        Thread.sleep(2000);
    }
}

class MessageQueueComponent implements StartupComponent {
    public String getName() { return "Message Queue"; }
    
    public void start() throws Exception {
        // Mô phỏng message queue setup
        Thread.sleep(1500);
    }
}

class SecurityComponent implements StartupComponent {
    public String getName() { return "Security"; }
    
    public void start() throws Exception {
        // Mô phỏng security initialization
        Thread.sleep(1000);
    }
}

class MetricsComponent implements StartupComponent {
    public String getName() { return "Metrics"; }
    
    public void start() throws Exception {
        // Mô phỏng metrics setup
        Thread.sleep(800);
    }
}
```

### 2. Parallel Data Processing Pipeline

```java
// 📊 PIPELINE XỬ LÝ DỮ LIỆU SONG SONG

@Service
public class DataProcessingPipeline {
    
    private final Logger logger = LoggerFactory.getLogger(DataProcessingPipeline.class);
    private final ExecutorService processingPool;
    
    public DataProcessingPipeline() {
        this.processingPool = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors()
        );
    }
    
    // ✅ Process large dataset với parallel chunks
    public ProcessingResult processDataset(Dataset dataset, int chunkSize) throws InterruptedException {
        logger.info("📊 Processing dataset with {} records in chunks of {}", 
                   dataset.size(), chunkSize);
        
        List<DataChunk> chunks = dataset.splitIntoChunks(chunkSize);
        CountDownLatch processingLatch = new CountDownLatch(chunks.size());
        
        // Kết quả processing từ các chunks
        ConcurrentHashMap<Integer, ChunkResult> results = new ConcurrentHashMap<>();
        AtomicInteger processedRecords = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        
        long startTime = System.currentTimeMillis();
        
        // Submit processing tasks cho từng chunk
        for (int i = 0; i < chunks.size(); i++) {
            final int chunkIndex = i;
            final DataChunk chunk = chunks.get(i);
            
            processingPool.submit(() -> {
                try {
                    logger.debug("🔄 Processing chunk {} with {} records", chunkIndex, chunk.size());
                    
                    ChunkResult result = processChunk(chunk, chunkIndex);
                    results.put(chunkIndex, result);
                    
                    processedRecords.addAndGet(result.getProcessedCount());
                    errorCount.addAndGet(result.getErrorCount());
                    
                    logger.debug("✅ Chunk {} completed: {} processed, {} errors", 
                               chunkIndex, result.getProcessedCount(), result.getErrorCount());
                    
                } catch (Exception e) {
                    logger.error("❌ Chunk {} failed: {}", chunkIndex, e.getMessage());
                    errorCount.addAndGet(chunk.size()); // Mark all as errors
                    
                } finally {
                    processingLatch.countDown();
                }
            });
        }
        
        // Monitor processing progress
        monitorProcessingProgress(processingLatch, chunks.size(), processedRecords, dataset.size());
        
        // Wait for all chunks to complete
        boolean completed = processingLatch.await(30, TimeUnit.MINUTES);
        long duration = System.currentTimeMillis() - startTime;
        
        if (!completed) {
            logger.error("⏱️ Processing timeout! {} chunks still processing", processingLatch.getCount());
            return new ProcessingResult(false, 0, 0, duration);
        }
        
        // Aggregate results
        ProcessingResult finalResult = aggregateResults(results, duration);
        
        logger.info("🎉 Dataset processing completed: {} processed, {} errors in {}ms", 
                   finalResult.getTotalProcessed(), finalResult.getTotalErrors(), duration);
        
        return finalResult;
    }
    
    private ChunkResult processChunk(DataChunk chunk, int chunkIndex) {
        int processed = 0;
        int errors = 0;
        
        for (DataRecord record : chunk.getRecords()) {
            try {
                // Simulate data processing
                validateRecord(record);
                transformRecord(record);
                persistRecord(record);
                
                processed++;
                
            } catch (Exception e) {
                logger.warn("Record processing failed in chunk {}: {}", chunkIndex, e.getMessage());
                errors++;
            }
        }
        
        return new ChunkResult(chunkIndex, processed, errors);
    }
    
    private void monitorProcessingProgress(CountDownLatch latch, int totalChunks, 
                                         AtomicInteger processedRecords, int totalRecords) {
        Thread monitor = new Thread(() -> {
            while (latch.getCount() > 0) {
                try {
                    int completedChunks = totalChunks - (int) latch.getCount();
                    double chunkProgress = (completedChunks * 100.0) / totalChunks;
                    double recordProgress = (processedRecords.get() * 100.0) / totalRecords;
                    
                    logger.info("📈 Progress: {}/{} chunks ({:.1f}%), ~{} records ({:.1f}%)", 
                              completedChunks, totalChunks, chunkProgress, 
                              processedRecords.get(), recordProgress);
                    
                    Thread.sleep(5000); // Report every 5 seconds
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        
        monitor.setDaemon(true);
        monitor.start();
    }
    
    private ProcessingResult aggregateResults(Map<Integer, ChunkResult> results, long duration) {
        int totalProcessed = results.values().stream()
            .mapToInt(ChunkResult::getProcessedCount)
            .sum();
            
        int totalErrors = results.values().stream()
            .mapToInt(ChunkResult::getErrorCount)
            .sum();
        
        return new ProcessingResult(true, totalProcessed, totalErrors, duration);
    }
    
    // Simulate processing operations
    private void validateRecord(DataRecord record) throws Exception {
        Thread.sleep(10); // Simulate validation time
        if (Math.random() < 0.05) { // 5% validation failure rate
            throw new ValidationException("Invalid record format");
        }
    }
    
    private void transformRecord(DataRecord record) {
        // Simulate transformation
        try {
            Thread.sleep(5);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    private void persistRecord(DataRecord record) throws Exception {
        Thread.sleep(15); // Simulate persistence time
        if (Math.random() < 0.02) { // 2% persistence failure rate
            throw new PersistenceException("Failed to save record");
        }
    }
    
    public void shutdown() {
        processingPool.shutdown();
        try {
            if (!processingPool.awaitTermination(30, TimeUnit.SECONDS)) {
                processingPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            processingPool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}

// Supporting classes
class Dataset {
    private final List<DataRecord> records;
    
    public Dataset(List<DataRecord> records) {
        this.records = records;
    }
    
    public int size() {
        return records.size();
    }
    
    public List<DataChunk> splitIntoChunks(int chunkSize) {
        List<DataChunk> chunks = new ArrayList<>();
        
        for (int i = 0; i < records.size(); i += chunkSize) {
            int end = Math.min(i + chunkSize, records.size());
            chunks.add(new DataChunk(records.subList(i, end)));
        }
        
        return chunks;
    }
}

class DataChunk {
    private final List<DataRecord> records;
    
    public DataChunk(List<DataRecord> records) {
        this.records = records;
    }
    
    public List<DataRecord> getRecords() {
        return records;
    }
    
    public int size() {
        return records.size();
    }
}

class DataRecord {
    private final String id;
    private final Map<String, Object> data;
    
    public DataRecord(String id, Map<String, Object> data) {
        this.id = id;
        this.data = data;
    }
    
    // Getters
    public String getId() { return id; }
    public Map<String, Object> getData() { return data; }
}

class ChunkResult {
    private final int chunkIndex;
    private final int processedCount;
    private final int errorCount;
    
    public ChunkResult(int chunkIndex, int processedCount, int errorCount) {
        this.chunkIndex = chunkIndex;
        this.processedCount = processedCount;
        this.errorCount = errorCount;
    }
    
    // Getters
    public int getChunkIndex() { return chunkIndex; }
    public int getProcessedCount() { return processedCount; }
    public int getErrorCount() { return errorCount; }
}

class ProcessingResult {
    private final boolean success;
    private final int totalProcessed;
    private final int totalErrors;
    private final long durationMs;
    
    public ProcessingResult(boolean success, int totalProcessed, int totalErrors, long durationMs) {
        this.success = success;
        this.totalProcessed = totalProcessed;
        this.totalErrors = totalErrors;
        this.durationMs = durationMs;
    }
    
    // Getters
    public boolean isSuccess() { return success; }
    public int getTotalProcessed() { return totalProcessed; }
    public int getTotalErrors() { return totalErrors; }
    public long getDurationMs() { return durationMs; }
}

// Custom exceptions
class ValidationException extends Exception {
    public ValidationException(String message) { super(message); }
}

class PersistenceException extends Exception {
    public PersistenceException(String message) { super(message); }
}
```

### 3. Microservice Health Check Coordinator

```java
// 🏥 ĐIỀU PHỐI HEALTH CHECK CHO MICROSERVICES

@Service
public class HealthCheckCoordinator {
    
    private final Logger logger = LoggerFactory.getLogger(HealthCheckCoordinator.class);
    private final RestTemplate restTemplate;
    private final ExecutorService healthCheckExecutor;
    
    // Danh sách các services cần check
    private final List<ServiceEndpoint> services;
    
    public HealthCheckCoordinator(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.healthCheckExecutor = Executors.newFixedThreadPool(10);
        this.services = Arrays.asList(
            new ServiceEndpoint("user-service", "http://user-service:8080/health"),
            new ServiceEndpoint("order-service", "http://order-service:8080/health"),  
            new ServiceEndpoint("payment-service", "http://payment-service:8080/health"),
            new ServiceEndpoint("inventory-service", "http://inventory-service:8080/health"),
            new ServiceEndpoint("notification-service", "http://notification-service:8080/health"),
            new ServiceEndpoint("auth-service", "http://auth-service:8080/health")
        );
    }
    
    // ✅ Parallel health check với timeout
    public SystemHealthStatus checkSystemHealth(long timeoutSeconds) throws InterruptedException {
        logger.info("🏥 Starting system health check for {} services", services.size());
        
        CountDownLatch healthCheckLatch = new CountDownLatch(services.size());
        ConcurrentHashMap<String, ServiceHealth> healthResults = new ConcurrentHashMap<>();
        
        long startTime = System.currentTimeMillis();
        
        // Submit health check tasks
        for (ServiceEndpoint service : services) {
            healthCheckExecutor.submit(() -> {
                try {
                    logger.debug("🔍 Checking health of {}", service.getName());
                    
                    ServiceHealth health = checkServiceHealth(service);
                    healthResults.put(service.getName(), health);
                    
                    if (health.isHealthy()) {
                        logger.debug("✅ {} is healthy ({}ms)", service.getName(), health.getResponseTime());
                    } else {
                        logger.warn("❌ {} is unhealthy: {}", service.getName(), health.getError());
                    }
                    
                } catch (Exception e) {
                    logger.error("💥 Health check failed for {}: {}", service.getName(), e.getMessage());
                    healthResults.put(service.getName(), 
                        ServiceHealth.unhealthy(service.getName(), e.getMessage(), 0));
                    
                } finally {
                    healthCheckLatch.countDown();
                }
            });
        }
        
        // Monitor progress
        monitorHealthCheckProgress(healthCheckLatch);
        
        // Wait for all health checks với timeout
        boolean completed = healthCheckLatch.await(timeoutSeconds, TimeUnit.SECONDS);
        long totalTime = System.currentTimeMillis() - startTime;
        
        if (!completed) {
            logger.error("⏱️ Health check timeout! {} services didn't respond", 
                       healthCheckLatch.getCount());
            
            // Mark non-responding services as unhealthy
            for (ServiceEndpoint service : services) {
                if (!healthResults.containsKey(service.getName())) {
                    healthResults.put(service.getName(), 
                        ServiceHealth.unhealthy(service.getName(), "Timeout", totalTime));
                }
            }
        }
        
        // Analyze overall system health
        SystemHealthStatus systemHealth = analyzeSystemHealth(healthResults, totalTime);
        
        logger.info("🎯 System health check completed: {} ({}ms)", 
                   systemHealth.getOverallStatus(), totalTime);
        
        return systemHealth;
    }
    
    private ServiceHealth checkServiceHealth(ServiceEndpoint service) {
        long startTime = System.currentTimeMillis();
        
        try {
            // HTTP health check với timeout
            ResponseEntity<Map> response = restTemplate.exchange(
                service.getHealthUrl(),
                HttpMethod.GET,
                null,
                Map.class
            );
            
            long responseTime = System.currentTimeMillis() - startTime;
            
            if (response.getStatusCode().is2xxSuccessful()) {
                Map<String, Object> body = response.getBody();
                String status = (String) body.get("status");
                
                if ("UP".equals(status)) {
                    return ServiceHealth.healthy(service.getName(), responseTime);
                } else {
                    return ServiceHealth.unhealthy(service.getName(), 
                        "Service reports status: " + status, responseTime);
                }
            } else {
                return ServiceHealth.unhealthy(service.getName(), 
                    "HTTP " + response.getStatusCode(), responseTime);
            }
            
        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;
            return ServiceHealth.unhealthy(service.getName(), e.getMessage(), responseTime);
        }
    }
    
    private void monitorHealthCheckProgress(CountDownLatch latch) {
        Thread monitor = new Thread(() -> {
            int totalServices = services.size();
            
            while (latch.getCount() > 0) {
                try {
                    int completed = totalServices - (int) latch.getCount();
                    double percentage = (completed * 100.0) / totalServices;
                    
                    logger.info("📊 Health check progress: {}/{} services ({:.1f}%)", 
                              completed, totalServices, percentage);
                    
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        
        monitor.setDaemon(true);
        monitor.start();
    }
    
    private SystemHealthStatus analyzeSystemHealth(Map<String, ServiceHealth> healthResults, long totalTime) {
        List<ServiceHealth> allResults = new ArrayList<>(healthResults.values());
        
        long healthyCount = allResults.stream().filter(ServiceHealth::isHealthy).count();
        long unhealthyCount = allResults.size() - healthyCount;
        
        // Calculate overall status
        OverallStatus overallStatus;
        if (unhealthyCount == 0) {
            overallStatus = OverallStatus.HEALTHY;
        } else if (healthyCount >= allResults.size() * 0.7) { // 70% healthy threshold
            overallStatus = OverallStatus.DEGRADED;
        } else {
            overallStatus = OverallStatus.UNHEALTHY;
        }
        
        // Calculate average response time for healthy services
        double avgResponseTime = allResults.stream()
            .filter(ServiceHealth::isHealthy)
            .mapToLong(ServiceHealth::getResponseTime)
            .average()
            .orElse(0.0);
        
        return new SystemHealthStatus(
            overallStatus,
            (int) healthyCount,
            (int) unhealthyCount,
            avgResponseTime,
            totalTime,
            allResults
        );
    }
    
    // ✅ Scheduled health monitoring
    @Scheduled(fixedRate = 60000) // Every minute
    public void scheduledHealthCheck() {
        try {
            SystemHealthStatus health = checkSystemHealth(30);
            
            if (health.getOverallStatus() != OverallStatus.HEALTHY) {
                logger.warn("⚠️ System health degraded: {}", health.getOverallStatus());
                
                // Alert unhealthy services
                health.getServiceResults().stream()
                    .filter(s -> !s.isHealthy())
                    .forEach(s -> logger.error("💔 {} is unhealthy: {}", s.getServiceName(), s.getError()));
            }
            
        } catch (InterruptedException e) {
            logger.error("Health check interrupted", e);
            Thread.currentThread().interrupt();
        }
    }
    
    public void shutdown() {
        healthCheckExecutor.shutdown();
        try {
            if (!healthCheckExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                healthCheckExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            healthCheckExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}

// Supporting classes
class ServiceEndpoint {
    private final String name;
    private final String healthUrl;
    
    public ServiceEndpoint(String name, String healthUrl) {
        this.name = name;
        this.healthUrl = healthUrl;
    }
    
    public String getName() { return name; }
    public String getHealthUrl() { return healthUrl; }
}

class ServiceHealth {
    private final String serviceName;
    private final boolean healthy;
    private final String error;
    private final long responseTime;
    
    private ServiceHealth(String serviceName, boolean healthy, String error, long responseTime) {
        this.serviceName = serviceName;
        this.healthy = healthy;
        this.error = error;
        this.responseTime = responseTime;
    }
    
    public static ServiceHealth healthy(String serviceName, long responseTime) {
        return new ServiceHealth(serviceName, true, null, responseTime);
    }
    
    public static ServiceHealth unhealthy(String serviceName, String error, long responseTime) {
        return new ServiceHealth(serviceName, false, error, responseTime);
    }
    
    // Getters
    public String getServiceName() { return serviceName; }
    public boolean isHealthy() { return healthy; }
    public String getError() { return error; }
    public long getResponseTime() { return responseTime; }
}

enum OverallStatus {
    HEALTHY("All services are healthy"),
    DEGRADED("Some services are unhealthy"),
    UNHEALTHY("Most services are unhealthy");
    
    private final String description;
    
    OverallStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() { return description; }
}

class SystemHealthStatus {
    private final OverallStatus overallStatus;
    private final int healthyServiceCount;
    private final int unhealthyServiceCount;
    private final double averageResponseTime;
    private final long totalCheckTime;
    private final List<ServiceHealth> serviceResults;
    
    public SystemHealthStatus(OverallStatus overallStatus, int healthyServiceCount, 
                            int unhealthyServiceCount, double averageResponseTime,
                            long totalCheckTime, List<ServiceHealth> serviceResults) {
        this.overallStatus = overallStatus;
        this.healthyServiceCount = healthyServiceCount;
        this.unhealthyServiceCount = unhealthyServiceCount;
        this.averageResponseTime = averageResponseTime;
        this.totalCheckTime = totalCheckTime;
        this.serviceResults = serviceResults;
    }
    
    // Getters
    public OverallStatus getOverallStatus() { return overallStatus; }
    public int getHealthyServiceCount() { return healthyServiceCount; }
    public int getUnhealthyServiceCount() { return unhealthyServiceCount; }
    public double getAverageResponseTime() { return averageResponseTime; }
    public long getTotalCheckTime() { return totalCheckTime; }
    public List<ServiceHealth> getServiceResults() { return serviceResults; }
}
```

---

## Advanced Patterns

### 1. CountDownLatch với Resource Management

```java
// 🏭 ADVANCED PATTERN: RESOURCE MANAGEMENT VỚI COUNTDOWNLATCH

public class ResourceManagerWithLatch {
    
    // ✅ Pattern: Resource Pool với startup coordination
    public static class ManagedResourcePool<T> {
        private final List<T> resources;
        private final BlockingQueue<T> availableResources;
        private final CountDownLatch initializationLatch;
        private final ExecutorService initExecutor;
        private volatile boolean initialized = false;
        
        public ManagedResourcePool(List<ResourceFactory<T>> factories) {
            this.resources = new ArrayList<>();
            this.availableResources = new LinkedBlockingQueue<>();
            this.initializationLatch = new CountDownLatch(factories.size());
            this.initExecutor = Executors.newFixedThreadPool(factories.size());
            
            // Initialize resources in parallel
            initializeResources(factories);
        }
        
        private void initializeResources(List<ResourceFactory<T>> factories) {
            for (ResourceFactory<T> factory : factories) {
                initExecutor.submit(() -> {
                    try {
                        System.out.println("🔧 Initializing resource: " + factory.getName());
                        T resource = factory.createResource();
                        
                        synchronized (resources) {
                            resources.add(resource);
                        }
                        availableResources.offer(resource);
                        
                        System.out.println("✅ Resource initialized: " + factory.getName());
                        
                    } catch (Exception e) {
                        System.err.println("❌ Failed to initialize resource " + factory.getName() + ": " + e.getMessage());
                        
                    } finally {
                        initializationLatch.countDown();
                    }
                });
            }
        }
        
        // Wait for all resources to be initialized
        public boolean waitForInitialization(long timeout, TimeUnit unit) throws InterruptedException {
            boolean completed = initializationLatch.await(timeout, unit);
            if (completed) {
                initialized = true;
                initExecutor.shutdown();
                System.out.println("🎉 Resource pool fully initialized with " + resources.size() + " resources");
            }
            return completed;
        }
        
        public T acquireResource(long timeout, TimeUnit unit) throws InterruptedException {
            if (!initialized) {
                throw new IllegalStateException("Resource pool not initialized");
            }
            
            return availableResources.poll(timeout, unit);
        }
        
        public void releaseResource(T resource) {
            if (resources.contains(resource)) {
                availableResources.offer(resource);
            }
        }
        
        public int getAvailableCount() {
            return availableResources.size();
        }
        
        public int getTotalCount() {
            return resources.size();
        }
    }
    
    // ✅ Pattern: Phased initialization với multiple latches
    public static class PhasedInitializer {
        
        public static boolean initializeInPhases(Map<String, List<Runnable>> phases, 
                                               long timeoutPerPhase, TimeUnit unit) 
                throws InterruptedException {
            
            System.out.println("🚀 Starting phased initialization with " + phases.size() + " phases");
            
            for (Map.Entry<String, List<Runnable>> phase : phases.entrySet()) {
                String phaseName = phase.getKey();
                List<Runnable> tasks = phase.getValue();
                
                System.out.println("📋 Phase: " + phaseName + " (" + tasks.size() + " tasks)");
                
                CountDownLatch phaseLatch = new CountDownLatch(tasks.size());
                ExecutorService phaseExecutor = Executors.newFixedThreadPool(tasks.size());
                
                // Submit tasks for this phase
                for (int i = 0; i < tasks.size(); i++) {
                    final int taskIndex = i;
                    final Runnable task = tasks.get(i);
                    
                    phaseExecutor.submit(() -> {
                        try {
                            System.out.println("  ⏳ Task " + taskIndex + " starting in phase " + phaseName);
                            task.run();
                            System.out.println("  ✅ Task " + taskIndex + " completed in phase " + phaseName);
                        } catch (Exception e) {
                            System.err.println("  ❌ Task " + taskIndex + " failed in phase " + phaseName + ": " + e.getMessage());
                        } finally {
                            phaseLatch.countDown();
                        }
                    });
                }
                
                // Wait for phase completion
                boolean phaseCompleted = phaseLatch.await(timeoutPerPhase, unit);
                phaseExecutor.shutdown();
                
                if (!phaseCompleted) {
                    System.err.println("❌ Phase " + phaseName + " timeout!");
                    return false;
                }
                
                System.out.println("✅ Phase " + phaseName + " completed");
            }
            
            System.out.println("🎉 All phases completed successfully!");
            return true;
        }
    }
    
    // ✅ Pattern: Dependent task coordination
    public static class DependentTaskCoordinator {
        
        public static class TaskNode {
            private final String name;
            private final Runnable task;
            private final List<TaskNode> dependencies;
            private final CountDownLatch completionLatch;
            private volatile boolean completed = false;
            
            public TaskNode(String name, Runnable task) {
                this.name = name;
                this.task = task;
                this.dependencies = new ArrayList<>();
                this.completionLatch = new CountDownLatch(1);
            }
            
            public void addDependency(TaskNode dependency) {
                dependencies.add(dependency);
            }
            
            public void execute(ExecutorService executor) {
                executor.submit(() -> {
                    try {
                        // Wait for all dependencies
                        for (TaskNode dependency : dependencies) {
                            System.out.println("⏳ Task " + name + " waiting for dependency: " + dependency.name);
                            dependency.completionLatch.await();
                        }
                        
                        System.out.println("🔄 Executing task: " + name);
                        task.run();
                        completed = true;
                        System.out.println("✅ Task completed: " + name);
                        
                    } catch (Exception e) {
                        System.err.println("❌ Task failed: " + name + " - " + e.getMessage());
                    } finally {
                        completionLatch.countDown();
                    }
                });
            }
            
            public boolean waitForCompletion(long timeout, TimeUnit unit) throws InterruptedException {
                return completionLatch.await(timeout, unit);
            }
            
            public String getName() { return name; }
            public boolean isCompleted() { return completed; }
        }
        
        public static boolean executeDependentTasks(List<TaskNode> tasks, long timeout, TimeUnit unit) 
                throws InterruptedException {
            
            System.out.println("🔗 Executing " + tasks.size() + " dependent tasks");
            
            ExecutorService executor = Executors.newFixedThreadPool(tasks.size());
            
            // Submit all tasks (they will wait for dependencies internally)
            for (TaskNode task : tasks) {
                task.execute(executor);
            }
            
            // Wait for all tasks to complete
            boolean allCompleted = true;
            for (TaskNode task : tasks) {
                boolean completed = task.waitForCompletion(timeout, unit);
                if (!completed) {
                    System.err.println("❌ Task " + task.getName() + " timeout!");
                    allCompleted = false;
                }
            }
            
            executor.shutdown();
            return allCompleted;
        }
    }
    
    // Helper interface
    interface ResourceFactory<T> {
        String getName();
        T createResource() throws Exception;
    }
}
```

### 2. CountDownLatch với Circuit Breaker Pattern

```java
// ⚡ ADVANCED PATTERN: CIRCUIT BREAKER VỚI COUNTDOWNLATCH

public class CircuitBreakerWithLatch {
    
    public static class DistributedCircuitBreaker {
        private final String serviceName;
        private final int maxConcurrentCalls;
        private final long timeoutMs;
        private final double failureThreshold;
        
        private volatile CircuitState state = CircuitState.CLOSED;
        private final AtomicInteger concurrentCalls = new AtomicInteger(0);
        private final AtomicInteger successCount = new AtomicInteger(0);
        private final AtomicInteger failureCount = new AtomicInteger(0);
        private volatile long lastFailureTime = 0;
        
        public DistributedCircuitBreaker(String serviceName, int maxConcurrentCalls, 
                                       long timeoutMs, double failureThreshold) {
            this.serviceName = serviceName;
            this.maxConcurrentCalls = maxConcurrentCalls;
            this.timeoutMs = timeoutMs;
            this.failureThreshold = failureThreshold;
        }
        
        // ✅ Execute với circuit breaker protection
        public <T> T execute(Callable<T> operation) throws Exception {
            // Check circuit state
            if (state == CircuitState.OPEN) {
                throw new CircuitBreakerOpenException("Circuit breaker is OPEN for " + serviceName);
            }
            
            // Check concurrent call limit
            if (concurrentCalls.get() >= maxConcurrentCalls) {
                throw new TooManyConcurrentCallsException("Too many concurrent calls to " + serviceName);
            }
            
            // Execute with coordination
            return executeWithLatch(operation);
        }
        
        private <T> T executeWithLatch(Callable<T> operation) throws Exception {
            concurrentCalls.incrementAndGet();
            CountDownLatch executionLatch = new CountDownLatch(1);
            
            AtomicReference<T> result = new AtomicReference<>();
            AtomicReference<Exception> exception = new AtomicReference<>();
            
            // Execute trong thread pool với timeout
            ExecutorService executor = Executors.newSingleThreadExecutor();
            
            try {
                executor.submit(() -> {
                    try {
                        T value = operation.call();
                        result.set(value);
                        recordSuccess();
                    } catch (Exception e) {
                        exception.set(e);
                        recordFailure();
                    } finally {
                        executionLatch.countDown();
                    }
                });
                
                // Wait với timeout
                boolean completed = executionLatch.await(timeoutMs, TimeUnit.MILLISECONDS);
                
                if (!completed) {
                    recordFailure();
                    throw new TimeoutException("Operation timeout for " + serviceName);
                }
                
                if (exception.get() != null) {
                    throw exception.get();
                }
                
                return result.get();
                
            } finally {
                concurrentCalls.decrementAndGet();
                executor.shutdown();
            }
        }
        
        private void recordSuccess() {
            successCount.incrementAndGet();
            updateCircuitState();
        }
        
        private void recordFailure() {
            failureCount.incrementAndGet();
            lastFailureTime = System.currentTimeMillis();
            updateCircuitState();
        }
        
        private void updateCircuitState() {
            int total = successCount.get() + failureCount.get();
            
            if (total >= 10) { // Minimum calls threshold
                double currentFailureRate = (double) failureCount.get() / total;
                
                if (currentFailureRate >= failureThreshold) {
                    state = CircuitState.OPEN;
                    System.out.println("⚡ Circuit breaker OPENED for " + serviceName + 
                                     " (failure rate: " + String.format("%.2f", currentFailureRate * 100) + "%)");
                } else if (state == CircuitState.OPEN && 
                          System.currentTimeMillis() - lastFailureTime > 60000) { // 1 minute recovery
                    state = CircuitState.HALF_OPEN;
                    System.out.println("🔄 Circuit breaker HALF-OPEN for " + serviceName);
                } else if (state == CircuitState.HALF_OPEN && currentFailureRate < failureThreshold) {
                    state = CircuitState.CLOSED;
                    resetCounters();
                    System.out.println("✅ Circuit breaker CLOSED for " + serviceName);
                }
            }
        }
        
        private void resetCounters() {
            successCount.set(0);
            failureCount.set(0);
        }
        
        public CircuitBreakerStats getStats() {
            return new CircuitBreakerStats(
                serviceName,
                state,
                concurrentCalls.get(),
                successCount.get(),
                failureCount.get(),
                System.currentTimeMillis() - lastFailureTime
            );
        }
    }
    
    // ✅ Batch execution với circuit breaker
    public static class BatchExecutorWithCircuitBreaker {
        private final Map<String, DistributedCircuitBreaker> circuitBreakers;
        private final ExecutorService batchExecutor;
        
        public BatchExecutorWithCircuitBreaker() {
            this.circuitBreakers = new ConcurrentHashMap<>();
            this.batchExecutor = Executors.newFixedThreadPool(20);
        }
        
        public <T> List<BatchResult<T>> executeBatch(List<BatchTask<T>> tasks, long timeoutSeconds) 
                throws InterruptedException {
            
            System.out.println("🚀 Executing batch of " + tasks.size() + " tasks");
            
            CountDownLatch batchLatch = new CountDownLatch(tasks.size());
            List<BatchResult<T>> results = Collections.synchronizedList(new ArrayList<>());
            
            // Submit tất cả tasks
            for (int i = 0; i < tasks.size(); i++) {
                final int taskIndex = i;
                final BatchTask<T> task = tasks.get(i);
                
                batchExecutor.submit(() -> {
                    try {
                        DistributedCircuitBreaker breaker = getOrCreateCircuitBreaker(task.getServiceName());
                        
                        T result = breaker.execute(task.getOperation());
                        results.add(BatchResult.success(taskIndex, result));
                        
                        System.out.println("✅ Task " + taskIndex + " completed successfully");
                        
                    } catch (Exception e) {
                        results.add(BatchResult.failure(taskIndex, e));
                        System.err.println("❌ Task " + taskIndex + " failed: " + e.getMessage());
                        
                    } finally {
                        batchLatch.countDown();
                    }
                });
            }
            
            // Wait cho tất cả tasks
            boolean completed = batchLatch.await(timeoutSeconds, TimeUnit.SECONDS);
            
            if (!completed) {
                System.err.println("⏱️ Batch execution timeout!");
            }
            
            // Sort results by task index
            results.sort(Comparator.comparing(BatchResult::getTaskIndex));
            
            System.out.println("📊 Batch completed: " + results.size() + " results");
            
            return results;
        }
        
        private DistributedCircuitBreaker getOrCreateCircuitBreaker(String serviceName) {
            return circuitBreakers.computeIfAbsent(serviceName, name ->
                new DistributedCircuitBreaker(name, 10, 5000, 0.5) // Max 10 concurrent, 5s timeout, 50% failure threshold
            );
        }
        
        public Map<String, CircuitBreakerStats> getAllCircuitBreakerStats() {
            return circuitBreakers.entrySet().stream()
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> entry.getValue().getStats()
                ));
        }
        
        public void shutdown() {
            batchExecutor.shutdown();
            try {
                if (!batchExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                    batchExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                batchExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
    
    // Supporting classes
    enum CircuitState {
        CLOSED, OPEN, HALF_OPEN
    }
    
    static class BatchTask<T> {
        private final String serviceName;
        private final Callable<T> operation;
        
        public BatchTask(String serviceName, Callable<T> operation) {
            this.serviceName = serviceName;
            this.operation = operation;
        }
        
        public String getServiceName() { return serviceName; }
        public Callable<T> getOperation() { return operation; }
    }
    
    static class BatchResult<T> {
        private final int taskIndex;
        private final boolean success;
        private final T result;
        private final Exception error;
        
        private BatchResult(int taskIndex, boolean success, T result, Exception error) {
            this.taskIndex = taskIndex;
            this.success = success;
            this.result = result;
            this.error = error;
        }
        
        public static <T> BatchResult<T> success(int taskIndex, T result) {
            return new BatchResult<>(taskIndex, true, result, null);
        }
        
        public static <T> BatchResult<T> failure(int taskIndex, Exception error) {
            return new BatchResult<>(taskIndex, false, null, error);
        }
        
        // Getters
        public int getTaskIndex() { return taskIndex; }
        public boolean isSuccess() { return success; }
        public T getResult() { return result; }
        public Exception getError() { return error; }
    }
    
    static class CircuitBreakerStats {
        private final String serviceName;
        private final CircuitState state;
        private final int concurrentCalls;
        private final int successCount;
        private final int failureCount;
        private final long timeSinceLastFailure;
        
        public CircuitBreakerStats(String serviceName, CircuitState state, int concurrentCalls,
                                 int successCount, int failureCount, long timeSinceLastFailure) {
            this.serviceName = serviceName;
            this.state = state;
            this.concurrentCalls = concurrentCalls;
            this.successCount = successCount;
            this.failureCount = failureCount;
            this.timeSinceLastFailure = timeSinceLastFailure;
        }
        
        // Getters
        public String getServiceName() { return serviceName; }
        public CircuitState getState() { return state; }
        public int getConcurrentCalls() { return concurrentCalls; }
        public int getSuccessCount() { return successCount; }
        public int getFailureCount() { return failureCount; }
        public long getTimeSinceLastFailure() { return timeSinceLastFailure; }
        
        public double getFailureRate() {
            int total = successCount + failureCount;
            return total > 0 ? (double) failureCount / total : 0.0;
        }
    }
    
    // Custom exceptions
    static class CircuitBreakerOpenException extends Exception {
        public CircuitBreakerOpenException(String message) { super(message); }
    }
    
    static class TooManyConcurrentCallsException extends Exception {
        public TooManyConcurrentCallsException(String message) { super(message); }
    }
}
```

---

## 📋 Tóm tắt và Khuyến nghị

### Bảng so sánh nhanh:

| Approach | Use Case | Complexity | Performance | Reusability |
|----------|----------|------------|-------------|-------------|
| `Polling` | ❌ Không khuyến nghị | ⭐ | ❌ (CPU waste) | ❌ |
| `Thread.join()` | Single-use threads | ⭐⭐ | ⭐⭐⭐ | ❌ |
| `CountDownLatch` | Task coordination | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ❌ (one-time) |
| `ExecutorService.awaitTermination()` | Service shutdown | ⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐ |

### Quy tắc vàng:

1. **🎯 Task Coordination** → `CountDownLatch`
2. **🚀 Application Startup** → `CountDownLatch` cho parallel initialization
3. **📊 Batch Processing** → `CountDownLatch` cho chunk completion
4. **🏥 Health Checks** → `CountDownLatch` cho parallel checks
5. **🔄 Service Lifecycle** → `ExecutorService.awaitTermination()`

### Khi nào sử dụng CountDownLatch:

✅ **Nên dùng khi:**
- Cần chờ nhiều tasks hoàn thành
- Parallel initialization của components
- Batch processing với synchronization
- Testing concurrent code
- One-time coordination events

❌ **Không nên dùng khi:**
- Cần reuse coordination object (dùng `CyclicBarrier`)
- Chỉ có 1 task (overhead không cần thiết)
- Cần complex synchronization logic (dùng `Semaphore` hoặc custom locks)
- Simple sequential processing

### Lưu ý quan trọng:

⚠️ **CountDownLatch KHÔNG THỂ reset** - chỉ dùng 1 lần!
⚠️ **Luôn countDown() trong finally block** để tránh deadlock
⚠️ **Sử dụng timeout** để tránh chờ vĩnh viễn
⚠️ **Handle InterruptedException** properly

**Nhớ:** CountDownLatch là perfect cho "wait for all to complete" scenarios! 🎯