package org.projectfloodlight.openflow.util;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.PrimitiveSinkable;

import com.google.common.collect.ImmutableSortedSet;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;

public class PrimitiveSinkUtilsTest {

    private HashFunction hash;

    @Before
    public void setup() {
        hash = Hashing.murmur3_128();
    }

    @Test
    public void testPutNullableString() {
        // test that these different invocations of putNullable
        // differ pairwise
        HashCode[] hs = new HashCode[] {
                calcPutNullableString((String) null),
                calcPutNullableString(""),
                calcPutNullableString(null, null),
                calcPutNullableString(null, ""),
                calcPutNullableString("", null),
                calcPutNullableString("a\0a", null),
                calcPutNullableString(null, "a\0a"),
        };

        checkPairwiseDifferent(hs);
    }

    @Test
    public void testPutNullable() {
        // test that these different invocations of putNullable
        // differ pairwise
        HashCode[] hs = new HashCode[] {
                calcPutNullables(),
                calcPutNullables(OFPort.of(1)),
                calcPutNullables(OFPort.of(1), null),
                calcPutNullables(OFPort.of(1), null, null),
                calcPutNullables(null, OFPort.of(1), null),
                calcPutNullables(null, null, OFPort.of(1))
        };

        checkPairwiseDifferent(hs);
    }

    private void checkPairwiseDifferent(HashCode[] hs) {
        for(int i=0;i<hs.length;i++) {
            for(int j=i+1; j<hs.length;j++) {
                assertThat(hs[i], not(hs[j]));
            }
        }
    }

    @Test
    public void testPutList() {
        HashCode[] hs = new HashCode[] {
                calcPutList(),
                calcPutList(OFPort.of(1)),
                calcPutList(OFPort.of(2)),
                calcPutList(OFPort.of(1), OFPort.of(2)),
                calcPutList(OFPort.of(2), OFPort.of(1)),
                calcPutList(OFPort.of(1), OFPort.of(3)),
                calcPutList(OFPort.of(1), OFPort.of(2), OFPort.of(3)),
        };

        checkPairwiseDifferent(hs);
    }

    @Test
    public void testPutSortedSet() {
        HashCode[] hs = new HashCode[] {
                calcPutSortedSet(),
                calcPutSortedSet(OFPort.of(1)),
                calcPutSortedSet(OFPort.of(2)),
                calcPutSortedSet(OFPort.of(1), OFPort.of(2)),
                calcPutSortedSet(OFPort.of(1), OFPort.of(3)),
                calcPutSortedSet(OFPort.of(1), OFPort.of(2), OFPort.of(3)),
        };

        checkPairwiseDifferent(hs);

        assertThat(calcPutSortedSet(OFPort.of(1), OFPort.of(2)),
                equalTo(calcPutSortedSet(OFPort.of(2), OFPort.of(1))));
    }

    private HashCode calcPutNullableString(String... strings) {
        Hasher h = hash.newHasher();
        for(String s: strings) {
            PrimitiveSinkUtils.putNullableStringTo(h, s);
        }
        return h.hash();
    }

    private HashCode calcPutSortedSet(OFPort... ports) {
        Hasher h = hash.newHasher();
        PrimitiveSinkUtils.putSortedSetTo(h, ImmutableSortedSet.copyOf(ports));
        return h.hash();
    }

    private HashCode calcPutList(OFPort... ports) {
        Hasher h = hash.newHasher();
        PrimitiveSinkUtils.putListTo(h, Arrays.asList(ports));
        return h.hash();
    }


    private HashCode calcPutNullables(PrimitiveSinkable... ps) {
        Hasher h = hash.newHasher();
        for(PrimitiveSinkable p : ps) {
            PrimitiveSinkUtils.putNullableTo(h, p);
        }
        return h.hash();
    }
}
