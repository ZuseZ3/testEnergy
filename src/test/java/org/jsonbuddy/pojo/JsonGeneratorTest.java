package org.jsonbuddy.pojo;

import org.jsonbuddy.JPowerMonitor.JPowerMonitor;
import org.jsonbuddy.JPowerMonitor.JPowerMonitorFile;
import org.jsonbuddy.JsonArray;
import org.jsonbuddy.JsonFactory;
import org.jsonbuddy.JsonNode;
import org.jsonbuddy.JsonNull;
import org.jsonbuddy.JsonNumber;
import org.jsonbuddy.JsonObject;
import org.jsonbuddy.pojo.testclasses.ClassImplementingInterface;
import org.jsonbuddy.pojo.testclasses.ClassWithBigNumbers;
import org.jsonbuddy.pojo.testclasses.ClassWithDifferentTypes;
import org.jsonbuddy.pojo.testclasses.ClassWithEnum;
import org.jsonbuddy.pojo.testclasses.ClassWithFieldInterface;
import org.jsonbuddy.pojo.testclasses.ClassWithGetterInterface;
import org.jsonbuddy.pojo.testclasses.ClassWithInterfaceListAndMapMethods;
import org.jsonbuddy.pojo.testclasses.ClassWithJsonElements;
import org.jsonbuddy.pojo.testclasses.ClassWithMap;
import org.jsonbuddy.pojo.testclasses.ClassWithStaticFieldsFromInterface;
import org.jsonbuddy.pojo.testclasses.ClassWithSuperclass;
import org.jsonbuddy.pojo.testclasses.ClassWithTime;
import org.jsonbuddy.pojo.testclasses.CombinedClassWithSetter;
import org.jsonbuddy.pojo.testclasses.InterfaceWithMethod;
import org.jsonbuddy.pojo.testclasses.JsonGeneratorOverrides;
import org.jsonbuddy.pojo.testclasses.SimpleWithName;
import org.jsonbuddy.pojo.testclasses.SimpleWithNameGetter;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    public void shouldHandleSimpleClass() {
        SimpleWithName simpleWithName = new SimpleWithName("Darth Vader");
        JsonNode generated = JsonGenerator.generateUsingImplementationAsTemplate(simpleWithName);
        assertThat(generated).isInstanceOf(JsonObject.class);
        JsonObject jsonObject = (JsonObject) generated;
        assertThat(jsonObject.requiredString("name")).isEqualTo("Darth Vader");
    }

    @Test
    @RepeatedTest(repetitions)
    public void shouldHandleSimpleValues() {
        assertThat(JsonGenerator.generateUsingImplementationAsTemplate(null)).isEqualTo(new JsonNull());
        assertThat(JsonGenerator.generateUsingImplementationAsTemplate("Darth")).isEqualTo(JsonFactory.jsonString("Darth"));
        assertThat(JsonGenerator.generateUsingImplementationAsTemplate(42)).isEqualTo(JsonFactory.jsonNumber(42));

    }

    @Test
    @RepeatedTest(repetitions)
    public void shouldHandleFloats() {
        JsonNode jsonNode = JsonGenerator.generateUsingImplementationAsTemplate(3.14f);
        JsonNumber jsonDouble = (JsonNumber) jsonNode;
        assertThat(Double.valueOf(jsonDouble.doubleValue()).floatValue()).isEqualTo(3.14f);
    }

    @Test
    @RepeatedTest(repetitions)
    public void shouldHandleList() {
        List<String> stringlist = Arrays.asList("one", "two", "three");

        JsonNode generate = JsonGenerator.generateUsingImplementationAsTemplate(stringlist);
        assertThat(generate).isInstanceOf(JsonArray.class);
        JsonArray array = (JsonArray) generate;
        assertThat(array.strings()).isEqualTo(stringlist);
    }

    @Test
    @RepeatedTest(repetitions)
    public void shouldHandleArray() {
        String[] stringarray = { "one", "two", "three" };

        JsonNode generate = JsonGenerator.generateUsingImplementationAsTemplate(stringarray);
        assertThat(generate).isInstanceOf(JsonArray.class);
        JsonArray array = (JsonArray) generate;
        assertThat(array.strings()).containsExactly(stringarray);
    }

    @Test
    @RepeatedTest(repetitions)
    public void shouldHandleListWithClasses() {
        List<SimpleWithName> simpleWithNames = Arrays.asList(new SimpleWithName("Darth"), new SimpleWithName("Anakin"));
        JsonArray array = (JsonArray) JsonGenerator.generateUsingImplementationAsTemplate(simpleWithNames);

        List<JsonObject> objects = array.objects(o -> o);

        assertThat(objects.get(0).requiredString("name")).isEqualTo("Darth");
        assertThat(objects.get(1).requiredString("name")).isEqualTo("Anakin");
    }

    @Test
    @RepeatedTest(repetitions)
    public void shouldHandleNestedLists() {
        List<List<String>> nestedList = Arrays.asList(Arrays.asList("Vader", "Sidious"), Arrays.asList("Anakin"));

        JsonArray array = (JsonArray) JsonGenerator.generate(nestedList);

        assertThat(array.requiredArray(0).strings()).containsExactly("Vader", "Sidious");
        assertThat(array.requiredArray(1).strings()).containsExactly("Anakin");
    }

    @Test
    @RepeatedTest(repetitions)
    public void shouldHandleClassWithGetter() {
        CombinedClassWithSetter combinedClassWithSetter = new CombinedClassWithSetter();
        combinedClassWithSetter.setPerson(new SimpleWithName("Darth Vader"));
        combinedClassWithSetter.setOccupation("Dark Lord");

        JsonObject jsonObject = (JsonObject) JsonGenerator.generateUsingImplementationAsTemplate(combinedClassWithSetter);

        assertThat(jsonObject.requiredString("occupation")).isEqualTo("Dark Lord");
        Optional<JsonObject> person = jsonObject.objectValue("person");

        assertThat(person).isPresent();
        assertThat(person.get()).isInstanceOf(JsonObject.class);
        assertThat(person.get().requiredString("name")).isEqualTo("Darth Vader");
    }
    
    @Test
    @RepeatedTest(repetitions)
    public void shouldHandleOptionalValues() {
        assertThat(JsonGenerator.generate(Optional.of(new SimpleWithName("Darth"))))
                .isEqualTo(new JsonObject().put("name", "Darth"));
        assertThat(JsonGenerator.generate(Optional.empty()))
                .isEqualTo(new JsonNull());
    }


    @Test
    @RepeatedTest(repetitions)
    public void shouldHandleOverriddenValues() {
        JsonGeneratorOverrides overrides = new JsonGeneratorOverrides();
        JsonObject generate = (JsonObject) JsonGenerator.generateUsingImplementationAsTemplate(overrides);

        assertThat(generate.requiredLong("myOverriddenValue")).isEqualTo(42);

    }

    @Test
    @RepeatedTest(repetitions)
    public void shouldHandleEmbeddedJson() {
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
    public void shouldMakeMapsIntoObjects() {
        Map<String, String> map = new HashMap<>();
        map.put("name", "Darth Vader");
        ClassWithMap classWithMap = new ClassWithMap(map);
        JsonObject generate = (JsonObject) JsonGenerator.generateUsingImplementationAsTemplate(classWithMap);
        assertThat(generate.requiredObject("properties").requiredString("name")).isEqualTo("Darth Vader");
    }

    @Test
    @RepeatedTest(repetitions)
    public void shouldHandleDifferentSimpleTypes() {
        ClassWithDifferentTypes classWithDifferentTypes = new ClassWithDifferentTypes("my text", 42, true, false);
        JsonObject generated = (JsonObject) JsonGenerator.generateUsingImplementationAsTemplate(classWithDifferentTypes);
        assertThat(generated.requiredString("text")).isEqualTo("my text");
        assertThat(generated.requiredLong("number")).isEqualTo(42);
        assertThat(generated.requiredBoolean("bool")).isTrue();
    }

    @Test
    @RepeatedTest(repetitions)
    public void shouldHandleClassWithTime() {
        OffsetDateTime dateTime = OffsetDateTime.of(2015, 8, 13, 21, 14, 18, 321, ZoneOffset.UTC);
        ClassWithTime classWithTime = new ClassWithTime();
        classWithTime.setTime(dateTime.toInstant());

        JsonObject jsonNode = (JsonObject) JsonGenerator.generateUsingImplementationAsTemplate(classWithTime);
        assertThat(jsonNode.requiredString("time")).isEqualTo("2015-08-13T21:14:18.000000321Z");
    }

    @Test
    @RepeatedTest(repetitions)
    public void shouldHandleClassWithEnum() {
        ClassWithEnum classWithEnum = new ClassWithEnum();
        JsonObject jso = (JsonObject) JsonGenerator.generateUsingImplementationAsTemplate(classWithEnum);
        assertThat(jso.requiredString("enumNumber")).isEqualTo("ONE");
    }

    @Test
    @RepeatedTest(repetitions)
    public void shouldHandleNumbers() {
        assertThat(JsonGenerator.generateUsingImplementationAsTemplate(12L)).isEqualTo(new JsonNumber(12L));
        assertThat(JsonGenerator.generateUsingImplementationAsTemplate(12)).isEqualTo(new JsonNumber(12));
    }

    @Test
    @RepeatedTest(repetitions)
    public void shouldHandleMapWithKeyOtherThanString() {
        Map<Long,String> myLongMap = new HashMap<>();
        myLongMap.put(42L, "Meaning of life");
        JsonNode generated = JsonGenerator.generateUsingImplementationAsTemplate(myLongMap);
        assertThat(generated).isInstanceOf(JsonObject.class);
        JsonObject jsonObject = (JsonObject) generated;
        assertThat(jsonObject.requiredString("42")).isEqualTo("Meaning of life");
    }

    @Test
    @RepeatedTest(repetitions)
    public void shouldHandleBigInteger() {
        ClassWithBigNumbers bn = new ClassWithBigNumbers();
        bn.setOneBigInt(BigInteger.valueOf(42L));

        JsonNode generated = JsonGenerator.generateUsingImplementationAsTemplate(bn);
        assertThat(generated).isInstanceOf(JsonObject.class);
        JsonObject jsonObject = (JsonObject) generated;
        assertThat(jsonObject.requiredLong("oneBigInt")).isEqualTo(42L);

    }

    @Test
    @RepeatedTest(repetitions)
    public void shouldHandleBigDecimal() {
        ClassWithBigNumbers bn = new ClassWithBigNumbers();
        bn.setOneBigDec(BigDecimal.valueOf(3.14d));

        JsonNode generated = JsonGenerator.generateUsingImplementationAsTemplate(bn);
        assertThat(generated).isInstanceOf(JsonObject.class);
        JsonObject jsonObject = (JsonObject) generated;
        assertThat(jsonObject.requiredDouble("oneBigDec")).isEqualTo(3.14d);

    }

    @Test
    @RepeatedTest(repetitions)
    public void shouldMaskMethodsNotInInterfaceWhenUsingGetter() {
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
    public void shouldMaskMethodsNotInInterfaceWhenUsingField() {
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
    public void shouldBeAbleToSpecifyGeneratorClass() {
        InterfaceWithMethod interfaceWithMethod = new ClassImplementingInterface("myPublic", "mySecret");
        JsonNode generated = JsonGenerator.generateWithSpecifyingClass(interfaceWithMethod, InterfaceWithMethod.class);
        JsonObject jsonObject = (JsonObject) generated;
        assertThat(jsonObject.requiredString("publicvalue")).isEqualTo("myPublic");
        assertThat(jsonObject.stringValue("privatevalue").isPresent()).isFalse();
    }

    @Test
    @RepeatedTest(repetitions)
    public void shouldHandleAnonymousClass() {
        InterfaceWithMethod interfaceWithMethod = () -> "Hello world";
        JsonNode generated = JsonGenerator.generateWithSpecifyingClass(interfaceWithMethod, InterfaceWithMethod.class);
        JsonObject jsonObject = (JsonObject) generated;
        assertThat(jsonObject.requiredString("publicvalue")).isEqualTo("Hello world");
    }

    @Test
    @RepeatedTest(repetitions)
    public void shouldHandleInterfaceTypesInMethodList() {
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
    public void shouldHandleInterfaceTypesInMethodMaps() {
        ClassWithInterfaceListAndMapMethods classWithInterfaceListAndMapMethods = new ClassWithInterfaceListAndMapMethods();
        Map<String,InterfaceWithMethod> myMap = new HashMap<>();
        myMap.put("mykey", new ClassImplementingInterface("mypub","myPriv"));
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

    @Test
    @RepeatedTest(repetitions)
    public void shouldNotSerializeStaticFields() {
        ClassWithStaticFieldsFromInterface classWithStaticFields = new ClassWithStaticFieldsFromInterface();
        classWithStaticFields.setName("Darth Vader");

        JsonObject generated = (JsonObject)JsonGenerator.generate(classWithStaticFields);

        assertThat(generated.keys()).containsOnly("name");
    }

    public static class TestClass {
        public final Map<String, List<String>> properties = new HashMap<>();
    }

    @Test
    @RepeatedTest(repetitions)
    public void shouldHandleNestedCollections() {
        TestClass object = new TestClass();
        object.properties.put("test", Arrays.asList("one", "two"));
        assertThat(JsonGenerator.generate(object).toJson())
            .isEqualTo("{\"properties\":{\"test\":[\"one\",\"two\"]}}");
    }

    @Test
    @RepeatedTest(repetitions)
    public void shouldHandleStreams() {
        JsonArray array = (JsonArray) JsonGenerator.generate(Stream.of("one", "two", 3));
        assertThat(array.strings()).containsExactly("one", "two", "3");
        assertThat(array.requiredLong(2)).isEqualTo(3);
    }

    @Test
    @RepeatedTest(repetitions)
    public void shouldHandleStreamOfObjects() throws NoSuchMethodException {
        Type streamType = JsonGeneratorTest.class.getMethod("getStream").getGenericReturnType();
        JsonArray array = (JsonArray) new JsonGenerator().generateNode(getStream(), Optional.of(streamType));
        assertThat(array.requiredObject(0).requiredString("name")).isEqualTo("Darth Vader");
    }

    public Stream<SimpleWithName> getStream() {
        return Stream.of(new SimpleWithName("Darth Vader"));
    }

    @Test
    @RepeatedTest(repetitions)
    public void shouldHandleGenerationWithPut() {
        JsonObject jsonObject = JsonFactory.jsonObject().put("myNested",new JsonGeneratorOverrides());
        assertThat(jsonObject.requiredObject("myNested").requiredLong("myOverriddenValue")).isEqualTo(42L);
    }
    
    @Test
    @RepeatedTest(repetitions)
    public void shouldSupportGenerationWithSnakeCasing() {
        SimpleWithNameGetter object = new SimpleWithNameGetter();
        object.setFullName("Darth Vader");
        assertThat(new JsonGenerator().withNameTransformer(JsonGenerator.UNDERSCORE_TRANSFORMER).generateNode(object))
                .isEqualTo(new JsonObject().put("full_name", "Darth Vader"));
    }
    
    @Test
    @RepeatedTest(repetitions)
    public void shouldMapPropertiesInSuperclass() {
        ClassWithSuperclass object = new ClassWithSuperclass();
        object.setType("some type");
        object.setName("some name");
        assertThat(new JsonGenerator().generateNode(object))
                .isEqualTo(new JsonObject().put("type", "some type").put("name", "some name"));
    }

    @Test
    @RepeatedTest(repetitions)
    public void optionalEmptyShouldReturnJsonNull() {
        assertThat(new JsonGenerator().generateNode(Optional.empty())).isEqualTo(new JsonNull());
    }
    
    @Test
    @RepeatedTest(repetitions)
    public void optionalPresentShouldReturnObject() {
        assertThat(new JsonGenerator().generateNode(Optional.of(
                new SimpleWithName("Darth Vader")
        ))).isEqualTo(new JsonObject().put("name", "Darth Vader"));
    }
    
}
