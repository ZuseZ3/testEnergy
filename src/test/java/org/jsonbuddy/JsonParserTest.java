package org.jsonbuddy;


import org.junit.Test;

import java.io.StringReader;

import static org.assertj.core.api.Assertions.assertThat;

public class JsonParserTest {

    @Test
    public void shouldParseEmptyObject() throws Exception {
        JsonNode jsonNode = JsonParser.parse(new StringReader("{}"));
        assertThat(jsonNode instanceof JsonObject).isTrue();
    }

    @Test
    public void shouldParseObjectWithStringValue() throws Exception {
        StringReader input = new StringReader(fixQuotes("{'name':'Darth Vader'}"));
        JsonObject jsonObject = (JsonObject) JsonParser.parse(input);
        assertThat(jsonObject.stringValue("name")).isPresent().contains("Darth Vader");
    }

    @Test
    public void shouldHandleMultipleValuesInObject() throws Exception {
        StringReader input = new StringReader(fixQuotes("{'firstname':'Darth', 'lastname': 'Vader'}"));
        JsonObject jsonObject = (JsonObject) JsonParser.parse(input);
        assertThat(jsonObject.stringValue("firstname")).isPresent().contains("Darth");
        assertThat(jsonObject.stringValue("lastname")).isPresent().contains("Vader");

    }

    private static String fixQuotes(String content) {
        return content.replace("'", "\"");
    }
}