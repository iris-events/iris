package org.iris_events.asyncapi.runtime.util;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

class VersionUtilTest {

    @Test
    void bumpVersion() {
        String version1 = VersionUtil.bumpVersion("1.0.0", 10);
        String version2 = VersionUtil.bumpVersion("1.0.10", 10);
        String version3 = VersionUtil.bumpVersion("1.10.10", 10);
        String version4 = VersionUtil.bumpVersion("1.0.10", 11);

        assertThat(version1, is("1.0.1"));
        assertThat(version2, is("1.1.0"));
        assertThat(version3, is("2.0.0"));
        assertThat(version4, is("1.0.11"));
    }
}
