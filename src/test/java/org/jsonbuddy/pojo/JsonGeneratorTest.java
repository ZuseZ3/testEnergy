package org.jsonbuddy.pojo;

import org.jsonbuddy.*;
import org.jsonbuddy.pojo.testclasses.*;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.jupiter.api.RepeatedTest;
import org.jsonbuddy.JPowerMonitor.JPowerMonitorFile;
import org.jsonbuddy.JPowerMonitor.JPowerMonitor;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
@ExtendWith(JPowerMonitor.class)
public class JsonGeneratorTest {

    static final int repetitions = 1000;

    @BeforeClass
    public static void setUp() throws IOException {
        JPowerMonitorFile.setRepeatValue(Integer.toString(repetitions));
        JPowerMonitorFile.setTestMethod("org.jsonbuddy.pojo.JsonGenerator.generateNode");
    }

    @Test
    @RepeatedTest(repetitions)
    public void shouldHandleSimpleClass() throws Exception {
        SimpleWithName simpleWithName = new SimpleWithName("Darth Vader");
        JsonNode generated = JsonGenerator.generateUsingImplementationAsTemplate(simpleWithName);
        assertThat(generated).isInstanceOf(JsonObject.class);
        JsonObject jsonObject = (JsonObject) generated;
        assertThat(jsonObject.stringValue("name").get()).isEqualTo("Darth Vader");
    }

    @Test
    @RepeatedTest(repetitions)
    public void shouldHandleSimpleValues() throws Exception {
        assertThat(JsonGenerator.generateUsingImplementationAsTemplate(null)).isEqualTo(new JsonNull());
        assertThat(JsonGenerator.generateUsingImplementationAsTemplate("Darth")).isEqualTo(JsonFactory.jsonString("Darth"));
        assertThat(JsonGenerator.generateUsingImplementationAsTemplate(42)).isEqualTo(JsonFactory.jsonNumber(42));

    }

    @Test
    @RepeatedTest(repetitions)
    public void shoulHandleFloats() throws Exception {
        JsonNode jsonNode = JsonGenerator.generateUsingImplementationAsTemplate(3.14f);
        JsonNumber jsonDouble = (JsonNumber) jsonNode;
        assertThat(new Double(jsonDouble.doubleValue()).floatValue()).isEqualTo(3.14f);
    }

    @Test
    @RepeatedTest(repetitions)
    public void shouldHandleList() throws Exception {
        List<String> stringlist = Arrays.asList("one", "two", "three");

        JsonNode generate = JsonGenerator.generateUsingImplementationAsTemplate(stringlist);
        assertThat(generate).isInstanceOf(JsonArray.class);
        JsonArray array = (JsonArray) generate;
        assertThat(array.strings()).isEqualTo(stringlist);
    }

    @Test
    @RepeatedTest(repetitions)
    public void shouldHandleArray() throws Exception {
        String[] stringarray = { "one", "two", "three" };

        JsonNode generate = JsonGenerator.generateUsingImplementationAsTemplate(stringarray);
        assertThat(generate).isInstanceOf(JsonArray.class);
        JsonArray array = (JsonArray) generate;
        assertThat(array.strings()).containsExactly(stringarray);
    }

    @Test
    @RepeatedTest(repetitions)
    public void shouldHandleListWithClasses() throws Exception {
        List<SimpleWithName> simpleWithNames = Arrays.asList(new SimpleWithName("Darth"), new SimpleWithName("Anakin"));
        JsonArray array = (JsonArray) JsonGenerator.generateUsingImplementationAsTemplate(simpleWithNames);

        List<JsonObject> objects = array.objects(o -> o);

        assertThat(objects.get(0).stringValue("name").get()).isEqualTo("Darth");
        assertThat(objects.get(1).stringValue("name").get()).isEqualTo("Anakin");
    }

    @Test
    @RepeatedTest(repetitions)
    public void shouldHandleClassWithGetter() throws Exception {
        CombinedClassWithSetter combinedClassWithSetter = new CombinedClassWithSetter();
        combinedClassWithSetter.setPerson(new SimpleWithName("Darth Vader"));
        combinedClassWithSetter.setOccupation("Dark Lord");

        JsonObject jsonObject = (JsonObject) JsonGenerator.generateUsingImplementationAsTemplate(combinedClassWithSetter);

        assertThat(jsonObject.stringValue("occupation").get()).isEqualTo("Dark Lord");
        Optional<JsonObject> person = jsonObject.objectValue("person");

        assertThat(person).isPresent();
        assertThat(person.get()).isInstanceOf(JsonObject.class);
        assertThat(person.get().requiredString("name")).isEqualTo("Darth Vader");

    }


    @Test
    @RepeatedTest(repetitions)
    public void shouldHandleOverriddenValues() throws Exception {
        JsonGeneratorOverrides overrides = new JsonGeneratorOverrides();
        JsonObject generate = (JsonObject) JsonGenerator.generateUsingImplementationAsTemplate(overrides);

        assertThat(generate.requiredLong("myOverriddenValue")).isEqualTo(42);

    }

