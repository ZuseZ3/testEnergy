package org.jsonbuddy.parse;

import org.jsonbuddy.*;

import java.io.*;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class JsonParser {
    public static JsonNode parse(Reader reader) throws JsonParseException {
        JsonParser jsonParser = new JsonParser(reader);
        return jsonParser.parseValue();
    }


    public static JsonNode parse(InputStream inputStream) throws JsonParseException  {
        return parse(new InputStreamReader(inputStream));
    }

    public static JsonNode parse(String input) throws JsonParseException  {
        return parse(new StringReader(input));
    }

    public static JsonObject parseToObject(Reader reader) throws JsonParseException {
        JsonParser jsonParser = new JsonParser(reader);
        return toObject(jsonParser.parseValue());
    }

    private static JsonObject toObject(JsonNode result) {
        if (!(result instanceof JsonObject)) {
            throw new JsonParseException("Expected json object got " + Optional.ofNullable(result).map(Object::getClass).map(Object::toString).orElse("null"));
        }
        return (JsonObject) result;
    }


    public static JsonArray parseToArray(InputStream inputStream) throws JsonParseException  {
        return toArray(parse(new InputStreamReader(inputStream)));
    }

    public static JsonArray parseToArray(String input) throws JsonParseException  {
        return toArray(parse(new StringReader(input)));
    }


    public static JsonArray parseToArray(Reader reader) throws JsonParseException {
        JsonParser jsonParser = new JsonParser(reader);
        return toArray(jsonParser.parseValue());
    }

    private static JsonArray toArray(JsonNode result) {
        if (!(result instanceof JsonArray)) {
            throw new JsonParseException("Expected json array got " + Optional.ofNullable(result).map(Object::getClass).map(Object::toString).orElse("null"));
        }
        return (JsonArray) result;
    }


    public static JsonObject parseToObject(InputStream inputStream) throws JsonParseException  {
        return toObject(parse(new InputStreamReader(inputStream)));
    }

    public static JsonObject parseToObject(String input) throws JsonParseException  {
        return toObject(parse(new StringReader(input)));
    }




    private Reader reader;
    private char lastRead;
    private boolean finished;

    private JsonParser(Reader reader) {
        this.reader = reader;
        readNext();
    }

    private void readNext()  {
        int read;
        try {
            read = reader.read();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (read == -1) {
            finished = true;
            return;
        }
        this.lastRead = (char) read;
    }



    private JsonNode parseValue() {
        while (!finished) {
            switch (lastRead) {
                case '{':
                    return parseObject();
                case '[':
                    return parseArray();
                case '"':
                    return parseStringValue();
                case 't':
                case 'f':
                    return parseBooleanValue();
                case 'n':
                    return parseNullValue();
            }
            if (lastRead == '-' || Character.isDigit(lastRead)) {
                return parseNumberValue();
            }
            if (!Character.isSpaceChar(lastRead)) {
                throw new JsonParseException("Unexpected charachter " + lastRead);
            }
            readNext();
        }
        return null;
    }


    private JsonSimpleValue parseNumberValue() {
        StringBuilder val = new StringBuilder();
        boolean isDouble = false;
        while (Character.isDigit(lastRead) || ".eE-".contains("" + lastRead)) {
            isDouble = isDouble || ".eE".contains("" + lastRead);
            val.append(lastRead);
            readNext();
        }
        if (!(Character.isSpaceChar(lastRead) || "}],".contains("" + lastRead))) {
            throw new JsonParseException("Illegal value " + val + lastRead);
        }
        if (isDouble) {
            return new JsonDouble(Double.parseDouble(val.toString()));
        }
        return new JsonLong(Long.parseLong(val.toString()));
    }


    private JsonNullValue parseNullValue() {
        expectValue("null");
        return new JsonNullValue();
    }

    private JsonBooleanValue parseBooleanValue() {
        boolean isTrue = (lastRead == 't');
        String expect = isTrue ? "true" : "false";
        expectValue(expect);
        return new JsonBooleanValue(isTrue);
    }

    private void expectValue(String value) {
        for (int i=0;i<value.length();i++) {
            readNext();
        }
    }

    private JsonArray parseArray() {
        JsonArray jsonArrayFactory = new JsonArray();
        while (!(finished || lastRead == ']')) {
            readNext();
            JsonNode jsonFactory = parseValue();
            jsonArrayFactory.add(jsonFactory);
            readSpaceUntil("Expected , or ] in array", ']', ',');
        }
        if (finished) {
            throw new JsonParseException("Expected , or ] in array");
        }
        return jsonArrayFactory;
    }

    private JsonSimpleValue parseStringValue() {
        readNext();
        String value = readText();
        try {
            return new JsonInstantValue(Instant.parse(value));
        } catch (DateTimeParseException e) {
            return new JsonTextValue(value);
        }
    }

    private JsonObject parseObject() {
        JsonObject jsonObject = new JsonObject();
        while (!(finished || lastRead == '}')) {
            readSpaceUntil("JsonObject not closed. Expected }", '}', '"');
            if (lastRead == '}') {
                return jsonObject;
            }
            readNext();
            String key = readText();
            readSpaceUntil("Expected value for objectkey " + key, ':');
            readNext();
            if (finished) {
                throw new JsonParseException("Expected value for key " + key);
            }
            JsonNode value = parseValue();
            jsonObject.withValue(key, value);
            readSpaceUntil("JsonObject not closed. Expected }", ',', '}');
        }
        if (finished) {
            throw new JsonParseException("JsonObject not closed. Expected }");
        }
        return jsonObject;
    }

    private String readText() {
        StringBuilder res = new StringBuilder();
        while (!(finished || lastRead == '"')) {
            if (lastRead == '\\') {
                readNext();
                if (finished) {
                    // Todo this is error
                    break;
                }
                switch (lastRead) {
                    case '"':
                        res.append("\"");
                        break;
                    case '\\':
                        res.append("\\");
                        break;
                    case '/':
                        res.append("/");
                        break;
                    case 'b':
                        res.append("\b");
                        break;
                    case 'f':
                        res.append("\f");
                        break;
                    case 'n':
                        res.append("\n");
                        break;
                    case 't':
                        res.append("\t");
                        break;
                    case 'u':
                        throw new RuntimeException("\\u Not supported yet");
                }
            } else {
                res.append(lastRead);
            }
            readNext();
        }
        return res.toString();
    }

    private void readSpaceUntil(String errormessage, Character... readUntil) {
        List<Character> until = Arrays.asList(readUntil);
        if (until.contains(lastRead)) {
            return;
        }
        readNext();
        while (!(finished || until.contains(lastRead))) {
            if (!Character.isWhitespace(lastRead)) {
                throw new JsonParseException(errormessage);
            }
            readNext();
        }
        if (finished) {
            throw new JsonParseException(errormessage);
        }
    }

}
