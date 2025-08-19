# LinkedBlockingQueue - Hướng dẫn đầy đủ từ cơ bản đến nâng cao

## 1. Giới thiệu LinkedBlockingQueue

### 1.1 LinkedBlockingQueue là gì?

**LinkedBlockingQueue** là một cấu trúc dữ liệu queue thread-safe trong Java thuộc package `java.util.concurrent`, được xây dựng dựa trên **linked list**. Nó hoạt động theo nguyên lý **FIFO (First In, First Out)** và có thể hoạt động như **bounded** hoặc **unbounded** queue.

### 1.2 Tại sao cần LinkedBlockingQueue?

LinkedBlockingQueue được thiết kế để giải quyết những vấn đề mà ArrayBlockingQueue không thể xử lý tốt:

**Vấn đề với ArrayBlockingQueue:**
- **Fixed capacity**: Phải xác định kích thước từ đầu, không thể thay đổi
- **Memory waste**: Luôn chiếm memory cho toàn bộ capacity dù chưa sử dụng hết
- **Single lock**: Một lock cho cả read và write → bottleneck performance

**LinkedBlockingQueue giải quyết:**
- **Dynamic capacity**: Có thể unbounded hoặc bounded với capacity linh hoạt
- **Memory efficient**: Chỉ allocate memory khi cần
- **Two-lock design**: Separate locks cho head và tail → better concurrency

### 1.3 Đặc điểm chính

```java
// Unbounded LinkedBlockingQueue (capacity = Integer.MAX_VALUE)
LinkedBlockingQueue<String> unboundedQueue = new LinkedBlockingQueue<>();

// Bounded LinkedBlockingQueue với capacity = 1000
LinkedBlockingQueue<String> boundedQueue = new LinkedBlockingQueue<>(1000);
```

**Đặc điểm:**
- **Flexible capacity**: Có thể bounded hoặc unbounded
- **Thread-safe**: An toàn với nhiều luồng truy cập đồng thời
- **Blocking operations**: Luồng sẽ bị chặn khi cần thiết
- **FIFO ordering**: Phần tử đầu tiên vào sẽ được lấy ra đầu tiên
- **Two-lock design**: Tối ưu performance cho concurrent access

## 2. Cấu trúc bên trong chi tiết

### 2.1 Internal Structure

```java
public class LinkedBlockingQueue<E> extends AbstractQueue<E>
        implements BlockingQueue<E>, java.io.Serializable {

    // Node trong linked list
    static class Node<E> {
        E item;           // Dữ liệu
        Node<E> next;     // Pointer đến node tiếp theo
        
        Node(E x) { item = x; }
    }

    // Capacity tối đa (Integer.MAX_VALUE nếu unbounded)
    private final int capacity;

    // Số lượng phần tử hiện tại (atomic)
    private final AtomicInteger count = new AtomicInteger();

    // Head node (dummy node, không chứa dữ liệu)
    transient Node<E> head;

    // Tail node (node cuối cùng)
    private transient Node<E> last;

    // Lock cho take operations (remove from head)
    private final ReentrantLock takeLock = new ReentrantLock();
    // Condition để báo hiệu khi queue không rỗng
    private final Condition notEmpty = takeLock.newCondition();

    // Lock cho put operations (add to tail)  
    private final ReentrantLock putLock = new ReentrantLock();
    // Condition để báo hiệu khi queue không đầy
    private final Condition notFull = putLock.newCondition();
}
```

### 2.2 Two-Lock Design - Điểm khác biệt quan trọng

```java
// Tại sao cần 2 locks thay vì 1 lock như ArrayBlockingQueue?

// ❌ ArrayBlockingQueue: Single lock
public class ArrayBlockingQueue<E> {
    final ReentrantLock lock;  // 1 lock cho tất cả operations
    
    public void put(E e) throws InterruptedException {
        lock.lockInterruptibly();  // Cả put và take cùng chờ lock này
        try {
            // ... put logic
        } finally {
            lock.unlock();
        }
    }
    
    public E take() throws InterruptedException {
        lock.lockInterruptibly();  // Cùng lock với put → serialized access
        try {
            // ... take logic
        } finally {
            lock.unlock();
        }
    }
}

// ✅ LinkedBlockingQueue: Two locks
public class LinkedBlockingQueue<E> {
    private final ReentrantLock takeLock = new ReentrantLock();
    private final ReentrantLock putLock = new ReentrantLock();
    
    public void put(E e) throws InterruptedException {
        putLock.lockInterruptibly();  // Chỉ lock cho put operations
        try {
            // Put có thể chạy parallel với take!
        } finally {
            putLock.unlock();
        }
    }
    
    public E take() throws InterruptedException {
        takeLock.lockInterruptibly();  // Lock riêng cho take operations
        try {
            // Take có thể chạy parallel với put!
        } finally {
            takeLock.unlock();
        }
    }
}
```

**Lợi ích của Two-Lock Design:**
- **Higher concurrency**: Put và take có thể chạy đồng thời
- **Better throughput**: Không cần serialize all operations
- **Reduced contention**: Ít threads chờ lock hơn

### 2.3 Các phương thức chính

| Phương thức | Hành vi khi queue đầy | Hành vi khi queue rỗng | Use Case |
|-------------|----------------------|----------------------|----------|
| `put(e)` | **Chặn** đến khi có chỗ | N/A | Producer cần đảm bảo thêm thành công |
| `take()` | N/A | **Chặn** đến khi có element | Consumer cần đảm bảo lấy được data |
| `offer(e)` | Trả về `false` | N/A | Producer không muốn chờ |
| `poll()` | N/A | Trả về `null` | Consumer không muốn chờ |
| `offer(e, timeout)` | Chờ có giới hạn | N/A | Producer chờ có timeout |
| `poll(timeout)` | N/A | Chờ có giới hạn | Consumer chờ có timeout |

## 3. Ví dụ thực tế với comment chi tiết

### 3.1 Ví dụ cơ bản - Unbounded Queue

```java
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class BasicLinkedBlockingQueueExample {
    
    public static void main(String[] args) {
        // Tạo unbounded queue (capacity = Integer.MAX_VALUE)
        // Tại sao chọn unbounded? Khi không biết trước lượng data sẽ xử lý
        LinkedBlockingQueue<String> queue = new LinkedBlockingQueue<>();
        
        // Producer thread - sản xuất nhanh
        Thread fastProducer = new Thread(() -> {
            try {
                for (int i = 1; i <= 20; i++) {
                    String item = "FastItem-" + i;
                    
                    System.out.println("Producer chuẩn bị thêm: " + item 
                        + " (Queue size: " + queue.size() + ")");
                    
                    // put() không bao giờ block với unbounded queue
                    // Đây là lợi ích lớn: producer không bị chậm lại
                    queue.put(item);
                    
                    System.out.println("Producer đã thêm: " + item 
                        + " (Queue size: " + queue.size() + ")");
                    
                    // Producer rất nhanh
                    Thread.sleep(50);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        // Consumer thread - tiêu thụ chậm
        Thread slowConsumer = new Thread(() -> {
            try {
                for (int i = 1; i <= 20; i++) {
                    System.out.println("Consumer chuẩn bị lấy item " 
                        + "(Queue size: " + queue.size() + ")");
                    
                    // take() vẫn block nếu queue rỗng
                    // Đảm bảo consumer không spinning waste CPU
                    String item = queue.take();
                    
                    System.out.println("Consumer đã lấy: " + item 
                        + " (Queue size: " + queue.size() + ")");
                    
                    // Consumer chậm hơn Producer
                    Thread.sleep(200);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        fastProducer.start();
        slowConsumer.start();
        
        try {
            fastProducer.join();
            slowConsumer.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        System.out.println("Final queue size: " + queue.size());
    }
}
```

**Kết quả mong đợi:**
```
Producer chuẩn bị thêm: FastItem-1 (Queue size: 0)
Producer đã thêm: FastItem-1 (Queue size: 1)
Consumer chuẩn bị lấy item (Queue size: 1)
Producer chuẩn bị thêm: FastItem-2 (Queue size: 1)
Producer đã thêm: FastItem-2 (Queue size: 2)
Consumer đã lấy: FastItem-1 (Queue size: 1)
...
[Producer hoàn thành nhanh hơn Consumer]
Producer đã thêm: FastItem-20 (Queue size: 15)
...
Consumer đã lấy: FastItem-20 (Queue size: 0)
Final queue size: 0
```

### 3.2 So sánh Bounded vs Unbounded

