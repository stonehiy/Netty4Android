package com.james.netty4android;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() {
        assertEquals(4, 2 + 2);
    }

    @Test
    public void test1() {
        int num = 0;
        byte b1 = (byte) ((num >>> 24) & 0xFF);
        System.out.println(b1);
        byte b2 = (byte) ((num >>> 16) & 0xFF);
        System.out.println(b2);
        byte b3 = (byte) ((num >>> 8) & 0xFF);
        System.out.println(b3);
        byte b4 = (byte) ((num >>> 0) & 0xFF);

        System.out.println(b4);
        String s = Integer.toBinaryString(num);
        System.out.println(s);

        int v = b4 & 0xFF;
        String hv = Integer.toHexString(v);
        String s1 = Integer.toBinaryString(v);
//        String s2 = Integer.toString(v, 2);
        System.out.println(hv);
        System.out.println(s1);
//        System.out.println(s2);

    }
}