    @Test
    @RepeatedTest(repetitions)
    public void shoulHandleEmbeddedJson() throws Exception {
        ClassWithJsonElements classWithJsonElements = new ClassWithJsonElements("Darth Vader",
                JsonFactory.jsonObject().put("title", "Dark Lord"),
                JsonFactory.jsonArray().add("Luke").add("Leia"));
        JsonObject generate = (JsonObject) JsonGenerator.generateUsingImplementationAsTemplate(classWithJsonElements);

        assertThat(generate.requiredString("name")).isEqualTo("Darth Vader");
        assertThat(generate.requiredObject("myObject").requiredString("title")).isEqualTo("Dark Lord");
        assertThat(generate.requiredArray("myArray").stringStream().collect(Collectors.toList())).containsExactly("Luke", "Leia");
    }

    @Test
    @RepeatedTest(repetitions)
    public void shouldMakeMapsIntoObjects() throws Exception {
        Map<String, String> map = new HashMap<>();
        map.put("name", "Darth Vader");
        ClassWithMap classWithMap = new ClassWithMap(map);
        JsonObject generate = (JsonObject) JsonGenerator.generateUsingImplementationAsTemplate(classWithMap);
        assertThat(generate.requiredObject("properties").requiredString("name")).isEqualTo("Darth Vader");
    }

    @Test
    public void shouldHandleDifferentSimpleTypes() throws Exception {
        ClassWithDifferentTypes classWithDifferentTypes = new ClassWithDifferentTypes("my text", 42, true, false);
        JsonObject generated = (JsonObject) JsonGenerator.generateUsingImplementationAsTemplate(classWithDifferentTypes);
        assertThat(generated.requiredString("text")).isEqualTo("my text");
        assertThat(generated.requiredLong("number")).isEqualTo(42);
        assertThat(generated.requiredBoolean("bool")).isTrue();
    }

    @Test
    @RepeatedTest(repetitions)
    public void shouldHandleClassWithTime() throws Exception {
        OffsetDateTime dateTime = OffsetDateTime.of(2015, 8, 13, 21, 14, 18, 321, ZoneOffset.UTC);
        ClassWithTime classWithTime = new ClassWithTime();
        classWithTime.setTime(dateTime.toInstant());

        JsonObject jsonNode = (JsonObject) JsonGenerator.generateUsingImplementationAsTemplate(classWithTime);
        assertThat(jsonNode.requiredString("time")).isEqualTo("2015-08-13T21:14:18.000000321Z");
    }

    @Test
    @RepeatedTest(repetitions)
    public void shouldHandleClassWithEnum() throws Exception {
        ClassWithEnum classWithEnum = new ClassWithEnum();
        JsonObject jso = (JsonObject) JsonGenerator.generateUsingImplementationAsTemplate(classWithEnum);
        assertThat(jso.requiredString("enumNumber")).isEqualTo("ONE");
    }

    @Test
    @RepeatedTest(repetitions)
    public void shouldHandleNumbers() throws Exception {
        assertThat(JsonGenerator.generateUsingImplementationAsTemplate(12L)).isEqualTo(new JsonNumber(12L));
        assertThat(JsonGenerator.generateUsingImplementationAsTemplate(12)).isEqualTo(new JsonNumber(12));
    }

    @Test
    @RepeatedTest(repetitions)
    public void shouldHandleMapWithKeyOtherThanString() throws Exception {
        Map<Long,String> myLongMap = new HashMap<>();
        myLongMap.put(42L, "Meaning of life");
        JsonNode generated = JsonGenerator.generateUsingImplementationAsTemplate(myLongMap);
        assertThat(generated).isInstanceOf(JsonObject.class);
        JsonObject jsonObject = (JsonObject) generated;
        assertThat(jsonObject.requiredString("42")).isEqualTo("Meaning of life");
    }

    @Test
    @RepeatedTest(repetitions)
    public void shouldHandleBigInteger() throws Exception {
        ClassWithBigNumbers bn = new ClassWithBigNumbers();
        bn.setOneBigInt(BigInteger.valueOf(42L));

        JsonNode generated = JsonGenerator.generateUsingImplementationAsTemplate(bn);
        assertThat(generated).isInstanceOf(JsonObject.class);
        JsonObject jsonObject = (JsonObject) generated;
        assertThat(jsonObject.requiredLong("oneBigInt")).isEqualTo(42L);

    }

    @Test
    @RepeatedTest(repetitions)
    public void shouldHandleBigDecimal() throws Exception {
        ClassWithBigNumbers bn = new ClassWithBigNumbers();
        bn.setOneBigDec(BigDecimal.valueOf(3.14d));

        JsonNode generated = JsonGenerator.generateUsingImplementationAsTemplate(bn);
        assertThat(generated).isInstanceOf(JsonObject.class);
        JsonObject jsonObject = (JsonObject) generated;
        assertThat(jsonObject.requiredDouble("oneBigDec")).isEqualTo(3.14d);

    }

