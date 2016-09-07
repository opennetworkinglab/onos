/*
 * Copyright 2016-present Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onosproject.ui.impl.birds;

import com.google.common.io.Files;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Random;

/**
 * Encrypt png images to .foo format.
 */
public class BirdEncoderTest {

    private static final String ORIG = "  original> ";
    private static final String DATA = "  data> ";
    private static final String BEAN = "  bean> ";
    private static final String SAUCE = "  sauce> ";
    private static final String NONE = "(none)";

    private static final String PNG = ".png";
    private static final String FOO = ".foo";

    private static class Encoding {
        private final String name;
        private String bean = NONE;
        private String sauce = NONE;

        Encoding(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return ORIG + name + PNG +
                    DATA + bean + FOO +
                    BEAN + bean +
                    SAUCE + sauce;
        }

        private Encoding encode() {
            String u = name.toUpperCase();
            int x = u.codePointAt(1) - 64;
            int r = (x * 7) % 26;

            String uc = doTransform(r, u);
            bean = uc.toLowerCase();

            StringBuilder sauceSb = new StringBuilder();
            sauceSb.append(r).append(":");
            for (String q : uc.split("")) {
                sauceSb.append(q.codePointAt(0));
            }
            sauce = sauceSb.toString();
            return this;
        }

        private String doTransform(int r, String u) {
            final int m = 65;
            final int x = 90;

            int d = x - m + 1;
            int s = x + m;
            String[] letters = u.split("");
            StringBuilder sb = new StringBuilder();

            for (String j : letters) {
                int k = j.codePointAt(0);
                int e = s - k + r;
                e = e > x ? e - d : e;
                sb.append(Character.toChars(e));
            }

            return sb.toString();
        }

        String dataName() {
            return bean + FOO;
        }

        String pngName() {
            return name + PNG;
        }
    }


    private static final FilenameFilter FOO_FILTER =
            (dir, name) -> name.endsWith(FOO);
    private static final FilenameFilter PNG_FILTER =
            (dir, name) -> name.endsWith(PNG);

    private static final Comparator<File> FILE_COMPARATOR =
            (o1, o2) -> o1.getName().compareTo(o2.getName());

    private static final File DIR =
            new File("src/test/java/org/onosproject/ui/impl/birds");

    private static final File FOO_DIR = new File(DIR, "foo");
    private static final File ORIG_DIR = new File(DIR, "orig");


    private static final byte UPPER = (byte) 0xf0;
    private static final byte LOWER = (byte) 0x0f;


    private static void print(String fmt, Object... params) {
        System.out.println(String.format(fmt, params));
    }

    private List<File> filesIn(File dir, FilenameFilter filter) {
        File[] files = dir.listFiles(filter);
        if (files == null) {
            return Collections.emptyList();
        }
        Arrays.sort(files, FILE_COMPARATOR);
        return Arrays.asList(files);
    }

    private String basename(File f) {
        String name = f.getName();
        int i = name.indexOf(PNG);
        return name.substring(0, i);
    }

    private boolean dataRequired(Encoding encoding, List<String> dataFiles) {
        return !dataFiles.contains(encoding.dataName());
    }

    private List<String> makeFileNames(List<File> dataFiles) {
        List<String> names = new ArrayList<>(dataFiles.size());
        for (File f : dataFiles) {
            names.add(f.getName());
        }
        return names;
    }

    private final Random random = new Random(new Date().getTime());
    private final byte[] rbytes = new byte[2];

    private int r1;
    private int r2;

    private void randomBytes() {
        random.nextBytes(rbytes);
        r1 = rbytes[0];
        r2 = rbytes[1];
    }

    private void addNoise(ByteBuffer bb, byte b) {
        randomBytes();

        int upper = b & UPPER;
        int lower = b & LOWER;

        int uNoise = r1 & LOWER;
        int lNoise = r2 & UPPER;

        int uEnc = upper | uNoise;
        int lEnc = lower | lNoise;

        bb.put((byte) uEnc);
        bb.put((byte) lEnc);
    }

    private ByteBuffer encodePng(File file) throws IOException {
        byte[] bytes = Files.toByteArray(file);
        int size = bytes.length;
        ByteBuffer bb = ByteBuffer.allocate(size * 2);
        for (int i = 0; i < size; i++) {
            addNoise(bb, bytes[i]);
        }
        return bb;
    }

    private void generateDataFile(Encoding encoding) {
        File png = new File(ORIG_DIR, encoding.pngName());
        File foo = new File(FOO_DIR, encoding.dataName());
        ByteBuffer bb;
        try {
            bb = encodePng(png);
            writeBufferToFile(bb, foo);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeBufferToFile(ByteBuffer bb, File file)
            throws IOException {
        bb.flip();
        FileChannel chan = new FileOutputStream(file, false).getChannel();
        chan.write(bb);
        chan.close();
        print("    Wrote file: %s  (%d bytes)", file, bb.capacity());
    }

    @Test @Ignore
    public void encodeThings() {
        print("Encoding things...");
        List<File> originals = filesIn(ORIG_DIR, PNG_FILTER);
        List<File> dataFiles = filesIn(FOO_DIR, FOO_FILTER);
        List<String> fileNames = makeFileNames(dataFiles);

        int count = 0;
        for (File f : originals) {
            Encoding encoding = new Encoding(basename(f)).encode();
            print("%n%s", encoding);
            if (dataRequired(encoding, fileNames)) {
                generateDataFile(encoding);
                count++;
            }
        }

        print("%nFoo files generated: %d", count);
    }

}
