
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * LRU Cache implementation.
 *
 * Three implementations provided:
 * 
 *   LRUCache          — Hand-rolled HashMap + Doubly Linked List. O(1) get/put
 *   LRUCacheSimple    — Java's LinkedHashMap with access-order mode. Same complexity
 *   ThreadSafeLRUCache — ReentrantLock wrapper around LRUCache for concurrent use
 * 
 *
 * Complexity summary
 * ------------------
 *             get     put     space
 * LRUCache    O(1)    O(1)    O(capacity)

 */
public class LRUCache<K, V> {

    

    /**
     * A single node in the doubly linked list.
     *
     * Stores both key AND value so that during eviction we can remove
     * the tail node from the HashMap in O(1) — the key is right on the node,
     * no reverse lookup needed.
     */
    private static class Node<K, V> {
        K key;
        V value;
        Node<K, V> prev;
        Node<K, V> next;

        Node(K key, V value) {
            this.key   = key;
            this.value = value;
        }
    }

   

    private final int capacity;
    private final HashMap<K, Node<K, V>> map;

    // entinel head — MRU side. Never holds real data. 
    private final Node<K, V> head;

    //  Sentinel tail — LRU side. Never holds real data. 
    private final Node<K, V> tail;

    
    /**
     * Creates an LRU Cache with the given capacity.
     *
     * @param capacity maximum number of key-value pairs (must be >= 1)
     * @throws IllegalArgumentException if capacity < 1
     */
    public LRUCache(int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException("Capacity must be >= 1, got " + capacity);
        }
        this.capacity = capacity;
        this.map      = new HashMap<>(capacity);

        // Sentinel nodes — wire them together to form an empty list.
        this.head = new Node<>(null, null);
        this.tail = new Node<>(null, null);
        head.next = tail;
        tail.prev = head;
    }

   

    /**
     * Returns the value for the given key, or null if not present.
     *
     * Marks the key as most-recently-used by moving its node to the
     * front of the list.
     *
     * Time complexity: O(1)
     *
     * @param key the lookup key
     * @return associated value, or null if absent
     */
    public V get(K key) {
        Node<K, V> node = map.get(key);
        if (node == null) {
            return null;
        }
        moveToFront(node);
        return node.value;
    }

    /**
     * Inserts or updates the key with the given value.
     *
     * If the key already exists, its value is updated and it becomes MRU.
     * If the cache is at capacity, the least-recently-used entry is evicted
     * before the new entry is inserted.
     *
     * Time complexity: O(1)
     *
     * @param key   the key
     * @param value the value to store
     */
    public void put(K key, V value) {
        if (map.containsKey(key)) {
            Node<K, V> node = map.get(key);
            node.value = value;
            moveToFront(node);
            return;
        }

        if (map.size() == capacity) {
            evictLRU();
        }

        Node<K, V> newNode = new Node<>(key, value);
        map.put(key, newNode);
        insertAtFront(newNode);
    }

    

    
    //  Returns the current number of entries in the cache.
    
    public int size() {
        return map.size();
    }

    /**
     * Returns true if the cache contains the given key.
     * Does NOT update recency (peek semantics).
     */
    public boolean contains(K key) {
        return map.containsKey(key);
    }

    /**
     * Returns the value for the given key WITHOUT updating its recency.
     * Useful for inspecting cache state in tests without side-effects.
     * Returns null if the key is not present.
     */
    public V peek(K key) {
        Node<K, V> node = map.get(key);
        return (node != null) ? node.value : null;
    }

    /**
     * Returns a list of keys ordered from LRU (next-to-evict) to MRU.
     * Walks the DLL from tail to head. O(n) — for debugging/testing only.
     */
    public List<K> evictionOrder() {
        List<K> order = new ArrayList<>();
        Node<K, V> cur = tail.prev;
        while (cur != head) {
            order.add(cur.key);
            cur = cur.prev;
        }
        return order;
    }

    

    // Insert node immediately after the head sentinel (MRU position). 
    private void insertAtFront(Node<K, V> node) {
        node.prev      = head;
        node.next      = head.next;
        head.next.prev = node;
        head.next      = node;
    }

    // Unlink node from wherever it sits in the list. 
    private void remove(Node<K, V> node) {
        node.prev.next = node.next;
        node.next.prev = node.prev;
    }

    //  Move an existing node to the MRU position. 
    private void moveToFront(Node<K, V> node) {
        remove(node);
        insertAtFront(node);
    }

    //  Remove the least-recently-used node (just before tail sentinel). 
    private void evictLRU() {
        Node<K, V> lruNode = tail.prev;
        if (lruNode == head) return;   // empty — should never happen
        remove(lruNode);
        map.remove(lruNode.key);
    }


    

    /**
     * LRU Cache using Java's LinkedHashMap in access-order mode.
     *
     *  LinkedHashMap(capacity, loadFactor, accessOrder=true) maintains
     * insertion order by default; with  accessOrder=true it reorders on
     * every get/put, which is exactly LRU semantics. Overriding
     * removeEldestEntry  triggers automatic eviction at capacity.
     *
     * Semantically identical to LRUCache but in fewer lines.
     * Shown here to demonstrate library awareness; the hand-rolled version
     * above is preferred for interviews because it reveals the mechanism.
     *
     * Complexity: O(1) get, O(1) put — same as  LRUCache.
     */
    public static class LRUCacheSimple<K, V> {

        private final LinkedHashMap<K, V> cache;

        public LRUCacheSimple(int capacity) {
            // accessOrder=true: get() and put() move the entry to the tail (MRU).
            // removeEldestEntry evicts the head (LRU) when over capacity.
            this.cache = new LinkedHashMap<K, V>(capacity, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                    return size() > capacity;
                }
            };
        }

        public V get(K key) {
            return cache.get(key);
        }

        public void put(K key, V value) {
            cache.put(key, value);
        }
    }


    

    /**
     * Thread-safe LRU Cache.
     *
     * Wraps  LRUCache with a  ReentrantLock so that
     * get and  put are each executed atomically.
     * No two threads can mutate the DLL or the HashMap simultaneously.
     *
     * Trade-off: A single coarse-grained lock serialises ALL
     * operations. For read-heavy workloads, a  java.util.concurrent.locks.ReadWriteLock
     * would allow concurrent reads. For very high throughput, sharding the
     * cache into N independent instances (each with its own lock) reduces
     * contention by a factor of N.
     *
     * 
     * Usage:
     *   ThreadSafeLRUCache<Integer, String> cache = new ThreadSafeLRUCache<>(128);
     *   cache.put(1, "A");
     *   cache.get(1);  // returns "A"
     */
    public static class ThreadSafeLRUCache<K, V> {

        private final LRUCache<K, V> cache;
        private final ReentrantLock lock = new ReentrantLock();

        public ThreadSafeLRUCache(int capacity) {
            this.cache = new LRUCache<>(capacity);
        }

        public V get(K key) {
            lock.lock();
            try {
                return cache.get(key);
            } finally {
                lock.unlock();
            }
        }

        public void put(K key, V value) {
            lock.lock();
            try {
                cache.put(key, value);
            } finally {
                lock.unlock();
            }
        }

        public int size() {
            lock.lock();
            try {
                return cache.size();
            } finally {
                lock.unlock();
            }
        }
    }
}
