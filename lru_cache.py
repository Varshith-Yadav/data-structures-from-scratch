from __future__ import annotations
import threading
from typing import Generic, TypeVar, Hashable, Optional


K = TypeVar("K", bound=Hashable)
V = TypeVar("V")


class Node(Generic[K, V]):
    def __init__(self, key: Optional[K], value: Optional[V]) -> None:
        self.key = key
        self.value = value
        self.prev: Optional[Node[K, V]] = None
        self.next: Optional[Node[K, V]] = None


class LRUCache(Generic[K, V]):

    def __init__(self, capacity: int):
        self.capacity = capacity
        self.cache: dict[K, Node[K, V]] = {}

        self.head = Node(None, None)  # dummy
        self.tail = Node(None, None)  # dummy

        self.head.next = self.tail
        self.tail.prev = self.head

        # for concurrency
        self.lock = threading.Lock()

    # ---------------- DLL OPERATIONS ---------------- #

    def _add_node(self, node: Node[K, V]) -> None:
        node.prev = self.tail.prev
        node.next = self.tail

        assert self.tail.prev is not None
        self.tail.prev.next = node
        self.tail.prev = node

    def _remove_node(self, node: Node[K, V]) -> None:
        assert node.prev is not None and node.next is not None

        prev_node = node.prev
        next_node = node.next

        prev_node.next = next_node
        next_node.prev = prev_node

    def _move_to_tail(self, node: Node[K, V]) -> None:
        self._remove_node(node)
        self._add_node(node)

    def _pop_lru(self) -> Node[K, V]:
        assert self.head.next is not None

        lru = self.head.next
        self._remove_node(lru)
        return lru

    # ---------------- CACHE OPERATIONS ---------------- #

    def get(self, key: K) -> V | int:
        with self.lock:
            if key not in self.cache:
                return -1

            node = self.cache[key]
            self._move_to_tail(node)

            return node.value  # type: ignore

    def put(self, key: K, value: V) -> None:
        with self.lock:
            if key in self.cache:
                node = self.cache[key]
                node.value = value
                self._move_to_tail(node)

            else:
                new_node = Node(key, value)
                self.cache[key] = new_node
                self._add_node(new_node)

                if len(self.cache) > self.capacity:
                    lru = self._pop_lru()
                    if lru.key is not None:
                        del self.cache[lru.key]