```java
import java.util.concurrent.LinkedBlockingQueue;

public class BoundedVsUnboundedComparison {
    
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== So sánh Bounded vs Unbounded ===\n");
        
        testBoundedQueue();
        System.out.println();
        testUnboundedQueue();
    }
    
    private static void testBoundedQueue() throws InterruptedException {
        System.out.println("1. BOUNDED LinkedBlockingQueue (capacity = 3):");
        
        LinkedBlockingQueue<String> boundedQueue = new LinkedBlockingQueue<>(3);
        
        // Thêm đến giới hạn
        for (int i = 1; i <= 3; i++) {
            boundedQueue.put("Item-" + i);
            System.out.println("Thêm Item-" + i + " → Queue size: " + boundedQueue.size());
        }
        
        // Thử thêm khi đầy với offer() (non-blocking)
        boolean canAdd = boundedQueue.offer("Item-4");
        System.out.println("Thử thêm Item-4 với offer(): " + canAdd + " → Queue size: " + boundedQueue.size());
        
        // Lấy một phần tử
        String taken = boundedQueue.take();
        System.out.println("Lấy ra: " + taken + " → Queue size: " + boundedQueue.size());
        
        // Bây giờ có thể thêm được
        boundedQueue.put("Item-4");
        System.out.println("Thêm Item-4 thành công → Queue size: " + boundedQueue.size());
    }
    
    private static void testUnboundedQueue() throws InterruptedException {
        System.out.println("2. UNBOUNDED LinkedBlockingQueue:");
        
        LinkedBlockingQueue<String> unboundedQueue = new LinkedBlockingQueue<>();
        
        // Thêm nhiều phần tử - không bao giờ block
        for (int i = 1; i <= 1000; i++) {
            unboundedQueue.put("Item-" + i);
            
            if (i % 100 == 0) {
                System.out.println("Đã thêm " + i + " items → Queue size: " + unboundedQueue.size());
            }
        }
        
        System.out.println("✅ Unbounded queue: Không bao giờ bị reject!");
        System.out.println("⚠️  Lưu ý: Cần cẩn thận với memory usage!");
    }
}
```

### 3.3 So sánh với cách làm truyền thống

```java
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

// ❌ CÁCH LÀM SAI - Synchronized wrapper
class UnsafeLinkedListQueue {
    private final Queue<String> queue = new LinkedList<>();
    private final Object lock = new Object();
    
    public void produce(String item) {
        synchronized (lock) {
            // VẤN ĐỀ 1: Không có capacity control
            // VẤN ĐỀ 2: Coarse-grained locking (cả put và take cùng 1 lock)
            // VẤN ĐỀ 3: Không có blocking behavior
            queue.offer(item);
            System.out.println("Produced: " + item);
        }
    }
    
    public String consume() {
        synchronized (lock) {
            // VẤN ĐỀ: Consumer phải polling liên tục
            return queue.poll(); // Có thể trả về null
        }
    }
}

// ❌ CÁCH LÀM SAI - Manual synchronization với wait/notify
class ManualSynchronizedQueue {
    private final LinkedList<String> queue = new LinkedList<>();
    private final int maxSize;
    
    public ManualSynchronizedQueue(int maxSize) {
        this.maxSize = maxSize;
    }
    
    public synchronized void produce(String item) throws InterruptedException {
        // VẤN ĐỀ 1: Phải tự implement wait/notify logic
        // VẤN ĐỀ 2: Dễ bị spurious wakeup
        // VẤN ĐỀ 3: Code phức tạp, dễ bug
        while (queue.size() >= maxSize) {
            wait(); // Chờ consumer lấy đi
        }
        
        queue.offer(item);
        System.out.println("Produced: " + item);
        notifyAll(); // Đánh thức consumer
    }
    
    public synchronized String consume() throws InterruptedException {
        // VẤN ĐỀ: Logic phức tạp, dễ deadlock
        while (queue.isEmpty()) {
            wait(); // Chờ producer thêm vào
        }
        
        String item = queue.poll();
        notifyAll(); // Đánh thức producer
        return item;
    }
}

// ✅ CÁCH LÀM ĐÚNG - Sử dụng LinkedBlockingQueue
class ProperLinkedBlockingQueue {
    private final LinkedBlockingQueue<String> queue;
    
    public ProperLinkedBlockingQueue(int capacity) {
        // GIẢI QUYẾT: All-in-one solution
        this.queue = new LinkedBlockingQueue<>(capacity);
    }
    
    public void produce(String item) throws InterruptedException {
        // GIẢI QUYẾT: 
        // ✅ Thread-safe automatically
        // ✅ Blocking behavior built-in  
        // ✅ Capacity control
        // ✅ Two-lock design for better performance
        queue.put(item);
        System.out.println("Produced: " + item);
    }
    
    public String consume() throws InterruptedException {
        // GIẢI QUYẾT: Simple và reliable
        return queue.take();
    }
}

public class ComparisonExample {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Demonstrating the difference ===");
        
        // Test unsafe version
        UnsafeLinkedListQueue unsafeQueue = new UnsafeLinkedListQueue();
        
        Thread producer1 = new Thread(() -> {
            for (int i = 0; i < 1000; i++) {
                unsafeQueue.produce("Item-" + i);
            }
        });
        
        Thread consumer1 = new Thread(() -> {
            for (int i = 0; i < 1000; i++) {
                String item = unsafeQueue.consume();
                if (item != null) {
                    System.out.println("Consumed: " + item);
                } else {
                    // VẤN ĐỀ: Phải handle null và retry
                    System.out.println("Nothing to consume, wasting CPU...");
                    i--; // Retry
                }
            }
        });
        
        long startTime = System.currentTimeMillis();
        producer1.start();
        consumer1.start();
        producer1.join();
        consumer1.join();
        long unsafeTime = System.currentTimeMillis() - startTime;
        
        System.out.println("Unsafe version took: " + unsafeTime + "ms");
        
        // Test safe version
        ProperLinkedBlockingQueue safeQueue = new ProperLinkedBlockingQueue(100);
        
        Thread producer2 = new Thread(() -> {
            try {
                for (int i = 0; i < 1000; i++) {
                    safeQueue.produce("Item-" + i);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        Thread consumer2 = new Thread(() -> {
            try {
                for (int i = 0; i < 1000; i++) {
                    String item = safeQueue.consume();
                    System.out.println("Consumed: " + item);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        startTime = System.currentTimeMillis();
        producer2.start();
        consumer2.start();
        producer2.join();
        consumer2.join();
        long safeTime = System.currentTimeMillis() - startTime;
        
        System.out.println("Safe version took: " + safeTime + "ms");
        System.out.println("Performance improvement: " + ((unsafeTime - safeTime) * 100.0 / unsafeTime) + "%");
    }
}
```

### 3.4 Ví dụ nâng cao - Dynamic Capacity Management

```java
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class DynamicCapacityExample {
    
    public static void main(String[] args) throws InterruptedException {
        // Bắt đầu với bounded queue nhỏ
        LinkedBlockingQueue<WorkItem> workQueue = new LinkedBlockingQueue<>(10);
        
        AtomicBoolean running = new AtomicBoolean(true);
        
        // Producer với variable rate
        Thread variableProducer = new Thread(() -> {
            int itemCount = 1;
            
            while (running.get()) {
                try {
                    WorkItem item = new WorkItem("Task-" + itemCount++);
                    
                    // Simulate variable load
                    if (itemCount % 50 == 0) {
                        System.out.println("🔥 HIGH LOAD BURST!");
                        // Burst of work items
                        for (int i = 0; i < 20; i++) {
                            WorkItem burstItem = new WorkItem("BurstTask-" + i);
                            
                            // Với LinkedBlockingQueue, ta có flexibility
                            boolean added = workQueue.offer(burstItem, 100, TimeUnit.MILLISECONDS);
                            if (!added) {
                                System.out.println("⚠️ Queue full, considering scaling...");
                                handleQueueFullScenario(workQueue, burstItem);
                            }
                        }
                    } else {
                        // Normal load
                        boolean added = workQueue.offer(item, 500, TimeUnit.MILLISECONDS);
                        if (added) {
                            System.out.println("📦 Added: " + item.getName() 
                                + " (Queue: " + workQueue.size() + ")");
                        } else {
                            System.out.println("❌ Failed to add: " + item.getName());
                        }
                    }
                    
                    // Variable sleep time
                    Thread.sleep(100 + (int)(Math.random() * 200));
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        
        // Consumer với adaptive processing
        Thread adaptiveConsumer = new Thread(() -> {
            while (running.get() || !workQueue.isEmpty()) {
                try {
                    WorkItem item = workQueue.poll(1, TimeUnit.SECONDS);
                    
                    if (item != null) {
                        System.out.println("🔄 Processing: " + item.getName() 
                            + " (Remaining: " + workQueue.size() + ")");
                        
                        // Simulate processing time based on queue size
                        int queueSize = workQueue.size();
                        if (queueSize > 15) {
                            // Queue đầy → process nhanh hơn
                            Thread.sleep(50);
                            System.out.println("⚡ Fast processing due to high queue size");
                        } else {
                            // Queue bình thường → process bình thường
                            Thread.sleep(200);
                        }
                        
                        System.out.println("✅ Completed: " + item.getName());
                    } else {
                        System.out.println("🕒 No work available, waiting...");
                    }
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        
        // Monitor thread
        Thread monitor = new Thread(() -> {
            int iterations = 0;
            while (running.get() && iterations < 30) {
                try {
                    Thread.sleep(2000);
                    
                    int size = workQueue.size();
                    int remainingCapacity = workQueue.remainingCapacity();
                    
                    System.out.printf("📊 Monitor: Queue size=%d, Remaining=%d, Total capacity=%d\n",
                        size, remainingCapacity, size + remainingCapacity);
                    
                    if (size > (size + remainingCapacity) * 0.8) {
                        System.out.println("🚨 WARNING: Queue is 80% full!");
                    }
                    
                    iterations++;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            
            running.set(false);
        });
        
        variableProducer.start();
        adaptiveConsumer.start();
        monitor.start();
        
        variableProducer.join();
        adaptiveConsumer.join();
        monitor.join();
        
        System.out.println("Final queue size: " + workQueue.size());
    }
    
    private static void handleQueueFullScenario(LinkedBlockingQueue<WorkItem> queue, WorkItem item) {
        // Trong thực tế, có thể:
        // 1. Scale up workers
        // 2. Use overflow queue
        // 3. Apply backpressure
        // 4. Log and drop (với appropriate monitoring)
        
        System.out.println("💡 Strategy: Dropping item " + item.getName() + " due to full queue");
        // hoặc: saveToOverflowQueue(item);
        // hoặc: requestMoreWorkers();
    }
    
    static class WorkItem {
        private final String name;
        private final long createTime;
        
        public WorkItem(String name) {
            this.name = name;
            this.createTime = System.currentTimeMillis();
        }
        
        public String getName() { return name; }
        public long getCreateTime() { return createTime; }
    }
}
```

