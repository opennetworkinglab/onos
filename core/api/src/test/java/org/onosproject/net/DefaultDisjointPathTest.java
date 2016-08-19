package org.onosproject.net;

import java.util.List;

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.testing.EqualsTester;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.onosproject.net.NetTestTools.PID;

/**
 * Unit tests for the DefaultDisjointPathTest.
 */
public class DefaultDisjointPathTest {
    private static DefaultLink link1 =
            DefaultLink.builder()
                    .type(Link.Type.DIRECT)
                    .providerId(PID)
                    .src(NetTestTools.connectPoint("dev1", 1))
                    .dst(NetTestTools.connectPoint("dev2", 1)).build();

    private static DefaultLink link2 =
            DefaultLink.builder()
                    .type(Link.Type.DIRECT)
                    .providerId(PID)
                    .src(NetTestTools.connectPoint("dev1", 1))
                    .dst(NetTestTools.connectPoint("dev2", 1)).build();

    private static DefaultLink link3 =
            DefaultLink.builder()
                    .type(Link.Type.DIRECT)
                    .providerId(PID)
                    .src(NetTestTools.connectPoint("dev2", 1))
                    .dst(NetTestTools.connectPoint("dev3", 1)).build();

    private static List<Link> links1 = ImmutableList.of(link1, link2);
    private static DefaultPath path1 =
            new DefaultPath(PID, links1, 1.0);

    private static List<Link> links2 = ImmutableList.of(link2, link1);
    private static DefaultPath path2 =
            new DefaultPath(PID, links2, 2.0);

    private static List<Link> links3 = ImmutableList.of(link1, link2, link3);
    private static DefaultPath path3 =
            new DefaultPath(PID, links3, 3.0);

    private static DefaultDisjointPath disjointPath1 =
            new DefaultDisjointPath(PID, path1, path2);
    private static DefaultDisjointPath sameAsDisjointPath1 =
            new DefaultDisjointPath(PID, path1, path2);
    private static DefaultDisjointPath disjointPath2 =
            new DefaultDisjointPath(PID, path2, path1);
    private static DefaultDisjointPath disjointPath3 =
            new DefaultDisjointPath(PID, path1, path3);
    private static DefaultDisjointPath disjointPath4 =
            new DefaultDisjointPath(PID, path1, null);


    /**
     * Tests construction and fetching of member data.
     */
    @Test
    public void testConstruction() {
        assertThat(disjointPath1.primary(), is(path1));
        assertThat(disjointPath1.backup(), is(path2));
        assertThat(disjointPath1.links(), is(links1));
        assertThat(disjointPath1.cost(), is(1.0));
    }

    /**
     * Tests switching to the backup path.
     */
    @Test
    public void testUseBackup() {
        disjointPath1.useBackup();
        assertThat(disjointPath1.primary(), is(path1));
        assertThat(disjointPath1.backup(), is(path2));
        assertThat(disjointPath1.links(), is(links2));
        assertThat(disjointPath1.cost(), is(2.0));

        disjointPath1.useBackup();
        assertThat(disjointPath1.links(), is(links1));
        assertThat(disjointPath1.cost(), is(1.0));

        assertThat(disjointPath4.primary(), is(path1));
        assertThat(disjointPath4.backup(), is((DefaultDisjointPath) null));
        disjointPath4.useBackup();
        assertThat(disjointPath4.primary(), is(path1));
        assertThat(disjointPath4.backup(), is((DefaultDisjointPath) null));
    }

    /**
     * Tests equals(), hashCode(), and toString() methods.
     */
    @Test
    public void testEquals() {
        new EqualsTester()
                .addEqualityGroup(disjointPath1, sameAsDisjointPath1, disjointPath2)
                .addEqualityGroup(disjointPath3)
                .addEqualityGroup(disjointPath4)
                .testEquals();
    }
}
