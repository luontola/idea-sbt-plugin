// Copyright © 2010, Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package net.orfjackal.sbt.runner;

import org.junit.Test;

import static org.junit.Assert.*;

public class CyclicCharBufferTest {

    private static final int UNUSED_CAPACITY = 100;

    @Test
    public void initially_the_buffer_is_empty() {
        CyclicCharBuffer buffer = new CyclicCharBuffer(UNUSED_CAPACITY);

        assertHasString("", buffer);
    }

    @Test
    public void the_buffer_contains_what_is_written_there() {
        CyclicCharBuffer buffer = new CyclicCharBuffer(UNUSED_CAPACITY);
        buffer.append('a');
        buffer.append('b');

        assertHasString("ab", buffer);
    }

    @Test
    public void the_first_chars_are_dropped_when_the_buffer_is_filled() {
        CyclicCharBuffer buffer = new CyclicCharBuffer(3);
        buffer.append('a');
        buffer.append('b');
        buffer.append('c');

        assertHasString("abc", buffer);

        buffer.append('d');

        assertHasString("bcd", buffer);
    }

    @Test
    public void can_test_whether_the_buffer_content_equals_a_string() {
        CyclicCharBuffer buffer = new CyclicCharBuffer(3);
        buffer.append('a');
        buffer.append('b');

        assertTrue("should equal <ab>", buffer.contentEquals("ab"));

        for (String s : new String[]{
                "a",    // shorter than length
                "ax",   // different content
                "abc",  // longer than length
                "abcd", // longer than capacity
        }) {
            assertFalse("should not equal <" + s + ">", buffer.contentEquals(s));
        }
    }

    @Test
    public void can_check_if_buffer_ends_with_content() {
        CyclicCharBuffer buffer = new CyclicCharBuffer(3);
        buffer.append('a');
        buffer.append('b');

        assertTrue("should end with <ab>", buffer.contentEndsWith("ab"));
        assertTrue("should end with <b>", buffer.contentEndsWith("b"));
        assertTrue("should end with <>", buffer.contentEndsWith(""));
        assertFalse("should end with <a>", buffer.contentEndsWith("a"));
        assertFalse("should end with <abc>", buffer.contentEndsWith("abc"));
    }

    private static void assertHasString(String expected, CyclicCharBuffer actual) {
        assertEquals("size", expected.length(), actual.length());
        assertEquals("content", expected, actual.toString());
        assertTrue("contentEquals", actual.contentEquals(expected));
    }
}