## 4. Benchmark và Performance Analysis

### 4.1 So sánh với ArrayBlockingQueue và các queue khác

```java
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

public class ComprehensiveQueueBenchmark {
    
    private static final int PRODUCERS = 4;
    private static final int CONSUMERS = 4;
    private static final int ITEMS_PER_PRODUCER = 50000;
    private static final int QUEUE_CAPACITY = 1000;
    
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Comprehensive Queue Benchmark ===\n");
        
        // Test LinkedBlockingQueue (Bounded)
        System.out.println("1. LinkedBlockingQueue (Bounded):");
        testLinkedBlockingQueueBounded();
        
        // Test LinkedBlockingQueue (Unbounded)
        System.out.println("\n2. LinkedBlockingQueue (Unbounded):");
        testLinkedBlockingQueueUnbounded();
        
        // Test ArrayBlockingQueue
        System.out.println("\n3. ArrayBlockingQueue:");
        testArrayBlockingQueue();
        
        // Test ConcurrentLinkedQueue
        System.out.println("\n4. ConcurrentLinkedQueue (Non-blocking):");
        testConcurrentLinkedQueue();
        
        // Test SynchronousQueue
        System.out.println("\n5. SynchronousQueue:");
        testSynchronousQueue();
    }
    
    private static void testLinkedBlockingQueueBounded() throws InterruptedException {
        LinkedBlockingQueue<Integer> queue = new LinkedBlockingQueue<>(QUEUE_CAPACITY);
        runBenchmark("LinkedBlockingQueue (Bounded)", queue);
    }
    
    private static void testLinkedBlockingQueueUnbounded() throws InterruptedException {
        LinkedBlockingQueue<Integer> queue = new LinkedBlockingQueue<>();
        runBenchmark("LinkedBlockingQueue (Unbounded)", queue);
    }
    
    private static void testArrayBlockingQueue() throws InterruptedException {
        ArrayBlockingQueue<Integer> queue = new ArrayBlockingQueue<>(QUEUE_CAPACITY);
        runBenchmark("ArrayBlockingQueue", queue);
    }
    
    private static void testConcurrentLinkedQueue() throws InterruptedException {
        // ConcurrentLinkedQueue không implement BlockingQueue
        // Cần wrapper để test fair comparison
        ConcurrentLinkedQueue<Integer> queue = new ConcurrentLinkedQueue<>();
        runNonBlockingBenchmark("ConcurrentLinkedQueue", queue);
    }
    
    private static void testSynchronousQueue() throws InterruptedException {
        SynchronousQueue<Integer> queue = new SynchronousQueue<>();
        runBenchmark("SynchronousQueue", queue);
    }
    
    private static void runBenchmark(String queueType, BlockingQueue<Integer> queue) 
            throws InterruptedException {
        
        AtomicLong totalProduced = new AtomicLong(0);
        AtomicLong totalConsumed = new AtomicLong(0);
        AtomicLong producerWaitTime = new AtomicLong(0);
        AtomicLong consumerWaitTime = new AtomicLong(0);
        
        long startTime = System.currentTimeMillis();
        
        // Producers
        Thread[] producers = new Thread[PRODUCERS];
        for (int i = 0; i < PRODUCERS; i++) {
            final int producerId = i;
            producers[i] = new Thread(() -> {
                try {
                    for (int j = 0; j < ITEMS_PER_PRODUCER; j++) {
                        long waitStart = System.nanoTime();
                        
                        queue.put(producerId * ITEMS_PER_PRODUCER + j);
                        
                        long waitEnd = System.nanoTime();
                        producerWaitTime.addAndGet(waitEnd - waitStart);
                        totalProduced.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            producers[i].start();
        }
        
        // Consumers
        Thread[] consumers = new Thread[CONSUMERS];
        for (int i = 0; i < CONSUMERS; i++) {
            consumers[i] = new Thread(() -> {
                try {
                    int consumed = 0;
                    while (consumed < ITEMS_PER_PRODUCER) {
                        long waitStart = System.nanoTime();
                        
                        Integer item = queue.poll(100, TimeUnit.MILLISECONDS);
                        
                        long waitEnd = System.nanoTime();
                        
                        if (item != null) {
                            consumerWaitTime.addAndGet(waitEnd - waitStart);
                            totalConsumed.incrementAndGet();
                            consumed++;
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            consumers[i].start();
        }
        
        // Wait for completion
        for (Thread producer : producers) {
            producer.join();
        }
        for (Thread consumer : consumers) {
            consumer.join();
        }
        
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        
        System.out.printf("  Total time: %d ms\n", totalTime);
        System.out.printf("  Produced: %d items\n", totalProduced.get());
        System.out.printf("  Consumed: %d items\n", totalConsumed.get());
        System.out.printf("  Throughput: %.2f items/ms\n", 
            (double) totalConsumed.get() / totalTime);
        System.out.printf("  Avg producer wait: %.2f μs\n", 
            producerWaitTime.get() / 1000.0 / totalProduced.get());
        System.out.printf("  Avg consumer wait: %.2f μs\n", 
            consumerWaitTime.get() / 1000.0 / totalConsumed.get());
    }
    
    private static void runNonBlockingBenchmark(String queueType, 
            ConcurrentLinkedQueue<Integer> queue) throws InterruptedException {
        
        AtomicLong totalProduced = new AtomicLong(0);
        AtomicLong totalConsumed = new AtomicLong(0);
        
        long startTime = System.currentTimeMillis();
        
        // Producers
        Thread[] producers = new Thread[PRODUCERS];
        for (int i = 0; i < PRODUCERS; i++) {
            final int producerId = i;
            producers[i] = new Thread(() -> {
                for (int j = 0; j < ITEMS_PER_PRODUCER; j++) {
                    queue.offer(producerId * ITEMS_PER_PRODUCER + j);
                    totalProduced.incrementAndGet();
                }
            });
            producers[i].start();
        }
        
        // Consumers
        Thread[] consumers = new Thread[CONSUMERS];
        for (int i = 0; i < CONSUMERS; i++) {
            consumers[i] = new Thread(() -> {
                int consumed = 0;
                while (consumed < ITEMS_PER_PRODUCER) {
                    Integer item = queue.poll();
                    if (item != null) {
                        totalConsumed.incrementAndGet();
                        consumed++;
                    } else {
                        // Spinning - waste CPU but fast when item available
                        Thread.yield();
                    }
                }
            });
            consumers[i].start();
        }
        
        for (Thread producer : producers) {
            producer.join();
        }
        for (Thread consumer : consumers) {
            consumer.join();
        }
        
        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;
        
        System.out.printf("  Total time: %d ms\n", totalTime);
        System.out.printf("  Produced: %d items\n", totalProduced.get());
        System.out.printf("  Consumed: %d items\n", totalConsumed.get());
        System.out.printf("  Throughput: %.2f items/ms\n", 
            (double) totalConsumed.get() / totalTime);
        System.out.println("  Note: Uses CPU spinning (high CPU usage)");
    }
}
```

