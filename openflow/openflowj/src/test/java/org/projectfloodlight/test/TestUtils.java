package org.projectfloodlight.test;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.primitives.Bytes;

public class TestUtils {
     private TestUtils() {}

     private static final int PER_LINE = 8;

     public static void betterAssertArrayEquals(byte[] expected, byte[] got) {
         int maxlen = Math.max(expected.length, got.length);

         List<String> expectedList = formatHex(Bytes.asList(expected));
         List<String> gotList = formatHex(Bytes.asList(got));

         boolean fail = false;
         for (int i = 0; i < maxlen;i+= PER_LINE) {
             int maxThisLine = Math.min(maxlen, PER_LINE);
             boolean print = false;

             ArrayList<String> changeMarkers = new ArrayList<String>();

             for (int j = i; j < maxThisLine; j++) {
                 if (j >= expected.length || j >= got.length  || expected[j] != got[j]) {
                     print = true;
                     fail = true;
                     changeMarkers.add("==");
                     break;
                 } else {
                     changeMarkers.add("  ");
                 }
             }
             if(print) {
                System.out.println(String.format("%4x: %s", i, Joiner.on(" ").join(expectedList.subList(i, Math.min(expectedList.size(), i+PER_LINE)))));
                System.out.println(String.format("%4x: %s", i, Joiner.on(" ").join(gotList.subList(i, Math.min(gotList.size(), i+PER_LINE)))));
                System.out.println(String.format("%4s  %s", "", Joiner.on(" ").join(changeMarkers)));
                System.out.println("\n");
             }
         }
         if(fail) {
             Assert.fail("Array comparison failed");
         }

     }

     private static List<String> formatHex(List<Byte> b) {
         return Lists.transform(b, new Function<Byte, String>() {
             @Override
             public String apply(Byte input) {
                 return String.format("%02x", input);
             }
         });
     }
}