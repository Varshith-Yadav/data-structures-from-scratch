# data-structures-from-scratch

Solutions to two classic systems design problems — an LRU Cache and an Event Scheduler — built from first principles. Both problems are solved in Python and Java, with full test suites and a detailed analysis of the design decisions behind each implementation.

---

## Table of Contents

- [Project Structure](#project-structure)
- [Problem 1 — LRU Cache](#problem-1--lru-cache)
- [Problem 2 — Event Scheduler](#problem-2--event-scheduler)
- [Final Discussion & Analysis](#final-discussion--analysis)
- [Running the Tests](#running-the-tests)

---

## Project Structure

```
Assignment/
|-- .gitignore
|-- EventScheduler.java
|-- EventSchedulerTests.java
|-- LRUCache.java
|-- LRUCacheTests.java
`-- README.md
```

---

## Problem 1 — LRU Cache

### The idea

An LRU (Least Recently Used) cache keeps the most recently accessed items and throws out the oldest ones when it runs out of space. The challenge is doing both `get` and `put` in O(1) time.

The trick is combining two data structures that cover each other's weaknesses:

- A **hash map** tells you where any item is in O(1). But a hash map alone has no concept of order — it can't tell you which item was used least recently.
- A **doubly linked list** maintains order perfectly — you can move any node to the front, and the tail always holds the oldest item. But walking a list to find a specific key is O(n).

Together: the hash map gives you the node instantly, and the linked list lets you reorder or remove that node in O(1) because you already have a direct pointer to it.

### Implementation details worth noting

**Sentinel nodes.** The list uses two dummy `head` and `tail` nodes that never hold real data. Real items always sit between them. This removes every edge case — empty list, single item, removing the first or last element — because the boundary nodes always exist and you never touch them.

**The node stores its own key.** During eviction, you remove the tail node and need to delete it from the hash map too. If the node only stored the value, you'd have to scan backwards through the map to find which key it belongs to. Storing the key on the node means the eviction is one `del map[node.key]` call — still O(1).

**Two implementations, one concept.** The codebase includes both the hand-rolled `LRUCache` (HashMap + DLL) and `LRUCacheSimple` which uses Python's `OrderedDict` / Java's `LinkedHashMap`. The hand-rolled version is what you'd actually write in an interview. The library version is there to show awareness of the standard tools.

### Complexity

| Operation | Time | Space |
|-----------|------|-------|
| `get(key)` | O(1) | O(1) |
| `put(key, value)` | O(1) | O(1) |
| Cache total | — | O(capacity) |

Every operation — hash map lookup, DLL insert, DLL remove, DLL move — is O(1). The cache holds at most `capacity` nodes in both the map and the list, so space is bounded by that.

---

## Problem 2 — Event Scheduler

### The idea

Two scheduling problems that sound related but need different approaches.

**`can_attend_all`** — can one person attend every event without conflicts?

Sort the events by start time. Then walk through consecutive pairs: if event A ends after event B starts, they overlap and the answer is false. Adjacent events (where one ends exactly when the next begins) are not considered overlaps, so the check is `prev_end > curr_start`, not `>=`. One pass after sorting — the sort is the expensive part.

**`min_rooms_required`** — how many rooms do you need to run all events simultaneously?

The intuition: at any given moment, the number of rooms in use equals the number of events currently running. The question becomes — at peak overlap, how many events are running at once?

The efficient approach is a min-heap of end times. Sort events by start time, then process them one by one. For each new event, check if the earliest-finishing room (heap top) is done before this event starts. If yes, reuse that room. If no, open a new one. Push the new event's end time onto the heap.

Why a min-heap specifically? You only ever need to know whether the *soonest-ending* room is free. A min-heap gives you that in O(1) and rebalances in O(log n). A plain list would cost O(n) per check.

**`assign_rooms`** — same heap logic, extended to track actual room labels.

The heap stores `(end_time, room_label)` pairs instead of just end times. When a room frees up, you get its label back and reassign it. New rooms are named A, B, … Z, AA, AB, … (spreadsheet-column style) so the scheduler never runs out of names regardless of how many concurrent rooms are needed.

### Complexity

| Operation | Time | Space |
|-----------|------|-------|
| `can_attend_all` | O(n log n) | O(n) |
| `min_rooms_required` | O(n log n) | O(n) |
| `assign_rooms` | O(n log n) | O(n) |

Space is O(n) for `can_attend_all` because we sort a copy of the input rather than sorting in place — this is intentional; mutating the caller's list would be a surprising side effect.

---

## Final Discussion & Analysis

### 1. Time & Space Complexity

Full breakdown across all functions:

**LRU Cache**

| Function | Time | Space | Notes |
|----------|------|-------|-------|
| `get(key)` | O(1) | O(1) | HashMap lookup + DLL move-to-front |
| `put(key, value)` | O(1) | O(1) | HashMap insert + DLL insert/evict |
| `_insert_at_front` | O(1) | O(1) | Two pointer rewires |
| `_remove` | O(1) | O(1) | Two pointer rewires |
| `_evict_lru` | O(1) | O(1) | Remove tail node + map delete |
| Cache total | — | O(capacity) | Map and list share the same nodes |

**Event Scheduler**

| Function | Time | Space | Notes |
|----------|------|-------|-------|
| `can_attend_all` | O(n log n) | O(n) | Sort dominates; sorted copy avoids mutation |
| `min_rooms_required` | O(n log n) | O(n) | Sort + n heap ops at O(log n) each |
| `assign_rooms` | O(n log n) | O(n) | Same as above; heap stores labels too |

---

### 2. Trade-offs: Why HashMap + Doubly Linked List?

The requirement is O(1) for both lookup and order updates. No single structure handles both.

**HashMap alone** — O(1) lookup but completely unordered. No way to know which key was used least recently without scanning everything.

**Doubly linked list alone** — perfect ordering, O(1) insert/remove/move once you have a pointer to a node. But finding a node by key requires scanning from the head — O(n).

**Together** — the map stores key → node pointers. When you `get(key)`, the map jumps straight to the node in O(1), then the list moves it to the front in O(1). When capacity is exceeded, the list's tail holds the LRU node and the node's stored key lets you remove it from the map in O(1).

Why a *doubly* linked list and not singly linked? A singly linked list can't delete a node in O(1) because you need the predecessor to update its `next` pointer — you'd have to scan from the head to find it. The `prev` pointer on each node is what makes O(1) deletion possible from any position.

| Structure | Lookup | Order tracking | O(1) delete from middle |
|-----------|--------|----------------|------------------------|
| Array | O(1) | Possible | No — O(n) shifting |
| Singly linked list | O(n) | Yes | No — needs predecessor |
| HashMap only | O(1) | No | N/A |
| HashMap + DLL | O(1) | Yes | Yes |

---

### 3. Future Proofing: Assigning Room Numbers

The `assign_rooms` method is already implemented and handles this directly. The core change from `min_rooms_required` is storing room labels alongside end times in the heap.

```python
# min_rooms_required: heap stores end times only
heapq.heappush(end_times, end)

# assign_rooms: heap stores (end_time, room_label) pairs
heapq.heappush(heap, (end, room_label))
```

When a room frees up, you pop its label off the heap and reassign it — guaranteeing minimum room usage while keeping labels stable. Room names follow spreadsheet-column ordering (A → Z → AA → AB …) so the system handles any number of concurrent rooms without needing to preallocate or hardcode a limit.

This pattern maps directly to real systems — Google Calendar's conference room picker, AWS resource slot allocation, CI/CD pipeline runner assignment. The scheduler doesn't care what the "rooms" represent; it just ensures each one is occupied by at most one event at any point in time.

---

### 4. Concurrency: Making the LRU Cache Thread-Safe

The base `LRUCache` is not thread-safe. Two threads running `put` simultaneously can both decide to evict, both modify the linked list, and leave the DLL in a corrupted state with dangling pointers. The map and the list get out of sync.

The solution in this codebase is `ThreadSafeLRUCache`, which wraps the cache with a `ReentrantLock` (Java) / `threading.Lock` (Python). Every `get` and `put` acquires the lock before touching any shared state and releases it in a `finally` block so it always gets released even if an exception is thrown.

```java
public int get(int key) {
    lock.lock();
    try {
        return cache.get(key);
    } finally {
        lock.unlock();   // always runs, even on exception
    }
}
```

**Trade-offs of this approach:**

| Strategy | Pros | Cons |
|----------|------|------|
| Single lock (current) | Simple, correct, easy to reason about | Serialises all operations — readers block each other |
| ReadWriteLock | Multiple concurrent reads allowed | More complex; writes still fully block |
| Sharded cache | Reduces contention by factor of N shards | N× memory; cross-shard operations need careful design |

For most applications, a single lock is the right starting point — it's provably correct and the overhead is negligible unless you're hitting millions of operations per second. ReadWriteLock makes sense for read-heavy workloads. Sharding makes sense when profiling shows the lock is actually the bottleneck.

---

### Closing

The goal throughout was to reach O(1) where the problem permits it and not do extra work where it doesn't. The sentinel node pattern eliminates edge-case branches. The min-heap in the scheduler avoids rescanning freed rooms on every event. The thread-safe wrapper is designed so the locking logic is completely separate from the cache logic — you can reason about each in isolation.

These are patterns that show up in production systems: CPU caches, CDN edge nodes, database buffer pools, and cloud scheduler resource managers all implement variations of LRU and interval scheduling. The implementations here are deliberately close to how you'd write them in a real codebase — typed, documented, tested, and built to be extended.

---

## Running the Tests

**Python**

```bash
# Install dependencies
pip install pytest

# Run all tests
pytest tests/ -v

# Run individual files
pytest tests/test_lru_cache.py -v
pytest tests/test_event_scheduler.py -v
```

**Java**

```bash
# Compile
javac java/LRUCache.java java/EventScheduler.java

# Run with JUnit (add junit-platform-console-standalone.jar to classpath)
java -cp .:junit.jar org.junit.platform.console.ConsoleLauncher --scan-classpath
```

**Test coverage summary**

| File | Tests | What's covered |
|------|-------|----------------|
| `test_lru_cache.py` | 45 | get/put contract, eviction order, recency updates, DLL integrity, sentinel nodes, thread safety under concurrent reads and writes |
| `test_event_scheduler.py` | 27 | empty input, single event, adjacent events, all-overlap, large disjoint, room reuse, room label overflow past Z, custom prefix |
