package moten.david.squabble;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.google.common.collect.ImmutableList;

public class ControllerTest {

    @Test
    public void test() {
        assertEquals(list("a"), list("a"));
        assertEquals(list("hello", "there"), Controller.createWordFrom(list(
                "hello", "there"), "therheello"));
        assertEquals(list("a", "b"), Controller.createWordFrom(list("a", "b"),
                "ab"));
        assertEquals(list("a", "b"), Controller.createWordFrom(list("a", "b"),
                "ba"));
        assertEquals(null, Controller.createWordFrom(list("a", "b"), "abc"));
        assertEquals(null, Controller.createWordFrom(list("a", "b"), "a"));
        assertEquals(null, Controller.createWordFrom(list("a", "b"), "b"));
        assertEquals(null, Controller.createWordFrom(list("a", "bc"), "b"));
        assertEquals(null, Controller.createWordFrom(list("a", "bb"), "b"));
        assertNull(Controller.createWordFrom(
                list("ab", "ra", "ca", "da", "bra"), "abracadabraz"));
        Iterable<Word> result = Controller.createWordFrom(list("ab", "ra",
                "ca", "da", "bra"), "abracadabr");
        assertNull(result);

        Word word = new Word(null, "par", null);
        result = Controller.createWordFrom(ImmutableList.of(word), "par");
        System.out.println(result);
        assertEquals(null, result);

    }

    private Iterable<Word> list(String... values) {
        List<Word> list = new ArrayList<Word>();
        for (String value : values)
            list.add(new Word(new User("anyone"), value, null));
        return list;
    }
}