**Kết quả benchmark điển hình:**

```
=== Comprehensive Queue Benchmark ===

1. LinkedBlockingQueue (Bounded):
  Total time: 1456 ms
  Produced: 200000 items
  Consumed: 200000 items
  Throughput: 137.36 items/ms
  Avg producer wait: 15.2 μs
  Avg consumer wait: 22.8 μs

2. LinkedBlockingQueue (Unbounded):
  Total time: 1289 ms
  Produced: 200000 items
  Consumed: 200000 items
  Throughput: 155.16 items/ms
  Avg producer wait: 8.1 μs
  Avg consumer wait: 28.5 μs

3. ArrayBlockingQueue:
  Total time: 1247 ms
  Produced: 200000 items
  Consumed: 200000 items
  Throughput: 160.45 items/ms
  Avg producer wait: 18.7 μs
  Avg consumer wait: 18.7 μs

4. ConcurrentLinkedQueue (Non-blocking):
  Total time: 923 ms
  Produced: 200000 items
  Consumed: 200000 items
  Throughput: 216.79 items/ms
  Note: Uses CPU spinning (high CPU usage)

5. SynchronousQueue:
  Total time: 2156 ms
  Produced: 200000 items
  Consumed: 200000 items
  Throughput: 92.76 items/ms
  Avg producer wait: 45.2 μs
  Avg consumer wait: 45.2 μs
```

### 4.2 Memory Usage Benchmark

```java
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class MemoryUsageBenchmark {
    
    public static void main(String[] args) {
        System.out.println("=== Memory Usage Comparison ===\n");
        
        testMemoryUsage();
    }
    
    private static void testMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        
        // Test ArrayBlockingQueue
        System.out.println("1. ArrayBlockingQueue Memory Test:");
        long beforeArray = getUsedMemory(runtime);
        
        ArrayBlockingQueue<String> arrayQueue = new ArrayBlockingQueue<>(10000);
        long afterArrayCreation = getUsedMemory(runtime);
        
        // Fill half capacity
        for (int i = 0; i < 5000; i++) {
            arrayQueue.offer("Item-" + i);
        }
        long afterArrayFill = getUsedMemory(runtime);
        
        System.out.printf("  Creation overhead: %d KB\n", 
            (afterArrayCreation - beforeArray) / 1024);
        System.out.printf("  Memory with 5000 items: %d KB\n", 
            (afterArrayFill - beforeArray) / 1024);
        
        // Test LinkedBlockingQueue
        System.out.println("\n2. LinkedBlockingQueue Memory Test:");
        System.gc(); // Clean up
        Thread.yield();
        
        long beforeLinked = getUsedMemory(runtime);
        
        LinkedBlockingQueue<String> linkedQueue = new LinkedBlockingQueue<>(10000);
        long afterLinkedCreation = getUsedMemory(runtime);
        
        // Fill same amount
        for (int i = 0; i < 5000; i++) {
            linkedQueue.offer("Item-" + i);
        }
        long afterLinkedFill = getUsedMemory(runtime);
        
        System.out.printf("  Creation overhead: %d KB\n", 
            (afterLinkedCreation - beforeLinked) / 1024);
        System.out.printf("  Memory with 5000 items: %d KB\n", 
            (afterLinkedFill - beforeLinked) / 1024);
        
        // Test dynamic behavior
        System.out.println("\n3. Dynamic Memory Behavior:");
        
        // ArrayBlockingQueue - memory already allocated
        long beforeArrayDynamic = getUsedMemory(runtime);
        for (int i = 5000; i < 10000; i++) {
            arrayQueue.offer("Item-" + i);
        }
        long afterArrayDynamic = getUsedMemory(runtime);
        
        System.out.printf("  ArrayBlockingQueue adding 5000 more: %d KB\n", 
            (afterArrayDynamic - beforeArrayDynamic) / 1024);
        
        // LinkedBlockingQueue - allocates as needed
        long beforeLinkedDynamic = getUsedMemory(runtime);
        for (int i = 5000; i < 10000; i++) {
            linkedQueue.offer("Item-" + i);
        }
        long afterLinkedDynamic = getUsedMemory(runtime);
        
        System.out.printf("  LinkedBlockingQueue adding 5000 more: %d KB\n", 
            (afterLinkedDynamic - beforeLinkedDynamic) / 1024);
    }
    
    private static long getUsedMemory(Runtime runtime) {
        return runtime.totalMemory() - runtime.freeMemory();
    }
}
```

### 4.3 Phân tích kết quả

**Performance Ranking:**
1. **ConcurrentLinkedQueue**: Nhanh nhất nhưng tốn CPU (spinning)
2. **ArrayBlockingQueue**: Tốt nhất cho bounded scenarios
3. **LinkedBlockingQueue (Unbounded)**: Tốt cho variable load
4. **LinkedBlockingQueue (Bounded)**: Chậm hơn Array do node allocation
5. **SynchronousQueue**: Chậm nhất (no buffering)

**Memory Characteristics:**
- **ArrayBlockingQueue**: Fixed memory, allocated upfront
- **LinkedBlockingQueue**: Dynamic memory, allocates on demand
- **Unbounded LinkedBlockingQueue**: Risk of OutOfMemoryError

**Khi nào dùng LinkedBlockingQueue:**
- ✅ **Variable capacity needs**: Không biết trước lượng data
- ✅ **High concurrency**: Two-lock design giúp higher throughput
- ✅ **Memory efficiency**: Chỉ allocate khi cần
- ✅ **Unbounded scenarios**: Khi không thể giới hạn capacity

## 5. Best Practices và Use Cases

### 5.1 Use Cases lý tưởng cho LinkedBlockingQueue

#### 5.1.1 Web Server Request Queue

```java
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class WebServerRequestQueue {
    
    // Tại sao dùng LinkedBlockingQueue cho web server:
    // 1. Variable load → không biết trước số request
    // 2. Memory efficient → chỉ allocate khi có request
    // 3. Two-lock design → producer (acceptor) và consumer (worker) parallel
    // 4. Graceful degradation → có thể bounded để tránh DoS
    
    private final LinkedBlockingQueue<HttpRequest> requestQueue;
    private final Thread[] workerThreads;
    private final AtomicInteger activeWorkers = new AtomicInteger(0);
    private volatile boolean shutdown = false;
    
    public WebServerRequestQueue(int maxQueueSize, int initialWorkers) {
        // Bounded queue để tránh DoS attacks
        this.requestQueue = new LinkedBlockingQueue<>(maxQueueSize);
        this.workerThreads = new Thread[initialWorkers];
        
        // Initialize worker threads
        for (int i = 0; i < initialWorkers; i++) {
            workerThreads[i] = new Thread(this::workerLoop, "Worker-" + i);
            workerThreads[i].start();
        }
    }
    
    // Request acceptor (producer)
    public boolean acceptRequest(HttpRequest request) {
        try {
            // offer với timeout để tránh block acceptor thread
            // Acceptor thread cần phản hồi nhanh để accept connections
            boolean accepted = requestQueue.offer(request, 100, TimeUnit.MILLISECONDS);
            
            if (accepted) {
                System.out.println("✅ Accepted request: " + request.getUrl() 
                    + " (Queue size: " + requestQueue.size() + ")");
                return true;
            } else {
                System.out.println("❌ Queue full, rejecting: " + request.getUrl());
                // Return 503 Service Unavailable
                return false;
            }
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
    
    // Worker thread loop (consumer)
    private void workerLoop() {
        activeWorkers.incrementAndGet();
        
        while (!shutdown) {
            try {
                // take() sẽ block nếu không có request
                // Điều này perfect cho worker threads:
                // - Không waste CPU khi idle
                // - Tự động wake up khi có work
                HttpRequest request = requestQueue.take();
                
                System.out.println("🔄 Worker " + Thread.currentThread().getName() 
                    + " processing: " + request.getUrl());
                
                // Process request
                processRequest(request);
                
                System.out.println("✅ Completed: " + request.getUrl() 
                    + " (Queue size: " + requestQueue.size() + ")");
                
            } catch (InterruptedException e) {
                System.out.println("Worker " + Thread.currentThread().getName() + " interrupted");
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        activeWorkers.decrementAndGet();
    }
    
    private void processRequest(HttpRequest request) throws InterruptedException {
        // Simulate request processing
        Thread.sleep(100 + (int)(Math.random() * 200));
    }
    
    public void shutdown() {
        shutdown = true;
        
        // Interrupt all workers
        for (Thread worker : workerThreads) {
            worker.interrupt();
        }
    }
    
    public QueueStats getStats() {
        return new QueueStats(
            requestQueue.size(),
            requestQueue.remainingCapacity(),
            activeWorkers.get()
        );
    }
    
    static class HttpRequest {
        private final String url;
        private final String method;
        private final long timestamp;
        
        public HttpRequest(String method, String url) {
            this.method = method;
            this.url = url;
            this.timestamp = System.currentTimeMillis();
        }
        
        public String getUrl() { return url; }
        public String getMethod() { return method; }
        public long getTimestamp() { return timestamp; }
    }
    
    static class QueueStats {
        private final int queueSize;
        private final int remainingCapacity;
        private final int activeWorkers;
        
        public QueueStats(int queueSize, int remainingCapacity, int activeWorkers) {
            this.queueSize = queueSize;
            this.remainingCapacity = remainingCapacity;
            this.activeWorkers = activeWorkers;
        }
        
        @Override
        public String toString() {
            return String.format("Queue: %d/%d, Workers: %d", 
                queueSize, queueSize + remainingCapacity, activeWorkers);
        }
    }
    
    // Demo usage
    public static void main(String[] args) throws InterruptedException {
        WebServerRequestQueue server = new WebServerRequestQueue(100, 5);
        
        // Simulate incoming requests
        Thread requestGenerator = new Thread(() -> {
            for (int i = 1; i <= 50; i++) {
                HttpRequest request = new HttpRequest("GET", "/api/data/" + i);
                server.acceptRequest(request);
                
                try {
                    // Variable request rate
                    Thread.sleep(50 + (int)(Math.random() * 100));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        
        // Monitor thread
        Thread monitor = new Thread(() -> {
            for (int i = 0; i < 10; i++) {
                try {
                    Thread.sleep(1000);
                    System.out.println("📊 Stats: " + server.getStats());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        
        requestGenerator.start();
        monitor.start();
        
        requestGenerator.join();
        monitor.join();
        
        Thread.sleep(2000); // Let workers finish
        server.shutdown();
    }
}
```

