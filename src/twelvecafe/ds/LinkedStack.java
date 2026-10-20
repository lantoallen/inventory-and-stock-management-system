package twelvecafe.ds;

import java.util.NoSuchElementException;

/**
 * A generic LIFO (Last-In, First-Out) stack built from linked nodes.
 *
 * Used for the "Undo last transaction" feature: every transaction pushes
 * a record, and undo pops the most recent one.
 */
public class LinkedStack<T> {

    private static class Node<T> {
        T data;
        Node<T> next;

        Node(T data, Node<T> next) {
            this.data = data;
            this.next = next;
        }
    }

    private Node<T> top;
    private int size;

    /** Places an element on top of the stack. O(1) */
    public void push(T data) {
        top = new Node<>(data, top);
        size++;
    }

    /** Removes and returns the top element. O(1) */
    public T pop() {
        if (isEmpty()) {
            throw new NoSuchElementException("Stack is empty");
        }
        T data = top.data;
        top = top.next;
        size--;
        return data;
    }

    /** Returns the top element without removing it. O(1) */
    public T peek() {
        if (isEmpty()) {
            throw new NoSuchElementException("Stack is empty");
        }
        return top.data;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public int size() {
        return size;
    }
}
