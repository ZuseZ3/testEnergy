package org.jsonbuddy;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class JsonArrayTest {
    @Test
    public void shouldMapValues() throws Exception {
        JsonArray jsonArray = JsonArray.fromNodeList(Arrays.asList(
                JsonFactory.jsonObject().put("name", "Darth"),
                JsonFactory.jsonObject().put("name", "Luke"),
                JsonFactory.jsonObject().put("name", "Leia")
        ));
        List<String> names = jsonArray.mapValues(jo -> jo.requiredString("name"));
        assertThat(names).containsExactly("Darth","Luke","Leia");

    }
}