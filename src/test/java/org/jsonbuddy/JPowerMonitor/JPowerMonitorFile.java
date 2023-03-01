package org.jsonbuddy.JPowerMonitor;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class JPowerMonitorFile {

    public static void setRepeatValue(String repeatValue) throws IOException {
        int lineNumber = 40;
        Path path = Paths.get("config.properties");
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        lines.set(lineNumber, "repeat-value=" + repeatValue);
        Files.write(path, lines, StandardCharsets.UTF_8);
    }

    public static void setTestMethod(String method) throws IOException {
        int lineNumber = 16;
        Path path = Paths.get("config.properties");
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        lines.set(lineNumber, "filter-method-names=" + method);
        Files.write(path, lines, StandardCharsets.UTF_8);
    }

}