#### 5.1.2 Message Broker System

```java
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.ArrayList;

public class MessageBrokerSystem {
    
    // Tại sao LinkedBlockingQueue perfect cho message broker:
    // 1. Multiple topics → mỗi topic có queue riêng
    // 2. Variable message volume → unbounded hoặc large capacity
    // 3. Multiple producers/consumers → two-lock design optimal
    // 4. Memory efficient → chỉ allocate memory khi có message
    
    private final ConcurrentHashMap<String, TopicQueue> topics = new ConcurrentHashMap<>();
    private volatile boolean running = true;
    
    // Tạo hoặc lấy topic queue
    public TopicQueue getOrCreateTopic(String topicName, int maxSize) {
        return topics.computeIfAbsent(topicName, k -> new TopicQueue(k, maxSize));
    }
    
    // Publish message to topic
    public boolean publish(String topic, Message message) {
        TopicQueue topicQueue = topics.get(topic);
        if (topicQueue == null) {
            System.err.println("Topic not found: " + topic);
            return false;
        }
        
        return topicQueue.publish(message);
    }
    
    // Subscribe to topic
    public void subscribe(String topic, MessageConsumer consumer) {
        TopicQueue topicQueue = topics.get(topic);
        if (topicQueue == null) {
            System.err.println("Topic not found: " + topic);
            return;
        }
        
        topicQueue.addConsumer(consumer);
    }
    
    public void shutdown() {
        running = false;
        topics.values().forEach(TopicQueue::shutdown);
    }
    
    class TopicQueue {
        private final String topicName;
        private final LinkedBlockingQueue<Message> messageQueue;
        private final List<MessageConsumer> consumers = new ArrayList<>();
        private final Thread dispatcherThread;
        
        public TopicQueue(String topicName, int maxSize) {
            this.topicName = topicName;
            // LinkedBlockingQueue với large capacity cho high-throughput topics
            this.messageQueue = new LinkedBlockingQueue<>(maxSize);
            
            // Dispatcher thread để fan-out messages tới consumers
            this.dispatcherThread = new Thread(this::dispatchLoop, 
                "Dispatcher-" + topicName);
            this.dispatcherThread.start();
        }
        
        public boolean publish(Message message) {
            try {
                // offer với timeout để tránh block publishers
                boolean success = messageQueue.offer(message, 1, TimeUnit.SECONDS);
                
                if (success) {
                    System.out.println("📨 Published to " + topicName + ": " 
                        + message.getContent() + " (Queue: " + messageQueue.size() + ")");
                } else {
                    System.err.println("❌ Failed to publish to " + topicName 
                        + ": queue full");
                }
                
                return success;
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        
        public synchronized void addConsumer(MessageConsumer consumer) {
            consumers.add(consumer);
            System.out.println("➕ Added consumer to " + topicName 
                + " (Total: " + consumers.size() + ")");
        }
        
        private void dispatchLoop() {
            while (running) {
                try {
                    // take() block nếu không có message
                    // Perfect cho dispatcher: chỉ wake up khi có work
                    Message message = messageQueue.take();
                    
                    System.out.println("📤 Dispatching from " + topicName + ": " 
                        + message.getContent() + " to " + consumers.size() + " consumers");
                    
                    // Fan-out to all consumers
                    synchronized (this) {
                        for (MessageConsumer consumer : consumers) {
                            try {
                                consumer.consume(message);
                            } catch (Exception e) {
                                System.err.println("Consumer error: " + e.getMessage());
                            }
                        }
                    }
                    
                } catch (InterruptedException e) {
                    System.out.println("Dispatcher for " + topicName + " interrupted");
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        
        public void shutdown() {
            dispatcherThread.interrupt();
        }
        
        public int getQueueSize() {
            return messageQueue.size();
        }
    }
    
    static class Message {
        private final String content;
        private final long timestamp;
        private final String messageId;
        
        public Message(String content) {
            this.content = content;
            this.timestamp = System.currentTimeMillis();
            this.messageId = "msg-" + System.nanoTime();
        }
        
        public String getContent() { return content; }
        public long getTimestamp() { return timestamp; }
        public String getMessageId() { return messageId; }
    }
    
    @FunctionalInterface
    interface MessageConsumer {
        void consume(Message message) throws Exception;
    }
    
    // Demo usage
    public static void main(String[] args) throws InterruptedException {
        MessageBrokerSystem broker = new MessageBrokerSystem();
        
        // Create topics
        broker.getOrCreateTopic("user-events", 1000);
        broker.getOrCreateTopic("system-logs", 2000);
        broker.getOrCreateTopic("analytics", 500);
        
        // Add consumers
        broker.subscribe("user-events", message -> {
            System.out.println("👤 User Event Consumer: " + message.getContent());
            Thread.sleep(50); // Simulate processing
        });
        
        broker.subscribe("system-logs", message -> {
            System.out.println("📋 Log Consumer: " + message.getContent());
            Thread.sleep(30);
        });
        
        broker.subscribe("analytics", message -> {
            System.out.println("📊 Analytics Consumer: " + message.getContent());
            Thread.sleep(100);
        });
        
        // Publishers
        Thread userEventPublisher = new Thread(() -> {
            for (int i = 1; i <= 20; i++) {
                broker.publish("user-events", new Message("User login: user" + i));
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        
        Thread systemLogPublisher = new Thread(() -> {
            for (int i = 1; i <= 30; i++) {
                broker.publish("system-logs", new Message("System log entry " + i));
                try {
                    Thread.sleep(150);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        
        Thread analyticsPublisher = new Thread(() -> {
            for (int i = 1; i <= 15; i++) {
                broker.publish("analytics", new Message("Analytics data point " + i));
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        
        userEventPublisher.start();
        systemLogPublisher.start();
        analyticsPublisher.start();
        
        userEventPublisher.join();
        systemLogPublisher.join();
        analyticsPublisher.join();
        
        Thread.sleep(2000); // Let consumers finish
        broker.shutdown();
    }
}
```

#### 5.1.3 Batch Processing System

