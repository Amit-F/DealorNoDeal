package deal.cli;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ArgsParsingTest {
    @Test
    void parsesKeyValueAndFlags() {
        var a = Args.parse(new String[] {"--cases=25", "--seed=123", "--verbose"});
        assertThat(a.get("cases")).isEqualTo("25");
        assertThat(a.get("seed")).isEqualTo("123");
        assertThat(a.get("verbose")).isEqualTo("true");
    }

    @Test
    void cliOptionsValidate() {
        var a = Args.parse(new String[] {"--cases=10", "--seed=999"});
        var opt = CliOptions.from(a);
        assertThat(opt.caseCount).isEqualTo(10);
        assertThat(opt.seed).isEqualTo(999L);
        assertThat(opt.help).isFalse();
    }

    @Test
    void helpFlag() {
        var a = Args.parse(new String[] {"--help"});
        var opt = CliOptions.from(a);
        assertThat(opt.help).isTrue();
    }

    @Test
    void badCasesFails() {
        var a = Args.parse(new String[] {"--cases=foo"});
        assertThatThrownBy(() -> CliOptions.from(a))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid integer for --cases");
    }
}