    @Test
    @RepeatedTest(repetitions)
    public void shouldMaskMethodsNotInInterfaceWhenUsingGetter() throws Exception {
        InterfaceWithMethod myInterface = new ClassImplementingInterface("myPublic", "mySecret");
        ClassWithGetterInterface classWithGetterInterface = new ClassWithGetterInterface(myInterface);
        JsonNode generated = JsonGenerator.generate(classWithGetterInterface);
        assertThat(generated).isInstanceOf(JsonObject.class);
        JsonObject jsonObject = (JsonObject) generated;
        JsonObject childObj = jsonObject.requiredObject("myInterface");
        assertThat(childObj.requiredString("publicvalue")).isEqualTo("myPublic");
        assertThat(childObj.stringValue("privatevalue").isPresent()).isFalse();
    }

    @Test
    @RepeatedTest(repetitions)
    public void shouldMaskMethodsNotInInterfaceWhenUsingField() throws Exception {
        InterfaceWithMethod myInterface = new ClassImplementingInterface("myPublic", "mySecret");
        ClassWithFieldInterface classWithFieldInterface = new ClassWithFieldInterface(myInterface);
        JsonNode generated = JsonGenerator.generate(classWithFieldInterface);
        assertThat(generated).isInstanceOf(JsonObject.class);
        JsonObject jsonObject = (JsonObject) generated;
        JsonObject childObj = jsonObject.requiredObject("myInterface");
        assertThat(childObj.requiredString("publicvalue")).isEqualTo("myPublic");
        assertThat(childObj.stringValue("privatevalue").isPresent()).isFalse();
    }

    @Test
    @RepeatedTest(repetitions)
    public void shouldBeAbleToSpesifyGeneratorClass() throws Exception {
        InterfaceWithMethod interfaceWithMethod = new ClassImplementingInterface("myPublic", "mySecret");
        JsonNode generated = JsonGenerator.generateWithSpecifyingClass(interfaceWithMethod, InterfaceWithMethod.class);
        JsonObject jsonObject = (JsonObject) generated;
        assertThat(jsonObject.requiredString("publicvalue")).isEqualTo("myPublic");
        assertThat(jsonObject.stringValue("privatevalue").isPresent()).isFalse();
    }

    @Test
    @RepeatedTest(repetitions)
    public void shouldHandleAnonymousClass() throws Exception {
        InterfaceWithMethod interfaceWithMethod = new InterfaceWithMethod() {
            @Override
            public String getPublicvalue() {
                return "Hello world";
            }
        };
        JsonNode generated = JsonGenerator.generateWithSpecifyingClass(interfaceWithMethod, InterfaceWithMethod.class);
        JsonObject jsonObject = (JsonObject) generated;
        assertThat(jsonObject.requiredString("publicvalue")).isEqualTo("Hello world");
    }

    @Test
    @RepeatedTest(repetitions)
    public void shouldHandleInterfaceTypesInMethodList() throws Exception {
        ClassWithInterfaceListAndMapMethods classWithInterfaceListAndMapMethods = new ClassWithInterfaceListAndMapMethods();
        List<InterfaceWithMethod> myList = new ArrayList<>();
        myList.add(new ClassImplementingInterface("mypub","myPriv"));
        classWithInterfaceListAndMapMethods.setMyList(myList);

        JsonNode generated = JsonGenerator.generate(classWithInterfaceListAndMapMethods);

        assertThat(generated).isInstanceOf(JsonObject.class);
        JsonObject jsonObject = (JsonObject) generated;
        JsonArray interfacelist = jsonObject.requiredArray("myList");
        assertThat(interfacelist).hasSize(1);

        JsonObject interfaceobj = interfacelist.get(0, JsonObject.class);
        assertThat(interfaceobj.requiredString("publicvalue")).isEqualTo("mypub");
        assertThat(interfaceobj.stringValue("privatevalue").isPresent()).isFalse();
   }

    @Test
    @RepeatedTest(repetitions)
    public void shouldHandleInterfaceTypesInMethodMaps() throws Exception {
        ClassWithInterfaceListAndMapMethods classWithInterfaceListAndMapMethods = new ClassWithInterfaceListAndMapMethods();
        Map<String,InterfaceWithMethod> myMap = new HashMap<>();
        myMap.put("mykey",new ClassImplementingInterface("mypub","myPriv"));
        classWithInterfaceListAndMapMethods.setMyMap(myMap);

        JsonNode generated = JsonGenerator.generate(classWithInterfaceListAndMapMethods);

        assertThat(generated).isInstanceOf(JsonObject.class);
        JsonObject jsonObject = (JsonObject) generated;
        JsonObject interfacemap = jsonObject.requiredObject("myMap");
        assertThat(interfacemap.keys()).hasSize(1);
        JsonObject interfaceobj = interfacemap.requiredObject("mykey");
        assertThat(interfaceobj.requiredString("publicvalue")).isEqualTo("mypub");
        assertThat(interfaceobj.stringValue("privatevalue").isPresent()).isFalse();
    }


}
