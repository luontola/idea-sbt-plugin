// Copyright © 2010, Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.sbt.runner;

public class CyclicCharBuffer {

    private final char[] buffer;
    private int start = 0;
    private int length = 0;

    public CyclicCharBuffer(int capacity) {
        buffer = new char[capacity];
    }

    public int length() {
        return length;
    }

    public char charAt(int i) {
        return buffer[index(i)];
    }

    private int index(int i) {
        return (start + i) % buffer.length;
    }

    public void append(char c) {
        if (isFull()) {
            removeFirst();
        }
        insertLast(c);
    }

    private boolean isFull() {
        return length == buffer.length;
    }

    private void removeFirst() {
        start++;
        length--;
    }

    private void insertLast(char c) {
        buffer[index(length)] = c;
        length++;
    }

    public boolean contentEquals(String that) {
        if (this.length() != that.length()) {
            return false;
        }
        for (int i = 0; i < length(); i++) {
            if (this.charAt(i) != that.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    public String toString() {
        StringBuilder s = new StringBuilder(buffer.length);
        for (int i = 0; i < length(); i++) {
            s.append(charAt(i));
        }
        return s.toString();
    }
}
