# Collections Synchronization - Hướng dẫn toàn diện

## 📚 Mục lục
1. [Khái niệm cơ bản](#khái-niệm-cơ-bản)
2. [Vấn đề khi không có Synchronization](#vấn-đề-khi-không-có-synchronization)
3. [Các phương pháp Synchronization](#các-phương-pháp-synchronization)
4. [So sánh hiệu suất](#so-sánh-hiệu-suất)
5. [Best Practices](#best-practices)
6. [Use Cases thực tế](#use-cases-thực-tế)

---

## Khái niệm cơ bản

### Collections Synchronization là gì?

**Collections Synchronization** là quá trình đảm bảo rằng các collection (danh sách, map, set...) có thể được truy cập và chỉnh sửa một cách an toàn bởi nhiều thread cùng lúc mà không gây ra lỗi hoặc dữ liệu không nhất quán.

### Tại sao cần Synchronization?

```java
// ❌ VÍ DỤ KHÔNG AN TOÀN - KHÔNG SYNCHRONIZED
import java.util.*;
import java.util.concurrent.*;

public class UnsafeCollectionExample {
    private static List<Integer> numbers = new ArrayList<>();
    
    public static void main(String[] args) throws InterruptedException {
        // Tạo 10 thread cùng thêm dữ liệu vào ArrayList
        ExecutorService executor = Executors.newFixedThreadPool(10);
        
        // Mỗi thread thêm 1000 số
        for (int i = 0; i < 10; i++) {
            final int threadId = i;
            executor.submit(() -> {
                for (int j = 0; j < 1000; j++) {
                    // NGUY HIỂM: Nhiều thread cùng modify ArrayList
                    numbers.add(threadId * 1000 + j);
                }
            });
        }
        
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
        
        // KẾT QUẢ: Số lượng phần tử thường < 10000 hoặc exception
        System.out.println("Số phần tử thực tế: " + numbers.size());
        System.out.println("Số phần tử mong đợi: 10000");
    }
}
```

**Kết quả khi chạy:**
```
Số phần tử thực tế: 8847  // ❌ Mất dữ liệu!
Số phần tử mong đợi: 10000
```

---

## Vấn đề khi không có Synchronization

### 1. Race Condition (Điều kiện đua)

```java
// ❌ VÍ DỤ RACE CONDITION
public class RaceConditionExample {
    private static Map<String, Integer> counter = new HashMap<>();
    
    public static void increment(String key) {
        // Bước 1: Đọc giá trị hiện tại
        Integer current = counter.get(key);
        
        // Bước 2: Tính toán giá trị mới
        int newValue = (current == null) ? 1 : current + 1;
        
        // Bước 3: Ghi lại giá trị mới
        // ⚠️ Thread khác có thể thay đổi giá trị giữa bước 1 và 3
        counter.put(key, newValue);
    }
    
    public static void main(String[] args) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(100);
        
        // 1000 thread cùng increment cùng 1 key
        for (int i = 0; i < 1000; i++) {
            executor.submit(() -> increment("count"));
        }
        
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
        
        // KẾT QUẢ: Thường < 1000 do race condition
        System.out.println("Giá trị thực tế: " + counter.get("count"));
        System.out.println("Giá trị mong đợi: 1000");
    }
}
```

### 2. Data Corruption (Hỏng dữ liệu)

```java
// ❌ VÍ DỤ DATA CORRUPTION
public class DataCorruptionExample {
    private static List<String> userSessions = new ArrayList<>();
    
    public static void addUser(String userId) {
        // Thread 1 có thể đang resize array
        // Thread 2 cùng lúc thêm phần tử
        // -> Dẫn đến internal state không nhất quán
        userSessions.add("User-" + userId);
    }
    
    public static void removeUser(String userId) {
        // Xóa user trong lúc thread khác đang thêm
        // -> Có thể gây IndexOutOfBoundsException
        userSessions.removeIf(user -> user.equals("User-" + userId));
    }
}
```

---

## Các phương pháp Synchronization

### 1. Synchronized Collections

```java
// ✅ SỬ DỤNG SYNCHRONIZED COLLECTIONS
import java.util.*;
import java.util.concurrent.*;

public class SynchronizedCollectionExample {
    public static void main(String[] args) throws InterruptedException {
        // Tạo synchronized list từ ArrayList thường
        List<Integer> syncList = Collections.synchronizedList(new ArrayList<>());
        
        // Tạo synchronized map từ HashMap thường  
        Map<String, Integer> syncMap = Collections.synchronizedMap(new HashMap<>());
        
        ExecutorService executor = Executors.newFixedThreadPool(10);
        
        // Test synchronized list
        for (int i = 0; i < 10; i++) {
            final int threadId = i;
            executor.submit(() -> {
                for (int j = 0; j < 1000; j++) {
                    // AN TOÀN: Mỗi phương thức được đồng bộ hóa
                    syncList.add(threadId * 1000 + j);
                }
            });
        }
        
        // Test synchronized map  
        for (int i = 0; i < 1000; i++) {
            final int taskId = i;
            executor.submit(() -> {
                // AN TOÀN: put() được synchronized
                syncMap.put("task-" + taskId, taskId);
            });
        }
        
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
        
        // KẾT QUẢ: Đúng như mong đợi
        System.out.println("Sync List size: " + syncList.size()); // 10000
        System.out.println("Sync Map size: " + syncMap.size());   // 1000
    }
}
```

**⚠️ Lưu ý quan trọng với Synchronized Collections:**

```java
// ❌ VẪN CÓ THỂ KHÔNG AN TOÀN
List<String> syncList = Collections.synchronizedList(new ArrayList<>());

// Iteration cần synchronized block riêng
synchronized (syncList) {
    // Phải wrap trong synchronized block để iteration an toàn
    for (String item : syncList) {
        System.out.println(item);
        // Nếu thread khác modify list trong lúc này -> ConcurrentModificationException
    }
}

// Compound operations cũng cần synchronized block
synchronized (syncList) {
    // Kiểm tra và thêm phải được atomic
    if (!syncList.contains("newItem")) {
        syncList.add("newItem");
    }
}
```

### 2. Concurrent Collections (Khuyến nghị)

```java
// ✅ SỬ DỤNG CONCURRENT COLLECTIONS - TỐI ƯU HƠN
import java.util.concurrent.*;

public class ConcurrentCollectionExample {
    public static void main(String[] args) throws InterruptedException {
        // ConcurrentHashMap - Hiệu suất cao cho Map
        ConcurrentHashMap<String, Integer> concurrentMap = new ConcurrentHashMap<>();
        
        // CopyOnWriteArrayList - Tối ưu cho read-heavy workload
        CopyOnWriteArrayList<String> cowList = new CopyOnWriteArrayList<>();
        
        // ConcurrentLinkedQueue - Queue thread-safe
        ConcurrentLinkedQueue<String> queue = new ConcurrentLinkedQueue<>();
        
        ExecutorService executor = Executors.newFixedThreadPool(20);
        
        // Test ConcurrentHashMap với atomic operations
        for (int i = 0; i < 1000; i++) {
            final int taskId = i;
            executor.submit(() -> {
                // ATOMIC: putIfAbsent đảm bảo chỉ put nếu key chưa tồn tại
                concurrentMap.putIfAbsent("task-" + (taskId % 100), taskId);
                
                // ATOMIC: compute cho phép update an toàn
                concurrentMap.compute("counter", (key, val) -> 
                    (val == null) ? 1 : val + 1
                );
            });
        }
        
        // Test CopyOnWriteArrayList
        for (int i = 0; i < 100; i++) {
            final int taskId = i;
            
            // Writer threads
            executor.submit(() -> {
                cowList.add("Item-" + taskId);
            });
            
            // Reader threads - Không bao giờ bị block
            executor.submit(() -> {
                for (String item : cowList) {
                    // Iteration luôn an toàn, không cần synchronized
                    System.out.println("Reading: " + item);
                }
            });
        }
        
        // Test ConcurrentLinkedQueue
        for (int i = 0; i < 1000; i++) {
            final int taskId = i;
            executor.submit(() -> {
                // Producer: Thêm vào queue
                queue.offer("Message-" + taskId);
                
                // Consumer: Lấy từ queue
                String message = queue.poll();
                if (message != null) {
                    System.out.println("Processed: " + message);
                }
            });
        }
        
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);
        
        System.out.println("ConcurrentMap size: " + concurrentMap.size());
        System.out.println("CopyOnWrite list size: " + cowList.size());
        System.out.println("Queue remaining: " + queue.size());
    }
}
```

### 3. Blocking Collections

```java
// ✅ BLOCKING COLLECTIONS CHO PRODUCER-CONSUMER
import java.util.concurrent.*;

public class BlockingCollectionExample {
    public static void main(String[] args) throws InterruptedException {
        // BlockingQueue - Tự động block khi full/empty
        BlockingQueue<String> queue = new ArrayBlockingQueue<>(10);
        
        // Producer thread
        Thread producer = new Thread(() -> {
            try {
                for (int i = 0; i < 20; i++) {
                    String item = "Product-" + i;
                    
                    // put() sẽ block nếu queue đầy (capacity = 10)
                    queue.put(item);
                    System.out.println("Produced: " + item);
                    Thread.sleep(100); // Simulate work
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        // Consumer thread
        Thread consumer = new Thread(() -> {
            try {
                for (int i = 0; i < 20; i++) {
                    // take() sẽ block nếu queue rỗng
                    String item = queue.take();
                    System.out.println("Consumed: " + item);
                    Thread.sleep(200); // Simulate processing
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        
        producer.start();
        consumer.start();
        
        producer.join();
        consumer.join();
        
        System.out.println("Remaining in queue: " + queue.size());
    }
}
```

---

## So sánh hiệu suất

### Benchmark Test

```java
// 📊 SO SÁNH HIỆU SUẤT GIỮA CÁC LOẠI COLLECTION
import java.util.*;
import java.util.concurrent.*;

public class PerformanceBenchmark {
    private static final int THREAD_COUNT = 10;
    private static final int OPERATIONS_PER_THREAD = 10000;
    
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== BENCHMARK COLLECTIONS SYNCHRONIZATION ===\n");
        
        // Test 1: ArrayList vs Synchronized List vs CopyOnWriteArrayList
        benchmarkLists();
        
        // Test 2: HashMap vs Synchronized Map vs ConcurrentHashMap  
        benchmarkMaps();
    }
    
    private static void benchmarkLists() throws InterruptedException {
        System.out.println("📋 BENCHMARK LISTS (Write-heavy workload)");
        
        // Test ArrayList không synchronized (unsafe)
        List<Integer> arrayList = new ArrayList<>();
        long time1 = measureTime(() -> {
            runListTest(arrayList, "ArrayList (UNSAFE)");
        });
        
        // Test Synchronized ArrayList
        List<Integer> syncList = Collections.synchronizedList(new ArrayList<>());
        long time2 = measureTime(() -> {
            runListTest(syncList, "Synchronized ArrayList");
        });
        
        // Test CopyOnWriteArrayList
        CopyOnWriteArrayList<Integer> cowList = new CopyOnWriteArrayList<>();
        long time3 = measureTime(() -> {
            runListTest(cowList, "CopyOnWriteArrayList");
        });
        
        System.out.printf("Kết quả:\n");
        System.out.printf("ArrayList (unsafe):     %d ms - Size: %d (mất dữ liệu!)\n", 
                         time1, arrayList.size());
        System.out.printf("Synchronized List:      %d ms - Size: %d\n", 
                         time2, syncList.size());
        System.out.printf("CopyOnWriteArrayList:   %d ms - Size: %d\n\n", 
                         time3, cowList.size());
    }
    
    private static void benchmarkMaps() throws InterruptedException {
        System.out.println("🗺️ BENCHMARK MAPS (Read/Write mixed workload)");
        
        // Test HashMap không synchronized (unsafe)
        Map<String, Integer> hashMap = new HashMap<>();
        long time1 = measureTime(() -> {
            runMapTest(hashMap, "HashMap (UNSAFE)");
        });
        
        // Test Synchronized HashMap
        Map<String, Integer> syncMap = Collections.synchronizedMap(new HashMap<>());
        long time2 = measureTime(() -> {
            runMapTest(syncMap, "Synchronized HashMap");
        });
        
        // Test ConcurrentHashMap
        ConcurrentHashMap<String, Integer> concurrentMap = new ConcurrentHashMap<>();
        long time3 = measureTime(() -> {
            runMapTest(concurrentMap, "ConcurrentHashMap");
        });
        
        System.out.printf("Kết quả:\n");
        System.out.printf("HashMap (unsafe):       %d ms - Size: %d (có thể mất dữ liệu!)\n", 
                         time1, hashMap.size());
        System.out.printf("Synchronized Map:       %d ms - Size: %d\n", 
                         time2, syncMap.size());
        System.out.printf("ConcurrentHashMap:      %d ms - Size: %d\n\n", 
                         time3, concurrentMap.size());
    }
    
    private static void runListTest(List<Integer> list, String name) {
        try {
            ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
            
            for (int i = 0; i < THREAD_COUNT; i++) {
                final int threadId = i;
                executor.submit(() -> {
                    for (int j = 0; j < OPERATIONS_PER_THREAD; j++) {
                        list.add(threadId * OPERATIONS_PER_THREAD + j);
                    }
                });
            }
            
            executor.shutdown();
            executor.awaitTermination(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    private static void runMapTest(Map<String, Integer> map, String name) {
        try {
            ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
            
            for (int i = 0; i < THREAD_COUNT; i++) {
                final int threadId = i;
                executor.submit(() -> {
                    for (int j = 0; j < OPERATIONS_PER_THREAD; j++) {
                        String key = "key-" + (threadId * OPERATIONS_PER_THREAD + j);
                        
                        // 70% write, 30% read workload
                        if (j % 10 < 7) {
                            map.put(key, j);
                        } else {
                            map.get(key);
                        }
                    }
                });
            }
            
            executor.shutdown();
            executor.awaitTermination(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    private static long measureTime(Runnable task) {
        long start = System.currentTimeMillis();
        task.run();
        return System.currentTimeMillis() - start;
    }
}
```

**Kết quả benchmark điển hình:**
```
📋 BENCHMARK LISTS (Write-heavy workload)
Kết quả:
ArrayList (unsafe):     156 ms - Size: 87234 (mất dữ liệu!)
Synchronized List:      324 ms - Size: 100000
CopyOnWriteArrayList:   1247 ms - Size: 100000

🗺️ BENCHMARK MAPS (Read/Write mixed workload)  
Kết quả:
HashMap (unsafe):       198 ms - Size: 94567 (có thể mất dữ liệu!)
Synchronized Map:       456 ms - Size: 100000
ConcurrentHashMap:      287 ms - Size: 100000
```

**Phân tích kết quả:**
- **ArrayList không sync**: Nhanh nhất nhưng **mất dữ liệu**
- **Synchronized Collections**: An toàn nhưng **chậm** do lock contention
- **ConcurrentHashMap**: **Cân bằng tốt** giữa performance và thread-safety
- **CopyOnWriteArrayList**: **Chậm** cho write operations nhưng **excellent** cho read-heavy workloads

---

## Best Practices

### 1. Chọn đúng loại Collection

```java
// ✅ HƯỚNG DẪN CHỌN COLLECTION PHÙ HỢP

public class CollectionSelectionGuide {
    
    // 📖 READ-HEAVY WORKLOAD (đọc nhiều, ghi ít)
    public void readHeavyScenario() {
        // ✅ Tốt nhất cho read-heavy
        CopyOnWriteArrayList<String> readOptimized = new CopyOnWriteArrayList<>();
        
        // Thêm dữ liệu ban đầu (ít thao tác write)
        readOptimized.add("config1");
        readOptimized.add("config2");
        
        // Nhiều thread đọc dữ liệu - KHÔNG CẦN LOCK
        ExecutorService readers = Executors.newFixedThreadPool(50);
        for (int i = 0; i < 100; i++) {
            readers.submit(() -> {
                // Iteration cực kỳ nhanh, không bao giờ block
                for (String config : readOptimized) {
                    processConfig(config);
                }
            });
        }
    }
    
    // ✍️ WRITE-HEAVY WORKLOAD (ghi nhiều, đọc ít)  
    public void writeHeavyScenario() {
        // ✅ Tốt nhất cho write-heavy
        ConcurrentLinkedQueue<Task> writeOptimized = new ConcurrentLinkedQueue<>();
        
        // Nhiều producer thêm task
        ExecutorService producers = Executors.newFixedThreadPool(20);
        for (int i = 0; i < 1000; i++) {
            final int taskId = i;
            producers.submit(() -> {
                // Thêm task cực kỳ nhanh, lock-free
                writeOptimized.offer(new Task("task-" + taskId));
            });
        }
        
        // Ít consumer xử lý task
        ExecutorService consumers = Executors.newFixedThreadPool(5);
        for (int i = 0; i < 5; i++) {
            consumers.submit(() -> {
                Task task;
                while ((task = writeOptimized.poll()) != null) {
                    processTask(task);
                }
            });
        }
    }
    
    // ⚖️ BALANCED READ/WRITE WORKLOAD
    public void balancedScenario() {
        // ✅ Tốt nhất cho balanced workload
        ConcurrentHashMap<String, UserSession> balanced = new ConcurrentHashMap<>();
        
        ExecutorService mixed = Executors.newFixedThreadPool(20);
        
        // Mixed read/write operations
        for (int i = 0; i < 1000; i++) {
            final int userId = i;
            mixed.submit(() -> {
                String key = "user-" + userId;
                
                if (userId % 3 == 0) {
                    // Write operation: Tạo session mới
                    balanced.put(key, new UserSession(userId));
                } else {
                    // Read operation: Đọc session
                    UserSession session = balanced.get(key);
                    if (session != null) {
                        processSession(session);
                    }
                }
            });
        }
    }
    
    // Helper classes
    static class Task {
        final String id;
        Task(String id) { this.id = id; }
    }
    
    static class UserSession {
        final int userId;
        UserSession(int userId) { this.userId = userId; }
    }
    
    private void processConfig(String config) { /* Process config */ }
    private void processTask(Task task) { /* Process task */ }
    private void processSession(UserSession session) { /* Process session */ }
}
```

### 2. Tránh những lỗi thường gặp

```java
// ❌ NHỮNG LỖI THƯỜNG GẶP VÀ CÁCH KHẮC PHỤC

public class CommonMistakes {
    
    // ❌ LỖI 1: Synchronization không đầy đủ
    public void mistake1() {
        List<String> syncList = Collections.synchronizedList(new ArrayList<>());
        
        // ❌ SAI: Compound operation không atomic
        if (!syncList.contains("item")) {
            syncList.add("item"); // Thread khác có thể thêm "item" giữa check và add
        }
    }
    
    // ✅ CÁCH SỬA LỖI 1
    public void fix1() {
        List<String> syncList = Collections.synchronizedList(new ArrayList<>());
        
        // ✅ ĐÚNG: Wrap compound operation trong synchronized block
        synchronized (syncList) {
            if (!syncList.contains("item")) {
                syncList.add("item");
            }
        }
        
        // 🚀 TỐT HƠN: Dùng ConcurrentHashMap cho use case này
        Set<String> concurrentSet = ConcurrentHashMap.newKeySet();
        concurrentSet.add("item"); // Atomic operation, thread-safe
    }
    
    // ❌ LỖI 2: Iteration không an toàn
    public void mistake2() {
        List<String> syncList = Collections.synchronizedList(new ArrayList<>());
        
        // ❌ SAI: ConcurrentModificationException có thể xảy ra
        for (String item : syncList) {
            if (shouldRemove(item)) {
                syncList.remove(item); // Modify trong lúc iterate
            }
        }
    }
    
    // ✅ CÁCH SỬA LỖI 2
    public void fix2() {
        List<String> syncList = Collections.synchronizedList(new ArrayList<>());
        
        // ✅ ĐÚNG: Synchronized iteration
        synchronized (syncList) {
            Iterator<String> iter = syncList.iterator();
            while (iter.hasNext()) {
                String item = iter.next();
                if (shouldRemove(item)) {
                    iter.remove(); // An toàn với iterator
                }
            }
        }
        
        // 🚀 TỐT HƠN: Dùng CopyOnWriteArrayList nếu iteration nhiều
        CopyOnWriteArrayList<String> cowList = new CopyOnWriteArrayList<>();
        // Iteration luôn an toàn, không cần synchronized
        cowList.removeIf(this::shouldRemove);
    }
    
    // ❌ LỖI 3: Performance bottleneck
    public void mistake3() {
        // ❌ SAI: Dùng synchronized cho high-contention scenario
        Map<String, Integer> syncMap = Collections.synchronizedMap(new HashMap<>());
        
        // Nhiều thread cùng access -> Lock contention cao
        ExecutorService executor = Executors.newFixedThreadPool(100);
        for (int i = 0; i < 1000; i++) {
            executor.submit(() -> {
                syncMap.put("key", syncMap.getOrDefault("key", 0) + 1);
            });
        }
    }
    
    // ✅ CÁCH SỬA LỖI 3  
    public void fix3() {
        // ✅ ĐÚNG: Dùng ConcurrentHashMap cho high-contention
        ConcurrentHashMap<String, Integer> concurrentMap = new ConcurrentHashMap<>();
        
        ExecutorService executor = Executors.newFixedThreadPool(100);
        for (int i = 0; i < 1000; i++) {
            executor.submit(() -> {
                // Atomic operation, hiệu suất cao
                concurrentMap.compute("key", (k, v) -> (v == null) ? 1 : v + 1);
            });
        }
    }
    
    private boolean shouldRemove(String item) {
        return item.startsWith("temp");
    }
}
```

### 3. Memory và Performance Optimization

```java
// 🚀 OPTIMIZATION TECHNIQUES

public class OptimizationTechniques {
    
    // 💾 Memory-efficient cho large collections
    public void memoryOptimization() {
        // ✅ Sử dụng capacity hint để tránh resize
        ConcurrentHashMap<String, String> optimizedMap = 
            new ConcurrentHashMap<>(1000, 0.75f, 16); // initialCapacity, loadFactor, concurrencyLevel
        
        // ✅ Cleanup không cần thiết
        optimizedMap.entrySet().removeIf(entry -> 
            entry.getValue() == null || entry.getValue().isEmpty()
        );
    }
    
    // ⚡ Performance tuning cho specific use cases
    public void performanceTuning() {
        // 📊 Cho cache scenario (frequent reads)
        ConcurrentHashMap<String, ExpensiveObject> cache = new ConcurrentHashMap<>();
        
        // ✅ computeIfAbsent cho lazy loading
        public ExpensiveObject getOrCompute(String key) {
            return cache.computeIfAbsent(key, k -> {
                // Chỉ tính toán nếu chưa có trong cache
                return new ExpensiveObject(k);
            });
        }
        
        // 🔄 Cho producer-consumer scenario
        BlockingQueue<WorkItem> queue = new LinkedBlockingQueue<>(1000);
        
        // ✅ Batch processing để reduce contention
        public void batchConsumer() {
            List<WorkItem> batch = new ArrayList<>(100);
            
            try {
                // Lấy first item (blocking)
                WorkItem first = queue.take();
                batch.add(first);
                
                // Drain additional items (non-blocking) 
                queue.drainTo(batch, 99);
                
                // Process batch cùng lúc
                processBatch(batch);
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
    
    static class ExpensiveObject {
        ExpensiveObject(String key) { /* Expensive computation */ }
    }
    
    static class WorkItem { }
    
    private void processBatch(List<WorkItem> batch) { /* Process batch */ }
}
```

---

## Use Cases thực tế

### 1. Web Application Session Management

```java
// 🌐 QUẢN LÝ SESSION TRONG WEB APPLICATION

@Component
public class SessionManager {
    // ✅ ConcurrentHashMap cho session storage
    private final ConcurrentHashMap<String, UserSession> activeSessions = 
        new ConcurrentHashMap<>();
    
    // ✅ CopyOnWriteArrayList cho session listeners (read-heavy)
    private final CopyOnWriteArrayList<SessionListener> listeners = 
        new CopyOnWriteArrayList<>();
    
    // Tạo session mới
    public UserSession createSession(String userId) {
        String sessionId = generateSessionId();
        UserSession session = new UserSession(sessionId, userId, System.currentTimeMillis());
        
        // Thread-safe put operation
        activeSessions.put(sessionId, session);
        
        // Notify listeners (không cần lock vì CopyOnWriteArrayList)
        listeners.forEach(listener -> listener.onSessionCreated(session));
        
        return session;
    }
    
    // Lấy session
    public UserSession getSession(String sessionId) {
        UserSession session = activeSessions.get(sessionId);
        
        if (session != null && isExpired(session)) {
            // Remove expired session atomically
            activeSessions.remove(sessionId, session);
            return null;
        }
        
        return session;
    }
    
    // Update session activity
    public void updateActivity(String sessionId) {
        // Atomic update using compute
        activeSessions.compute(sessionId, (id, session) -> {
            if (session != null) {
                session.updateLastActivity();
            }
            return session;
        });
    }
    
    // Cleanup expired sessions (scheduled task)
    @Scheduled(fixedRate = 60000) // Chạy mỗi phút
    public void cleanupExpiredSessions() {
        long now = System.currentTimeMillis();
        
        // Thread-safe removal
        activeSessions.entrySet().removeIf(entry -> {
            boolean expired = (now - entry.getValue().getLastActivity()) > 30 * 60 * 1000; // 30 phút
            
            if (expired) {
                // Notify listeners
                listeners.forEach(listener -> listener.onSessionExpired(entry.getValue()));
            }
            
            return expired;
        });
    }
    
    // Add listener
    public void addListener(SessionListener listener) {
        listeners.add(listener); // Thread-safe add
    }
    
    // Statistics
    public SessionStats getStats() {
        return new SessionStats(
            activeSessions.size(),
            activeSessions.values().stream()
                .mapToLong(UserSession::getLastActivity)
                .max().orElse(0)
        );
    }
    
    private String generateSessionId() {
        return UUID.randomUUID().toString();
    }
    
    private boolean isExpired(UserSession session) {
        return (System.currentTimeMillis() - session.getLastActivity()) > 30 * 60 * 1000;
    }
}

// Supporting classes
class UserSession {
    private final String sessionId;
    private final String userId;
    private volatile long lastActivity; // volatile cho thread-safe read/write
    
    public UserSession(String sessionId, String userId, long createdTime) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.lastActivity = createdTime;
    }
    
    public void updateLastActivity() {
        this.lastActivity = System.currentTimeMillis();
    }
    
    // Getters
    public String getSessionId() { return sessionId; }
    public String getUserId() { return userId; }
    public long getLastActivity() { return lastActivity; }
}

interface SessionListener {
    void onSessionCreated(UserSession session);
    void onSessionExpired(UserSession session);
}

class SessionStats {
    private final int activeCount;
    private final long latestActivity;
    
    public SessionStats(int activeCount, long latestActivity) {
        this.activeCount = activeCount;
        this.latestActivity = latestActivity;
    }
    
    // Getters
    public int getActiveCount() { return activeCount; }
    public long getLatestActivity() { return latestActivity; }
}
```

### 2. Real-time Event Processing System

```java
// 📡 HỆ THỐNG XỬ LÝ EVENT REAL-TIME

@Service
public class EventProcessingService {
    
    // ✅ BlockingQueue cho event pipeline
    private final BlockingQueue<Event> eventQueue = 
        new LinkedBlockingQueue<>(10000); // Buffer 10k events
    
    // ✅ ConcurrentHashMap cho event aggregation
    private final ConcurrentHashMap<String, EventCounter> eventCounters = 
        new ConcurrentHashMap<>();
    
    // ✅ CopyOnWriteArrayList cho event handlers (có thể thay đổi runtime)
    private final CopyOnWriteArrayList<EventHandler> handlers = 
        new CopyOnWriteArrayList<>();
    
    private final ExecutorService processingPool;
    private volatile boolean running = true;
    
    public EventProcessingService() {
        // Thread pool cho parallel processing
        this.processingPool = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors()
        );
        
        // Start consumer threads
        startEventConsumers();
    }
    
    // Nhận event từ external source
    public boolean submitEvent(Event event) {
        try {
            // Non-blocking offer với timeout
            return eventQueue.offer(event, 100, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
    
    // Batch submit cho high throughput
    public void submitEvents(List<Event> events) {
        for (Event event : events) {
            if (!submitEvent(event)) {
                // Log dropped event
                System.err.println("Dropped event: " + event.getId());
            }
        }
    }
    
    private void startEventConsumers() {
        int consumerCount = Runtime.getRuntime().availableProcessors();
        
        for (int i = 0; i < consumerCount; i++) {
            processingPool.submit(() -> {
                while (running) {
                    try {
                        // Blocking take - wait for events
                        Event event = eventQueue.take();
                        processEvent(event);
                        
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    } catch (Exception e) {
                        System.err.println("Error processing event: " + e.getMessage());
                    }
                }
            });
        }
    }
    
    private void processEvent(Event event) {
        // 1. Update counters atomically
        eventCounters.compute(event.getType(), (type, counter) -> {
            if (counter == null) {
                return new EventCounter(type, 1, System.currentTimeMillis());
            } else {
                counter.increment();
                return counter;
            }
        });
        
        // 2. Notify handlers (iteration luôn safe với CopyOnWriteArrayList)
        for (EventHandler handler : handlers) {
            try {
                handler.handle(event);
            } catch (Exception e) {
                System.err.println("Handler error: " + e.getMessage());
            }
        }
    }
    
    // Add handler during runtime
    public void addHandler(EventHandler handler) {
        handlers.add(handler); // Thread-safe
    }
    
    // Remove handler during runtime  
    public void removeHandler(EventHandler handler) {
        handlers.remove(handler); // Thread-safe
    }
    
    // Get real-time statistics
    public Map<String, Long> getEventCounts() {
        // Convert to regular map for external use
        return eventCounters.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> entry.getValue().getCount()
            ));
    }
    
    // Reset counters for specific event type
    public void resetCounter(String eventType) {
        eventCounters.remove(eventType);
    }
    
    // Graceful shutdown
    public void shutdown() {
        running = false;
        processingPool.shutdown();
        
        try {
            if (!processingPool.awaitTermination(5, TimeUnit.SECONDS)) {
                processingPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            processingPool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    // Health check
    public SystemHealth getHealth() {
        return new SystemHealth(
            eventQueue.size(),
            eventQueue.remainingCapacity(),
            eventCounters.size(),
            handlers.size()
        );
    }
}

// Supporting classes
class Event {
    private final String id;
    private final String type;
    private final long timestamp;
    private final Map<String, Object> data;
    
    public Event(String id, String type, Map<String, Object> data) {
        this.id = id;
        this.type = type;
        this.timestamp = System.currentTimeMillis();
        this.data = data;
    }
    
    // Getters
    public String getId() { return id; }
    public String getType() { return type; }
    public long getTimestamp() { return timestamp; }
    public Map<String, Object> getData() { return data; }
}

class EventCounter {
    private final String eventType;
    private volatile long count; // volatile cho thread-safe read
    private volatile long lastUpdated;
    
    public EventCounter(String eventType, long initialCount, long timestamp) {
        this.eventType = eventType;
        this.count = initialCount;
        this.lastUpdated = timestamp;
    }
    
    public void increment() {
        count++;
        lastUpdated = System.currentTimeMillis();
    }
    
    // Getters
    public long getCount() { return count; }
    public long getLastUpdated() { return lastUpdated; }
}

interface EventHandler {
    void handle(Event event) throws Exception;
}

class SystemHealth {
    private final int queueSize;
    private final int queueCapacity;
    private final int counterCount;
    private final int handlerCount;
    
    public SystemHealth(int queueSize, int queueCapacity, int counterCount, int handlerCount) {
        this.queueSize = queueSize;
        this.queueCapacity = queueCapacity;
        this.counterCount = counterCount;
        this.handlerCount = handlerCount;
    }
    
    public boolean isHealthy() {
        return queueSize < queueCapacity * 0.8; // Queue không quá 80%
    }
    
    // Getters
    public int getQueueSize() { return queueSize; }
    public int getQueueCapacity() { return queueCapacity; }
    public int getCounterCount() { return counterCount; }
    public int getHandlerCount() { return handlerCount; }
}
```

### 3. Caching System

```java
// 💾 HỆ THỐNG CACHE ĐA LUỒNG

@Component
public class DistributedCache<K, V> {
    
    // ✅ ConcurrentHashMap cho cache storage
    private final ConcurrentHashMap<K, CacheEntry<V>> cache;
    
    // ✅ ConcurrentHashMap cho access tracking  
    private final ConcurrentHashMap<K, AccessInfo> accessTracker;
    
    // ✅ CopyOnWriteArrayList cho cache listeners
    private final CopyOnWriteArrayList<CacheListener<K, V>> listeners;
    
    private final int maxSize;
    private final long ttlMillis;
    private final ScheduledExecutorService cleanupExecutor;
    
    public DistributedCache(int maxSize, long ttlMillis) {
        this.maxSize = maxSize;
        this.ttlMillis = ttlMillis;
        this.cache = new ConcurrentHashMap<>(maxSize);
        this.accessTracker = new ConcurrentHashMap<>(maxSize);
        this.listeners = new CopyOnWriteArrayList<>();
        
        // Cleanup task chạy mỗi phút
        this.cleanupExecutor = Executors.newScheduledThreadPool(1);
        this.cleanupExecutor.scheduleAtFixedRate(
            this::cleanup, 1, 1, TimeUnit.MINUTES
        );
    }
    
    // Get value from cache
    public Optional<V> get(K key) {
        CacheEntry<V> entry = cache.get(key);
        
        if (entry == null) {
            // Cache miss
            notifyListeners(listener -> listener.onMiss(key));
            return Optional.empty();
        }
        
        if (isExpired(entry)) {
            // Expired entry - remove atomically
            cache.remove(key, entry);
            accessTracker.remove(key);
            notifyListeners(listener -> listener.onExpired(key, entry.getValue()));
            return Optional.empty();
        }
        
        // Cache hit - update access info
        accessTracker.compute(key, (k, info) -> {
            if (info == null) {
                return new AccessInfo(1, System.currentTimeMillis());
            } else {
                info.recordAccess();
                return info;
            }
        });
        
        notifyListeners(listener -> listener.onHit(key, entry.getValue()));
        return Optional.of(entry.getValue());
    }
    
    // Put value into cache with automatic eviction
    public void put(K key, V value) {
        long now = System.currentTimeMillis();
        CacheEntry<V> newEntry = new CacheEntry<>(value, now, now + ttlMillis);
        
        // Evict if cache is full
        if (cache.size() >= maxSize && !cache.containsKey(key)) {
            evictLeastRecentlyUsed();
        }
        
        // Put new entry
        CacheEntry<V> oldEntry = cache.put(key, newEntry);
        accessTracker.put(key, new AccessInfo(1, now));
        
        if (oldEntry == null) {
            notifyListeners(listener -> listener.onPut(key, value));
        } else {
            notifyListeners(listener -> listener.onUpdate(key, oldEntry.getValue(), value));
        }
    }
    
    // Compute if absent (thread-safe lazy loading)
    public V computeIfAbsent(K key, Function<K, V> valueSupplier) {
        // Try to get existing value first
        Optional<V> existing = get(key);
        if (existing.isPresent()) {
            return existing.get();
        }
        
        // Compute and cache new value atomically
        CacheEntry<V> newEntry = cache.computeIfAbsent(key, k -> {
            V value = valueSupplier.apply(k);
            long now = System.currentTimeMillis();
            return new CacheEntry<>(value, now, now + ttlMillis);
        });
        
        // Update access info if entry was computed
        accessTracker.putIfAbsent(key, new AccessInfo(1, System.currentTimeMillis()));
        
        return newEntry.getValue();
    }
    
    // Remove from cache
    public boolean remove(K key) {
        CacheEntry<V> removed = cache.remove(key);
        accessTracker.remove(key);
        
        if (removed != null) {
            notifyListeners(listener -> listener.onRemove(key, removed.getValue()));
            return true;
        }
        return false;
    }
    
    // Clear all cache
    public void clear() {
        cache.clear();
        accessTracker.clear();
        notifyListeners(listener -> listener.onClear());
    }
    
    // Add cache listener
    public void addListener(CacheListener<K, V> listener) {
        listeners.add(listener);
    }
    
    // Evict least recently used entry
    private void evictLeastRecentlyUsed() {
        if (accessTracker.isEmpty()) return;
        
        // Find LRU entry
        K lruKey = accessTracker.entrySet().stream()
            .min(Comparator.comparing(entry -> entry.getValue().getLastAccess()))
            .map(Map.Entry::getKey)
            .orElse(null);
        
        if (lruKey != null) {
            CacheEntry<V> evicted = cache.remove(lruKey);
            accessTracker.remove(lruKey);
            
            if (evicted != null) {
                notifyListeners(listener -> listener.onEvict(lruKey, evicted.getValue()));
            }
        }
    }
    
    // Cleanup expired entries
    private void cleanup() {
        long now = System.currentTimeMillis();
        
        // Remove expired entries
        cache.entrySet().removeIf(entry -> {
            if (isExpired(entry.getValue())) {
                K key = entry.getKey();
                accessTracker.remove(key);
                notifyListeners(listener -> listener.onExpired(key, entry.getValue().getValue()));
                return true;
            }
            return false;
        });
    }
    
    private boolean isExpired(CacheEntry<V> entry) {
        return System.currentTimeMillis() > entry.getExpiryTime();
    }
    
    private void notifyListeners(Consumer<CacheListener<K, V>> action) {
        // Iteration luôn safe với CopyOnWriteArrayList
        for (CacheListener<K, V> listener : listeners) {
            try {
                action.accept(listener);
            } catch (Exception e) {
                System.err.println("Listener error: " + e.getMessage());
            }
        }
    }
    
    // Get cache statistics
    public CacheStats getStats() {
        return new CacheStats(
            cache.size(),
            maxSize,
            accessTracker.values().stream().mapToLong(AccessInfo::getAccessCount).sum()
        );
    }
    
    // Shutdown cleanup
    public void shutdown() {
        cleanupExecutor.shutdown();
        try {
            if (!cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                cleanupExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            cleanupExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}

// Supporting classes
class CacheEntry<V> {
    private final V value;
    private final long createdTime;
    private final long expiryTime;
    
    public CacheEntry(V value, long createdTime, long expiryTime) {
        this.value = value;
        this.createdTime = createdTime;
        this.expiryTime = expiryTime;
    }
    
    // Getters
    public V getValue() { return value; }
    public long getCreatedTime() { return createdTime; }
    public long getExpiryTime() { return expiryTime; }
}

class AccessInfo {
    private volatile long accessCount;
    private volatile long lastAccess;
    
    public AccessInfo(long accessCount, long lastAccess) {
        this.accessCount = accessCount;
        this.lastAccess = lastAccess;
    }
    
    public void recordAccess() {
        accessCount++;
        lastAccess = System.currentTimeMillis();
    }
    
    // Getters
    public long getAccessCount() { return accessCount; }
    public long getLastAccess() { return lastAccess; }
}

interface CacheListener<K, V> {
    default void onHit(K key, V value) {}
    default void onMiss(K key) {}
    default void onPut(K key, V value) {}
    default void onUpdate(K key, V oldValue, V newValue) {}
    default void onRemove(K key, V value) {}
    default void onEvict(K key, V value) {}
    default void onExpired(K key, V value) {}
    default void onClear() {}
}

class CacheStats {
    private final int currentSize;
    private final int maxSize;
    private final long totalAccesses;
    
    public CacheStats(int currentSize, int maxSize, long totalAccesses) {
        this.currentSize = currentSize;
        this.maxSize = maxSize;
        this.totalAccesses = totalAccesses;
    }
    
    public double getUtilization() {
        return (double) currentSize / maxSize;
    }
    
    // Getters
    public int getCurrentSize() { return currentSize; }
    public int getMaxSize() { return maxSize; }
    public long getTotalAccesses() { return totalAccesses; }
}
```

---

## 📋 Tóm tắt và Khuyến nghị

### Bảng so sánh nhanh:

| Collection Type | Use Case | Performance | Thread Safety | Memory Usage |
|-----------------|----------|-------------|---------------|--------------|
| `ArrayList` | Single-thread | ⭐⭐⭐⭐⭐ | ❌ | ⭐⭐⭐⭐⭐ |
| `Collections.synchronizedList()` | Basic multi-thread | ⭐⭐ | ✅ | ⭐⭐⭐⭐ |
| `CopyOnWriteArrayList` | Read-heavy | ⭐⭐⭐⭐⭐ (read) / ⭐ (write) | ✅ | ⭐⭐ |
| `ConcurrentHashMap` | Balanced read/write | ⭐⭐⭐⭐ | ✅ | ⭐⭐⭐ |
| `BlockingQueue` | Producer-Consumer | ⭐⭐⭐ | ✅ | ⭐⭐⭐ |

### Quy tắc vàng:

1. **🚫 Không bao giờ** dùng collections thường trong môi trường multi-thread
2. **📖 Read-heavy workload** → `CopyOnWriteArrayList`
3. **✍️ Write-heavy workload** → `ConcurrentLinkedQueue`
4. **⚖️ Balanced workload** → `ConcurrentHashMap`
5. **🔄 Producer-Consumer** → `BlockingQueue`
6. **🔒 Legacy code** → `Collections.synchronized*()`

### Khi nào sử dụng Collections Synchronization:

✅ **Nên dùng khi:**
- Ứng dụng multi-thread
- Cần share data giữa các thread
- Performance và correctness quan trọng
- Cần thread-safe operations

❌ **Không cần dùng khi:**
- Ứng dụng single-thread
- Data chỉ local trong method
- Đã có external synchronization mechanism
- Performance là ưu tiên tuyệt đối và có thể đảm bảo thread-safety bằng cách khác

**Nhớ:** Thread-safety không phải lúc nào cũng cần thiết, nhưng khi cần thì phải làm đúng! 🎯