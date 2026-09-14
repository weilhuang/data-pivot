package com.data.pivot.plugin.mapping;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class NameMatcherTest {
    @Test
    public void preprocessDropsUnderscoresAndCase() {
        assertEquals("sysuser", NameMatcher.preprocess("SysUser"));
        assertEquals("sysuser", NameMatcher.preprocess("sys_user"));
        assertEquals("userid", NameMatcher.preprocess("USER_ID"));
        assertEquals("", NameMatcher.preprocess(null));
    }

    @Test
    public void exactMatchAfterPreprocessIsPreferred() {
        NameMatcher.NameMatch<String> match = NameMatcher.pickUnique(List.of(
                new NameMatcher.Scored<>("sys_user", NameMatcher.score("sys_user", "SysUser")),
                new NameMatcher.Scored<>("sys_role", NameMatcher.score("sys_role", "SysUser"))
        ));

        assertEquals(MappingConfidence.EXACT, match.confidence());
        assertEquals("sys_user", match.value());
    }

    @Test
    public void uniqueFuzzyMatchIsFallback() {
        NameMatcher.NameMatch<String> match = NameMatcher.pickUnique(List.of(
                new NameMatcher.Scored<>("user_account", 0.93),
                new NameMatcher.Scored<>("order_item", 0.2)
        ));

        assertEquals(MappingConfidence.FUZZY, match.confidence());
        assertEquals("user_account", match.value());
    }

    @Test
    public void multipleFuzzyHitsAreAmbiguousAndDoNotPickMax() {
        NameMatcher.NameMatch<String> match = NameMatcher.pickUnique(List.of(
                new NameMatcher.Scored<>("user_info", 0.95),
                new NameMatcher.Scored<>("user_item", 0.92)
        ));

        assertTrue(match.isAmbiguous());
        assertNull(match.value());
        assertEquals(2, match.candidates().size());
    }

    @Test
    public void multipleExactHitsAreAmbiguous() {
        NameMatcher.NameMatch<String> match = NameMatcher.pickUnique(List.of(
                new NameMatcher.Scored<>("ds1.user", 1.0),
                new NameMatcher.Scored<>("ds2.user", 1.0)
        ));

        assertEquals(MappingConfidence.AMBIGUOUS, match.confidence());
        assertNull(match.value());
    }

    @Test
    public void noCandidateIsUnresolved() {
        NameMatcher.NameMatch<String> match = NameMatcher.pickUnique(List.of(
                new NameMatcher.Scored<>("orders", 0.1)
        ));
        assertEquals(MappingConfidence.UNRESOLVED, match.confidence());
        assertNull(match.value());
    }
}
