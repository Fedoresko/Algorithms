package fedoresko.home.strutures;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Heap-based priority queue with ability to mutate elements inside.
 * More efficient than re-inserting elements to {@link PriorityQueue} because no need to iterate through all elements
 * @param <E> elements type
 */
public class MutablePriorityQueue<E> extends AbstractQueue<E> {

    private final Comparator<? super E> comparator;
    private int size;
    private Node root;
    private long modc;
    private final Function<E, Consumer<Consumer<E>>> mutationSourceFactory;


    /**
     * Non-mutable, useless. ))
     */
    public MutablePriorityQueue() {
        this(v -> null);
    }

    /**
     * For Comparable elements
      * @param mutationSourceFactory - for given value take listener and subscribe it for further value changes
     */
    public MutablePriorityQueue(Function<E, Consumer<Consumer<E>>> mutationSourceFactory) {
        this(mutationSourceFactory, (o1, o2) -> ((Comparable<E>)o1).compareTo(o2));
    }

    /**
     * For any elements
     * @param mutationSourceFactory - for given value take listener and subscribe it for further value changes
     * @param comparator - compare elements
     */
    public MutablePriorityQueue(Function<E, Consumer<Consumer<E>>> mutationSourceFactory, Comparator<? super E> comparator) {
        this.mutationSourceFactory = mutationSourceFactory;
        this.comparator = comparator;
    }

    @Override
    public Iterator<E> iterator() {
        return new HeapIterator();
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean offer(E e) {
        if (root == null) {
            root = new Node(e);
            root.value = e;
            size++;
        } else {
            addNodeForValue(e);
        }
        return true;
    }

    @Override
    public E poll() {
        E res = peek();
        if (root != null) {
            remove(root);
        }
        return res;
    }

    @Override
    public E peek() {
        return (root == null) ? null : root.value;
    }

    /**
     * Heap height
     * @return the heap height - should be O(log N) where N = @this.size()
     */
    public int depth() {
        if (root == null) return 0;
        return root.deep;
    }

    private Node siftUp(Node node) {
        modc++;
        while (node.root != null) {
            if (comparator.compare(node.root.value, node.value) > 0) {
                swapValues(node, node.root);
                node = node.root;
            } else {
                break;
            }
        }
        return node;
    }

    private void swapValues(Node a, Node b) {
        E t = a.value; a.value = b.value; b.value = t;
        if (a.valueSubscriber != null && b.valueSubscriber != null) {
            ValueSubscriber o = a.valueSubscriber; a.valueSubscriber = b.valueSubscriber; b.valueSubscriber = o;
            a.valueSubscriber.attach(a);
            b.valueSubscriber.attach(b);
        }
    }

    private void remove(Node node) {
        modc++;
        if (node.left != null && node.right != null) {
            if (comparator.compare(node.left.value, node.right.value) > 0) {
                swapValues(node, node.right);
                remove(node.right);
            } else {
                swapValues(node, node.left);
                remove(node.left);
            }
        } else if (node.left != null) {
            swapValues(node, node.left);
            remove(node.left);
        } else if (node.right != null) {
            swapValues(node, node.right);
            remove(node.right);
        } else {
            if (node.root != null) {
                if (node.root.left == node) {
                    node.root.left = null;
                } else {
                    node.root.right = null;
                }
            } else {
                root = null;
            }
            size--;
            return;
        }
        if (node.left == null && node.right == null) {
            node.deep = 0;
        } else {
            node.deep = Math.max(node.left == null ? 0 : node.left.deep, node.right == null ? 0 : node.right.deep) + 1;
        }
    }

    private void addNodeForValue(E value) {
        modc++;
        Node current = root;

        while (current.left != null && current.right != null) {
            if (current.left.deep <= current.right.deep)
                current = current.left;
            else
                current = current.right;
        }

        Node newNode = new Node(value);
        if (current.left == null) {
            current.left = newNode;
        } else {
            current.right = newNode;
        }
        newNode.root = current;
        newNode.value = value;

        while (current != null) {
            int deepWas = current.deep;
            current.deep = Math.max(current.left == null ? 0 : current.left.deep, current.right == null ? 0 : current.right.deep) + 1;
            if (deepWas == current.deep)
                break;
            current = current.root;
        }

        siftUp(newNode);
        size++;
    }

    private Node siftDown(Node node) {
        modc++;
        Node current = node;
        E newVal = node.value;
        while (current.left != null || current.right != null) {
            Node newCandidate = current;
            if (current.left != null && current.right != null) {
                newCandidate = (comparator.compare(current.left.value, current.right.value) > 0) ?
                        current.right : current.left;
            } else {
                newCandidate = current.left != null ? current.left : current.right;
            }

            if (comparator.compare(newVal, newCandidate.value) <= 0) {
                break;
            }

            swapValues(current, newCandidate);
            current = newCandidate;
        }
        return current;
    }

    private void touch(Node node) {
        siftDown(siftUp(node));
    }

    private class ValueSubscriber implements Consumer<E> {
        private Node node;

        ValueSubscriber(Node node) {
            attach(node);
        }

        public void attach(Node node) {
            this.node = node;
        }

        @Override
        public void accept(E e) {
            node.value = e;
            MutablePriorityQueue.this.touch(node);
        }
    }

    private class Node {
        E value;
        Node root;
        Node left;
        Node right;
        ValueSubscriber valueSubscriber;
        int deep;

        Node(E value) {
            this.value = value;

            Consumer<Consumer<E>> publisher = mutationSourceFactory.apply(value);

            if (publisher != null) {
                valueSubscriber = new ValueSubscriber(this);
                publisher.accept(valueSubscriber);
            }
        }
    }

    //Traverse all nodes in a NLR order
    private class HeapIterator implements Iterator<E> {
        private Node current;
        private long modc;
        private Node next;

        HeapIterator() {
            modc = MutablePriorityQueue.this.modc;
        }

        @Override
        public boolean hasNext() {
            if (current == null) {
                return  MutablePriorityQueue.this.root != null;
            }
            if (next == null) {
                next = nextNode();
            }
            return next != null;
        }

        @Override
        public E next() {
            if (modc != MutablePriorityQueue.this.modc) {
                throw new ConcurrentModificationException("Priority Queue have changes while iterating");
            }

            if (next != null) {
                current = next;
                next = null;
            } else {
                current = nextNode();
            }

            if (current == null) {
                throw new IllegalStateException("Iterator finished");
            }

            return current.value;
        }

        private Node nextNode() {
            Node curNode = this.current;
            if (curNode == null) {
                curNode = MutablePriorityQueue.this.root;
            } else {
                if (curNode.left != null) {
                    curNode = curNode.left;
                } else if (curNode.right != null) {
                    curNode = curNode.right;
                } else {
                    while (curNode.root != null && (curNode.root.right == curNode || curNode.root.right == null)) {
                        curNode = curNode.root;
                    }
                    if (curNode.root != null) {
                        curNode = curNode.root.right;
                    } else {
                        curNode = null;
                    }
                }
            }

            return curNode;
        }

        @Override
        public void remove() {
            next = current;
            MutablePriorityQueue.this.remove(current);
            modc = MutablePriorityQueue.this.modc;
        }
    }
}
