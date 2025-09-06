package deal.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

final class DefaultPrizeLadderTest {

    @Test
    void custom8_usesLowestEight_fromLegacy25() {
        var ladder = new DefaultPrizeLadder();
        List<Integer> amounts = ladder.amountsFor(8);
        assertEquals(8, amounts.size());
        assertEquals(200, (int) amounts.get(7)); // highest for 8 cases
    }

    @Test
    void custom11_usesLowestEleven_fromLegacy25() {
        var ladder = new DefaultPrizeLadder();
        List<Integer> amounts = ladder.amountsFor(11);
        assertEquals(11, amounts.size());
        assertEquals(500, (int) amounts.get(10)); // highest for 11 cases
    }

    @Test
    void legacy25_unchanged() {
        var ladder = new DefaultPrizeLadder();
        List<Integer> amounts = ladder.amountsFor(25);
        assertEquals(25, amounts.size());
        assertEquals(1_000_000, (int) amounts.get(24));
    }
}
