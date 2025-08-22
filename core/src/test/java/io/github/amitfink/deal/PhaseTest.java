package io.github.amitfink.deal;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PhaseTest {
    @Test
    void hasBasicPhases() {
        assertThat(Phase.valueOf("INIT")).isNotNull();
    }
}
