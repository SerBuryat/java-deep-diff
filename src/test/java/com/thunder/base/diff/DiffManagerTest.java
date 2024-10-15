package com.thunder.base.diff;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

import static com.thunder.base.diff.DiffManager.ROOT_FIELD_NAME;
import static com.thunder.base.diff.DiffManager.diff;
import static com.thunder.base.diff.ResultNodeState.CHANGED;
import static com.thunder.base.diff.ResultNodeState.UNTOUCHED;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class DiffManagerTest {

    // without Boolean
    private List<Function<String, ?>> primitivesFactory() {
        return List.of(
                Byte::valueOf,
                Short::valueOf,
                Integer::valueOf,
                Long::valueOf,
                Float::valueOf,
                Double::valueOf,
                value -> value.toCharArray()[0],
                value -> value
        );
    }

    @Test
    public void When_primitivesHasDifferentValues_Expect_Changed() {
        primitivesFactory().forEach(primitiveFactory -> {
            var value1 = primitiveFactory.apply("1");
            var value2 = primitiveFactory.apply("3");

            var result1 = new ResultNode(ROOT_FIELD_NAME, value1, value2, CHANGED, Collections.emptyList());
            assertEquals(result1, diff(value1, value2));
        });
    }

    @Test
    public void When_primitivesHasSameValues_Expect_Untouched() {
        primitivesFactory().forEach(primitiveFactory -> {
            var value1 = primitiveFactory.apply("8");
            var value2 = primitiveFactory.apply("8");

            var result2 = new ResultNode(ROOT_FIELD_NAME, value1, value2, UNTOUCHED, Collections.emptyList());
            assertEquals(result2, diff(value1, value2));
        });
    }

}