```java
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.ArrayList;

public class BatchProcessingSystem {
    
    // Tại sao LinkedBlockingQueue ideal cho batch processing:
    // 1. Variable batch sizes → dynamic memory allocation
    // 2. Unbounded input → không drop data khi spike load
    // 3. Background processing → blocking behavior perfect
    // 4. Memory efficient → chỉ allocate khi có data
    
    private final LinkedBlockingQueue<DataItem> inputQueue;
    private final Thread[] batchProcessors;
    private final int batchSize;
    private final long batchTimeoutMs;
    private volatile boolean shutdown = false;
    
    public BatchProcessingSystem(int numProcessors, int batchSize, long batchTimeoutMs) {
        // Unbounded queue để không drop data
        this.inputQueue = new LinkedBlockingQueue<>();
        this.batchSize = batchSize;
        this.batchTimeoutMs = batchTimeoutMs;
        this.batchProcessors = new Thread[numProcessors];
        
        // Start batch processor threads
        for (int i = 0; i < numProcessors; i++) {
            batchProcessors[i] = new Thread(this::batchProcessorLoop, 
                "BatchProcessor-" + i);
            batchProcessors[i].start();
        }
    }
    
    // Add data item for processing
    public void addItem(DataItem item) {
        try {
            // put() không bao giờ block với unbounded queue
            // Perfect cho data ingestion: không bao giờ drop data
            inputQueue.put(item);
            
            System.out.println("📥 Added item: " + item.getId() 
                + " (Queue size: " + inputQueue.size() + ")");
                
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Failed to add item: " + item.getId());
        }
    }
    
    // Batch processor loop
    private void batchProcessorLoop() {
        while (!shutdown) {
            try {
                List<DataItem> batch = collectBatch();
                
                if (!batch.isEmpty()) {
                    processBatch(batch);
                }
                
            } catch (InterruptedException e) {
                System.out.println("Batch processor " + Thread.currentThread().getName() 
                    + " interrupted");
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
    
    // Collect batch with size and timeout constraints
    private List<DataItem> collectBatch() throws InterruptedException {
        List<DataItem> batch = new ArrayList<>();
        long batchStartTime = System.currentTimeMillis();
        
        while (batch.size() < batchSize && !shutdown) {
            long remainingTimeout = batchTimeoutMs - (System.currentTimeMillis() - batchStartTime);
            
            if (remainingTimeout <= 0) {
                // Timeout reached, process current batch
                break;
            }
            
            // poll với remaining timeout
            // Nếu timeout, sẽ process batch hiện tại (có thể < batchSize)
            DataItem item = inputQueue.poll(remainingTimeout, TimeUnit.MILLISECONDS);
            
            if (item != null) {
                batch.add(item);
                System.out.println("📦 Added to batch: " + item.getId() 
                    + " (Batch size: " + batch.size() + ")");
            }
        }
        
        return batch;
    }
    
    // Process batch of items
    private void processBatch(List<DataItem> batch) {
        String processorName = Thread.currentThread().getName();
        System.out.println("🔄 " + processorName + " processing batch of " 
            + batch.size() + " items");
        
        try {
            // Simulate batch processing (database insert, API call, etc.)
            Thread.sleep(500 + batch.size() * 10);
            
            System.out.println("✅ " + processorName + " completed batch of " 
                + batch.size() + " items");
                
            // Log batch details
            for (DataItem item : batch) {
                System.out.println("  ✓ Processed: " + item.getId());
            }
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Batch processing interrupted");
        }
    }
    
    public void shutdown() {
        shutdown = true;
        
        // Interrupt all processors
        for (Thread processor : batchProcessors) {
            processor.interrupt();
        }
        
        // Process remaining items
        System.out.println("🔄 Processing remaining " + inputQueue.size() + " items...");
        
        List<DataItem> remainingItems = new ArrayList<>();
        inputQueue.drainTo(remainingItems);
        
        if (!remainingItems.isEmpty()) {
            processBatch(remainingItems);
        }
    }
    
    public int getQueueSize() {
        return inputQueue.size();
    }
    
    static class DataItem {
        private final String id;
        private final String data;
        private final long timestamp;
        
        public DataItem(String id, String data) {
            this.id = id;
            this.data = data;
            this.timestamp = System.currentTimeMillis();
        }
        
        public String getId() { return id; }
        public String getData() { return data; }
        public long getTimestamp() { return timestamp; }
    }
    
    // Demo usage
    public static void main(String[] args) throws InterruptedException {
        // Batch size = 5, timeout = 2 seconds
        BatchProcessingSystem batchSystem = new BatchProcessingSystem(2, 5, 2000);
        
        // Data producer - variable rate
        Thread dataProducer = new Thread(() -> {
            for (int i = 1; i <= 23; i++) {
                DataItem item = new DataItem("item-" + i, "data-" + i);
                batchSystem.addItem(item);
                
                try {
                    // Variable production rate
                    if (i % 7 == 0) {
                        // Pause occasionally to trigger timeout-based batching
                        Thread.sleep(2500);
                    } else {
                        Thread.sleep(100 + (int)(Math.random() * 200));
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        
        // Monitor thread
        Thread monitor = new Thread(() -> {
            for (int i = 0; i < 15; i++) {
                try {
                    Thread.sleep(1000);
                    System.out.println("📊 Queue size: " + batchSystem.getQueueSize());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        
        dataProducer.start();
        monitor.start();
        
        dataProducer.join();
        monitor.join();
        
        Thread.sleep(3000); // Let processors finish current batches
        batchSystem.shutdown();
    }
}
```

### 5.2 Best Practices

#### 5.2.1 Choosing Between Bounded and Unbounded

```java
public class BoundedVsUnboundedGuideline {
    
    // ✅ Khi nào dùng BOUNDED LinkedBlockingQueue
    public void boundedUseCase() {
        // 1. Web applications - tránh DoS
        LinkedBlockingQueue<HttpRequest> requestQueue = new LinkedBlockingQueue<>(1000);
        
        // 2. Resource-constrained environments
        LinkedBlockingQueue<Task> taskQueue = new LinkedBlockingQueue<>(500);
        
        // 3. Real-time systems - bounded latency
        LinkedBlockingQueue<Event> eventQueue = new LinkedBlockingQueue<>(100);
        
        // 4. Memory-sensitive applications
        LinkedBlockingQueue<DataPacket> dataQueue = new LinkedBlockingQueue<>(200);
    }
    
    // ✅ Khi nào dùng UNBOUNDED LinkedBlockingQueue  
    public void unboundedUseCase() {
        // 1. Batch processing - không drop data
        LinkedBlockingQueue<LogEntry> logQueue = new LinkedBlockingQueue<>();
        
        // 2. Message brokers - buffer spikes
        LinkedBlockingQueue<Message> messageQueue = new LinkedBlockingQueue<>();
        
        // 3. Data ingestion pipelines
        LinkedBlockingQueue<RawData> ingestionQueue = new LinkedBlockingQueue<>();
        
        // 4. Background processing - work when available
        LinkedBlockingQueue<BackgroundTask> bgQueue = new LinkedBlockingQueue<>();
    }
    
    // ⚠️ Hybrid approach - Start bounded, become unbounded under pressure
    public class AdaptiveQueue<T> {
        private LinkedBlockingQueue<T> queue;
        private final int normalCapacity;
        private volatile boolean pressureMode = false;
        
        public AdaptiveQueue(int normalCapacity) {
            this.normalCapacity = normalCapacity;
            this.queue = new LinkedBlockingQueue<>(normalCapacity);
        }
        
        public boolean offer(T item) {
            boolean added = queue.offer(item);
            
            if (!added && !pressureMode) {
                // Switch to unbounded mode under pressure
                System.out.println("🚨 Switching to pressure mode (unbounded)");
                switchToPressureMode();
                added = queue.offer(item);
            }
            
            return added;
        }
        
        private void switchToPressureMode() {
            LinkedBlockingQueue<T> oldQueue = queue;
            queue = new LinkedBlockingQueue<>(); // Unbounded
            
            // Transfer existing items
            oldQueue.drainTo(queue);
            pressureMode = true;
            
            // Schedule switch back to normal mode
            scheduleNormalModeCheck();
        }
        
        private void scheduleNormalModeCheck() {
            // Implementation: check periodically if queue size < normalCapacity
            // then switch back to bounded mode
        }
        
        public T take() throws InterruptedException {
            return queue.take();
        }
        
        public int size() {
            return queue.size();
        }
    }
}
```

#### 5.2.2 Error Handling và Recovery

