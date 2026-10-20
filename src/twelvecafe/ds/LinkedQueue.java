package twelvecafe.ds;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * A generic FIFO (First-In, First-Out) queue built from linked nodes.
 *
 * Used to hold the stock batches of every inventory item: the oldest
 * delivery sits at the front and is used first, which reduces spoilage.
 */
public class LinkedQueue<T> implements Iterable<T> {

    private static class Node<T> {
        T data;
        Node<T> next;

        Node(T data) {
            this.data = data;
        }
    }

    private Node<T> front; // removed from here
    private Node<T> rear;  // added here
    private int size;

    /** Adds an element at the rear of the queue. O(1) */
    public void enqueue(T data) {
        Node<T> node = new Node<>(data);
        if (isEmpty()) {
            front = node;
        } else {
            rear.next = node;
        }
        rear = node;
        size++;
    }

    /** Removes and returns the element at the front of the queue. O(1) */
    public T dequeue() {
        if (isEmpty()) {
            throw new NoSuchElementException("Queue is empty");
        }
        T data = front.data;
        front = front.next;
        if (front == null) {
            rear = null;
        }
        size--;
        return data;
    }

    /** Returns the element at the front without removing it. O(1) */
    public T peek() {
        if (isEmpty()) {
            throw new NoSuchElementException("Queue is empty");
        }
        return front.data;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public int size() {
        return size;
    }

    /** Iterates from front to rear without changing the queue. */
    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            private Node<T> current = front;

            @Override
            public boolean hasNext() {
                return current != null;
            }

            @Override
            public T next() {
                if (current == null) {
                    throw new NoSuchElementException();
                }
                T data = current.data;
                current = current.next;
                return data;
            }
        };
    }
}