```java
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class RobustLinkedBlockingQueueUsage {
    
    private final LinkedBlockingQueue<Task> primaryQueue;
    private final LinkedBlockingQueue<Task> deadLetterQueue;
    private final AtomicLong processedCount = new AtomicLong(0);
    private final AtomicLong failedCount = new AtomicLong(0);
    
    public RobustLinkedBlockingQueueUsage(int primaryCapacity) {
        this.primaryQueue = new LinkedBlockingQueue<>(primaryCapacity);
        // Dead letter queue cho failed tasks
        this.deadLetterQueue = new LinkedBlockingQueue<>();
    }
    
    // Robust producer với retry logic
    public boolean produce(Task task) {
        int maxRetries = 3;
        int retryCount = 0;
        
        while (retryCount < maxRetries) {
            try {
                // Try với timeout để tránh indefinite blocking
                boolean success = primaryQueue.offer(task, 1, TimeUnit.SECONDS);
                
                if (success) {
                    return true;
                }
                
                // Queue full, try again after backoff
                retryCount++;
                System.out.println("⚠️ Queue full, retry " + retryCount + "/" + maxRetries);
                
                // Exponential backoff
                Thread.sleep(100 * (1L << retryCount));
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("Producer interrupted while adding task");
                return false;
            }
        }
        
        // All retries failed, send to dead letter queue
        System.err.println("❌ Failed to add task after " + maxRetries + " retries");
        return handleFailedProduce(task);
    }
    
    private boolean handleFailedProduce(Task task) {
        try {
            // Dead letter queue usually unbounded để không lose data
            deadLetterQueue.put(task);
            System.out.println("📨 Sent to dead letter queue: " + task.getId());
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Failed to add to dead letter queue: " + task.getId());
            return false;
        }
    }
    
    // Robust consumer với error handling
    public void consume() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                // take() với proper interrupt handling
                Task task = primaryQueue.take();
                
                if (processTask(task)) {
                    processedCount.incrementAndGet();
                } else {
                    failedCount.incrementAndGet();
                    // Failed task có thể retry hoặc send to dead letter queue
                    handleFailedTask(task);
                }
                
            } catch (InterruptedException e) {
                System.out.println("Consumer interrupted, shutting down gracefully");
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
    
    private boolean processTask(Task task) {
        try {
            // Simulate task processing
            if (Math.random() < 0.1) { // 10% failure rate
                throw new RuntimeException("Simulated processing error");
            }
            
            Thread.sleep(100); // Simulate work
            System.out.println("✅ Processed: " + task.getId());
            return true;
            
        } catch (Exception e) {
            System.err.println("❌ Failed to process " + task.getId() + ": " + e.getMessage());
            return false;
        }
    }
    
    private void handleFailedTask(Task task) {
        if (task.getRetryCount() < 3) {
            // Retry với delay
            task.incrementRetryCount();
            
            // Re-queue với delay (có thể dùng ScheduledExecutorService)
            new Thread(() -> {
                try {
                    Thread.sleep(5000); // 5 second delay
                    primaryQueue.offer(task, 1, TimeUnit.SECONDS);
                    System.out.println("🔄 Retrying task: " + task.getId() 
                        + " (attempt " + task.getRetryCount() + ")");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
            
        } else {
            // Max retries exceeded, send to dead letter queue
            try {
                deadLetterQueue.put(task);
                System.out.println("💀 Sent to dead letter queue after max retries: " + task.getId());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
    
    // Graceful shutdown
    public void shutdown() {
        System.out.println("🔄 Starting graceful shutdown...");
        
        // Process remaining tasks in primary queue
        System.out.println("Processing remaining " + primaryQueue.size() + " tasks...");
        
        while (!primaryQueue.isEmpty()) {
            try {
                Task task = primaryQueue.poll(100, TimeUnit.MILLISECONDS);
                if (task != null) {
                    processTask(task);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        System.out.println("✅ Graceful shutdown completed");
        System.out.println("📊 Processed: " + processedCount.get() + ", Failed: " + failedCount.get());
        System.out.println("💀 Dead letter queue size: " + deadLetterQueue.size());
    }
    
    static class Task {
        private final String id;
        private int retryCount = 0;
        
        public Task(String id) {
            this.id = id;
        }
        
        public String getId() { return id; }
        public int getRetryCount() { return retryCount; }
        public void incrementRetryCount() { retryCount++; }
    }
}
```

### 5.3 Memory Management Best Practices

```java
import java.util.concurrent.LinkedBlockingQueue;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;

public class MemoryAwareQueueManagement {
    
    private static final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
    
    // ✅ Memory-aware queue với automatic capacity adjustment
    public static class MemoryAwareQueue<T> {
        private LinkedBlockingQueue<T> queue;
        private final long maxMemoryUsage;
        private final int initialCapacity;
        private volatile int currentCapacity;
        
        public MemoryAwareQueue(int initialCapacity, long maxMemoryUsageBytes) {
            this.initialCapacity = initialCapacity;
            this.maxMemoryUsage = maxMemoryUsageBytes;
            this.currentCapacity = initialCapacity;
            this.queue = new LinkedBlockingQueue<>(initialCapacity);
            
            // Start memory monitoring
            startMemoryMonitoring();
        }
        
        public boolean offer(T item) {
            // Check memory before adding
            if (getCurrentMemoryUsage() > maxMemoryUsage * 0.9) {
                System.out.println("⚠️ Memory pressure detected, rejecting new items");
                return false;
            }
            
            return queue.offer(item);
        }
        
        public T take() throws InterruptedException {
            T item = queue.take();
            
            // Check if we can expand queue after taking item
            if (getCurrentMemoryUsage() < maxMemoryUsage * 0.7 
                && currentCapacity < initialCapacity * 2) {
                expandQueueIfNeeded();
            }
            
            return item;
        }
        
        private void startMemoryMonitoring() {
            Thread monitor = new Thread(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        Thread.sleep(5000); // Check every 5 seconds
                        
                        long memoryUsage = getCurrentMemoryUsage();
                        double memoryPercentage = (double) memoryUsage / maxMemoryUsage * 100;
                        
                        System.out.printf("📊 Memory: %.1f%% (Queue size: %d, Capacity: %d)\n",
                            memoryPercentage, queue.size(), currentCapacity);
                        
                        if (memoryUsage > maxMemoryUsage * 0.8) {
                            shrinkQueueIfNeeded();
                        }
                        
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            });
            
            monitor.setDaemon(true);
            monitor.start();
        }
        
        private long getCurrentMemoryUsage() {
            return memoryBean.getHeapMemoryUsage().getUsed();
        }
        
        private void expandQueueIfNeeded() {
            // Implementation to expand queue capacity
            // Note: LinkedBlockingQueue capacity cannot be changed after creation
            // In practice, you might need to create new queue and transfer items
        }
        
        private void shrinkQueueIfNeeded() {
            // Trigger garbage collection to free memory
            System.gc();
            
            // In practice, might need to create smaller queue and transfer items
            System.out.println("🗑️ Memory pressure: triggered GC");
        }
        
        public int size() {
            return queue.size();
        }
    }
    
    // ✅ Best practice: Monitor queue growth
    public static class QueueGrowthMonitor {
        private final LinkedBlockingQueue<?> queue;
        private final String queueName;
        private long lastSize = 0;
        private long lastTime = System.currentTimeMillis();
        
        public QueueGrowthMonitor(LinkedBlockingQueue<?> queue, String queueName) {
            this.queue = queue;
            this.queueName = queueName;
            startMonitoring();
        }
        
        private void startMonitoring() {
            Thread monitor = new Thread(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        Thread.sleep(10000); // Check every 10 seconds
                        
                        long currentSize = queue.size();
                        long currentTime = System.currentTimeMillis();
                        
                        long sizeChange = currentSize - lastSize;
                        long timeElapsed = currentTime - lastTime;
                        
                        if (timeElapsed > 0) {
                            double growthRate = (double) sizeChange / timeElapsed * 1000; // per second
                            
                            System.out.printf("📈 %s growth rate: %.2f items/sec (size: %d)\n",
                                queueName, growthRate, currentSize);
                            
                            if (growthRate > 100) { // Growing too fast
                                System.out.println("🚨 WARNING: " + queueName + " growing rapidly!");
                                // Alert admin, scale consumers, etc.
                            }
                            
                            if (currentSize > 10000) { // Too large
                                System.out.println("🚨 WARNING: " + queueName + " size exceeds 10K!");
                            }
                        }
                        
                        lastSize = currentSize;
                        lastTime = currentTime;
                        
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            });
            
            monitor.setDaemon(true);
            monitor.start();
        }
    }
}
```

## 6. Lưu ý quan trọng và Pitfalls

### 6.1 Memory Leaks với Unbounded Queues

```java
public class MemoryLeakExamples {
    
    // ❌ NGUY HIỂM: Unbounded queue với fast producer, slow consumer
    public void memoryLeakScenario() {
        LinkedBlockingQueue<HeavyObject> queue = new LinkedBlockingQueue<>();
        
        // Fast producer
        Thread producer = new Thread(() -> {
            for (int i = 0; i < 1000000; i++) {
                queue.offer(new HeavyObject(1024 * 1024)); // 1MB objects
                // Producer tạo 1GB data nhưng consumer không theo kịp
                // → OutOfMemoryError!
            }
        });
        
        // Very slow consumer
        Thread consumer = new Thread(() -> {
            try {
                while (true) {
                    HeavyObject obj = queue.take();
                    Thread.sleep(1000); // Very slow processing
                    // Consumer chỉ process 1 object/second
                    // Producer tạo hàng nghìn objects/second
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        producer.start();
        consumer.start();
        // Kết quả: OutOfMemoryError sau vài giây
    }
    
    // ✅ ĐÚNG: Bounded queue với backpressure
    public void backpressureExample() {
        LinkedBlockingQueue<HeavyObject> queue = new LinkedBlockingQueue<>(100); // Limited
        
        Thread producer = new Thread(() -> {
            for (int i = 0; i < 1000000; i++) {
                try {
                    // put() sẽ block nếu queue đầy
                    // Điều này tạo backpressure → producer tự động chậm lại
                    queue.put(new HeavyObject(1024 * 1024));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        
        Thread consumer = new Thread(() -> {
            try {
                while (true) {
                    HeavyObject obj = queue.take();
                    Thread.sleep(100); // Reasonable processing time
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        producer.start();
        consumer.start();
        // Kết quả: Stable memory usage, producer chờ consumer
    }
    
    static class HeavyObject {
        private final byte[] data;
        
        public HeavyObject(int size) {
            this.data = new byte[size];
        }
    }
}
```

### 6.2 Interrupt Handling

```java
public class ProperInterruptHandling {
    
    // ❌ SAI: Không handle InterruptedException properly
    public void badInterruptHandling() {
        LinkedBlockingQueue<String> queue = new LinkedBlockingQueue<>();
        
        Thread worker = new Thread(() -> {
            while (true) {
                try {
                    String item = queue.take();
                    // Process item
                } catch (InterruptedException e) {
                    // ❌ SAI: Chỉ log error, không set interrupt flag
                    System.err.println("Interrupted");
                    // Thread tiếp tục chạy, không thể shutdown gracefully
                }
            }
        });
    }
    
    // ✅ ĐÚNG: Proper interrupt handling
    public void goodInterruptHandling() {
        LinkedBlockingQueue<String> queue = new LinkedBlockingQueue<>();
        
        Thread worker = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    String item = queue.take();
                    // Process item
                    
                } catch (InterruptedException e) {
                    // ✅ ĐÚNG: Restore interrupt status
                    System.out.println("Worker interrupted, shutting down gracefully");
                    Thread.currentThread().interrupt();
                    break; // Exit loop
                }
            }
            
            // Cleanup code
            System.out.println("Worker shutdown completed");
        });
        
        // To shutdown:
        // worker.interrupt();
        // worker.join(); // Wait for graceful shutdown
    }
    
    // ✅ BEST PRACTICE: Timeout với interrupt checking
    public void robustWaitingWithTimeout() {
        LinkedBlockingQueue<String> queue = new LinkedBlockingQueue<>();
        
        Thread worker = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    // poll() với timeout thay vì take() blocking indefinitely
                    String item = queue.poll(1, TimeUnit.SECONDS);
                    
                    if (item != null) {
                        // Process item
                        processItem(item);
                    } else {
                        // Timeout - check interrupt status periodically
                        // Điều này cho phép responsive shutdown
                        System.out.println("No work available, checking for shutdown...");
                    }
                    
                } catch (InterruptedException e) {
                    System.out.println("Interrupted during wait");
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
    }
    
    private void processItem(String item) {
        // Process item with interrupt checking
        if (Thread.currentThread().isInterrupted()) {
            return; // Exit early if interrupted
        }
        
        // Do work...
    }
}
```

### 6.3 Performance Pitfalls

```java
public class PerformancePitfalls {
    
    // ❌ BAD: Frequent size() calls
    public void expensiveSizeCalls() {
        LinkedBlockingQueue<String> queue = new LinkedBlockingQueue<>();
        
        // size() method cần lock và traverse
        // Không nên call thường xuyên trong hot path
        while (true) {
            if (queue.size() > 1000) { // ❌ Expensive call
                // Handle large queue
            }
            
            // Add item
            queue.offer("item");
        }
    }
    
    // ✅ GOOD: Cache size hoặc dùng AtomicInteger counter
    public void efficientSizeTracking() {
        LinkedBlockingQueue<String> queue = new LinkedBlockingQueue<>();
        AtomicInteger queueSize = new AtomicInteger(0);
        
        // Track size separately
        if (queueSize.get() > 1000) { // ✅ Fast atomic read
            // Handle large queue
        }
        
        // Update counter when adding/removing
        if (queue.offer("item")) {
            queueSize.incrementAndGet();
        }
    }
    
    // ❌ BAD: Wrong queue choice cho use case
    public void wrongQueueChoice() {
        // Nếu chỉ có 1 producer và 1 consumer
        LinkedBlockingQueue<String> queue = new LinkedBlockingQueue<>();
        // Two-lock design overkill, ArrayBlockingQueue sẽ nhanh hơn
        
        // Nếu cần priority ordering
        LinkedBlockingQueue<Task> taskQueue = new LinkedBlockingQueue<>();
        // Nên dùng PriorityBlockingQueue thay vì
        
        // Nếu cần direct handoff
        LinkedBlockingQueue<Work> workQueue = new LinkedBlockingQueue<>(1);
        // SynchronousQueue sẽ phù hợp hơn
    }
    
    // ✅ GOOD: Choose right queue for use case
    public void rightQueueChoice() {
        // Single producer/consumer → ArrayBlockingQueue
        ArrayBlockingQueue<String> singlePair = new ArrayBlockingQueue<>(1000);
        
        // Priority needed → PriorityBlockingQueue
        PriorityBlockingQueue<Task> priorityQueue = new PriorityBlockingQueue<>();
        
        // Direct handoff → SynchronousQueue
        SynchronousQueue<Work> handoff = new SynchronousQueue<>();
        
        // Multiple producers/consumers với variable capacity → LinkedBlockingQueue
        LinkedBlockingQueue<Item> multiQueue = new LinkedBlockingQueue<>();
    }
}
```

## 7. Kết luận

### 7.1 Khi nào nên dùng LinkedBlockingQueue

**✅ Nên dùng khi:**
- Cần **variable capacity** hoặc **unbounded** queue
- Có **multiple producers và consumers** (two-lock design hiệu quả)
- **Memory efficiency** quan trọng (dynamic allocation)
- Không biết trước **peak load** hoặc **burst patterns**
- Cần **flexibility** giữa bounded và unbounded modes
- **Background processing** hoặc **batch systems**

**❌ Không nên dùng khi:**
- **Single producer/consumer** → ArrayBlockingQueue nhanh hơn
- Cần **fixed capacity** và biết trước exact size → ArrayBlockingQueue
- **Priority ordering** cần thiết → PriorityBlockingQueue
- **Direct handoff** pattern → SynchronousQueue
- **Memory constrained** environment với known limits → ArrayBlockingQueue

### 7.2 So sánh tổng quan với các queue khác

```
Queue Performance & Use Case Comparison:

LinkedBlockingQueue:
├── Performance: Good (two-lock design)
├── Memory: Dynamic (allocate on demand)
├── Capacity: Bounded or Unbounded
├── Use Case: Variable load, multiple producers/consumers
└── Best For: Web servers, message brokers, batch systems

ArrayBlockingQueue:
├── Performance: Better (single lock, array-based)
├── Memory: Fixed (pre-allocated)
├── Capacity: Fixed bounded
├── Use Case: Known capacity, consistent load
└── Best For: Thread pools, fixed-size buffers

ConcurrentLinkedQueue:
├── Performance: Best (lock-free)
├── Memory: Dynamic
├── Capacity: Unbounded only
├── Use Case: High throughput, no blocking needed
└── Best For: Producer-consumer với spinning consumers

SynchronousQueue:
├── Performance: Good for direct handoff
├── Memory: None (no storage)
├── Capacity: 0 (direct transfer)
├── Use Case: Direct producer-consumer handoff
└── Best For: Task delegation, work stealing
```

### 7.3 Key Takeaways

**🔧 Technical Advantages:**
- **Two-lock design**: Put và take operations có thể parallel
- **Dynamic memory**: Chỉ allocate khi cần, memory efficient
- **Flexible capacity**: Có thể bounded hoặc unbounded
- **FIFO ordering**: Đảm bảo thứ tự xử lý

**📊 Performance Characteristics:**
- **Throughput**: Tốt, đặc biệt với multiple producers/consumers
- **Latency**: Reasonable, cao hơn ArrayBlockingQueue một chút
- **Memory overhead**: Thấp hơn Array khi queue không full
- **Scalability**: Excellent với concurrent access

**⚠️ Common Pitfalls:**
- **Unbounded queues**: Risk của OutOfMemoryError
- **Frequent size() calls**: Performance impact
- **Wrong use case**: Chọn queue không phù hợp
- **Interrupt handling**: Cần handle InterruptedException properly

**LinkedBlockingQueue** là lựa chọn excellent cho majority of producer-consumer scenarios, đặc biệt khi cần flexibility và efficiency với variable workloads!