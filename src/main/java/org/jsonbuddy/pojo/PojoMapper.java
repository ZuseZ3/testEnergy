package org.jsonbuddy.pojo;

import org.jsonbuddy.*;
import org.jsonbuddy.parse.JsonParseException;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class PojoMapper {

    private static Map<Class<?>,JsonPojoBuilder<?>> globalPojoBuilders = new HashMap<>();

    private Map<Class<?>,JsonPojoBuilder<?>> pojoBuilders = new HashMap<>();

    public static <T> T map(JsonObject jsonObject,Class<T> clazz) {
        return new PojoMapper().mapToPojo(jsonObject,clazz);
    }

    public static <T> List<T> map(JsonArray jsonArray,Class<T> listClazz) {
        return new PojoMapper().mapToPojo(jsonArray,listClazz);
    }

    private PojoMapper() {
        pojoBuilders.putAll(globalPojoBuilders);
    }

    public static PojoMapper create() {
        return new PojoMapper();
    }


    public <T> PojoMapper registerClassBuilder(Class<T> clazz,JsonPojoBuilder<T> jsonPojoBuilder) {
        pojoBuilders.put(clazz, jsonPojoBuilder);
        return this;
    }

    public static <T> void registerGlobalClassBuilder(Class<T> clazz,JsonPojoBuilder<T> jsonPojoBuilder) {
        globalPojoBuilders.put(clazz,jsonPojoBuilder);
    }

    public <T> T mapToPojo(JsonObject jsonObject,Class<T> clazz) throws CanNotMapException {
        try {
            return (T) mapit(jsonObject,clazz);
        } catch (Exception e) {
            ExceptionUtil.soften(e);
            return null;
        }
    }

    public  <T> List<T> mapToPojo(JsonArray jsonArray,Class<T> listClazz) throws CanNotMapException {
        return jsonArray.nodeStream()
                .filter(node -> node instanceof JsonObject)
                .map(node -> mapToPojo((JsonObject) node,listClazz))
                .collect(Collectors.toList());
    }

    private Object mapit(JsonNode jsonNode,Class<?> clazz) throws Exception {
        if (clazz.isAnnotationPresent(OverrideMapper.class)) {
            OverrideMapper[] annotationsByType = clazz.getAnnotationsByType(OverrideMapper.class);
            return annotationsByType[0].using().newInstance().build(jsonNode);
        }
        if (jsonNode instanceof JsonSimpleValue) {
            return ((JsonSimpleValue) jsonNode).javaObjectValue();
        }
        if (jsonNode instanceof JsonArray) {
            return mapArray((JsonArray) jsonNode,clazz);
        }
        JsonObject jsonObject = (JsonObject) jsonNode;
        JsonPojoBuilder<?> jsonPojoBuilder = pojoBuilders.get(clazz);
        if (jsonPojoBuilder != null) {
            return jsonPojoBuilder.build(jsonObject);
        }

        Object result;
        Constructor<?>[] declaredConstructors = Optional.ofNullable(clazz.getDeclaredConstructors()).orElse(new Constructor[0]);
        result = null;
        for (Constructor<?> constructor : declaredConstructors) {
            if (constructor.getParameterCount() == 0) {
                boolean accessible = constructor.isAccessible();
                if (!accessible) {
                    constructor.setAccessible(true);
                }
                result = constructor.newInstance();
                if (!accessible) {
                    constructor.setAccessible(false);

                }
                break;
            }
        }
        if (result == null) {
            throw new CanNotMapException(String.format("Class %s has no default constructor",clazz.getName()));
        }
        for (String key : jsonObject.keys()) {
            if (findField(clazz, jsonObject, result, key)) {
                continue;
            }
            if (findSetter(jsonObject, clazz, result, key)) {
                continue;
            }
        }

        return result;
    }

    private Object mapArray(JsonArray jsonArray, Class<?> clazz) {
        return jsonArray.nodeStream().map(jn -> {
            try {
                return mapit(jn, clazz);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toList());
    }

    private boolean findField(Class<?> clazz, JsonObject jsonObject, Object result, String key) throws Exception {
        Field declaredField = null;
        try {
            declaredField = clazz.getDeclaredField(key);
        } catch (NoSuchFieldException e) {
            return false;
        }
        JsonNode nodeValue = jsonObject.value(key).get();
        Object value;
        OverriddenVal overriddenValue = overriddenValue(declaredField.getType(), nodeValue);
        if (overriddenValue.ovverridden) {
            value = overriddenValue.value;
        } else if (declaredField.getType().isAssignableFrom(nodeValue.getClass())) {
            value = nodeValue;
        } else if (Map.class.isAssignableFrom(declaredField.getType()) && (nodeValue instanceof JsonObject)) {
            value = mapAsMap((ParameterizedType) declaredField.getGenericType(),(JsonObject) nodeValue);
        } else {
            value = mapit(nodeValue, computeType(declaredField, nodeValue));
            value = convertIfNessesary(value,declaredField.getType());
        }

        declaredField.setAccessible(true);
        declaredField.set(result,value);
        declaredField.setAccessible(false);
        return true;
    }

    private Map<String, Object> mapAsMap(ParameterizedType genericType, JsonObject nodeValue) throws Exception {
        Class<?> valueclass;
        String typeName = genericType.getActualTypeArguments()[1].getTypeName();
        int genericStart = typeName.indexOf("<");
        if (genericStart != -1) {
            typeName = typeName.substring(genericStart+1,typeName.indexOf(">"));
        }
        try {
            valueclass = Class.forName(typeName);
        } catch (ClassNotFoundException e) {
            throw new JsonParseException("Could not find class " + typeName) ;
        }

        Map<String, Object> result = new HashMap<>();
        for (String key : nodeValue.keys()) {
            result.put(key, mapit(nodeValue.value(key).get(), valueclass));
        }
        return result;
    }

    private Object convertIfNessesary(Object value, Class<?> destinationType) {
        if (value == null) {
            return null;
        }
        if (destinationType.isAssignableFrom(value.getClass())) {
            return value;
        }
        if (value instanceof Long && Integer.class.equals(destinationType)) {
            int intval = Integer.parseInt(value.toString());
            return intval;
        }
        if (value instanceof Integer && Long.class.equals(destinationType)) {
            long longval = (long) value;
            return longval;
        }
        if (Integer.class.equals(destinationType) && (value instanceof String)) {
            return Integer.parseInt((String) value);
        }
        if (Long.class.equals(destinationType) && (value instanceof String)) {
            return Long.parseLong((String) value);
        }
        if (destinationType.isAssignableFrom(Integer.class) && (value instanceof String)) {
            return Integer.parseInt((String) value);
        }
        if (destinationType.isAssignableFrom(Long.class) && (value instanceof String)) {
            return Long.parseLong((String) value);
        }
        if (destinationType.isEnum() && (value instanceof String)) {
            String stringValue = (String) value;
            Object[] enumConstants = destinationType.getEnumConstants();
            return Arrays.asList(enumConstants).stream()
                    .filter(o -> stringValue.equals(o.toString()))
                    .findAny().get();
        }
        return value;
    }

    private static class OverriddenVal {
        private boolean ovverridden;
        private Object value;

        public OverriddenVal(boolean ovverridden, Object value) {
            this.ovverridden = ovverridden;
            this.value = value;
        }
    }

    private static OverriddenVal overriddenValue(Class declaredClass,JsonNode nodValue) {
        if (declaredClass.isAnnotationPresent(OverrideMapper.class)) {
            OverrideMapper[] annotationsByType = (OverrideMapper[]) declaredClass.getAnnotationsByType(OverrideMapper.class);
            try {
                Object res = annotationsByType[0].using().newInstance().build(nodValue);
                return new OverriddenVal(true,res);
            } catch (InstantiationException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        return new OverriddenVal(false,null);
    }

    private static Class<?> computeType(Field declaredField, JsonNode nodeValue) {
        if (!(nodeValue instanceof JsonArray)) {
            return declaredField.getType();
        }
        AnnotatedType annotatedType = declaredField.getAnnotatedType();
        AnnotatedParameterizedType para = (AnnotatedParameterizedType) annotatedType;
        Type listType = para.getAnnotatedActualTypeArguments()[0].getType();
        try {
            return Class.forName(listType.getTypeName());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean findSetter(JsonObject jsonObject, Class<?> clazz, Object instance, String key) throws Exception {
        String setterName = "set" + Character.toUpperCase(key.charAt(0)) + key.substring(1);
        Optional<Method> setter = Arrays.asList(clazz.getMethods()).stream()
                .filter(met -> setterName.equals(met.getName()) && met.getParameterCount() == 1)
                .findAny();
        if (!setter.isPresent()) {
            return false;
        }

        Method method = setter.get();
        Class<?> setterClass = method.getParameterTypes()[0];
        Object value;
        OverriddenVal overriddenValue = overriddenValue(setterClass, jsonObject);
        if (overriddenValue.ovverridden) {
            value = overriddenValue.value;
        } else if (setterClass.isAssignableFrom(jsonObject.getClass())) {
            value = jsonObject;
        } else if (Map.class.isAssignableFrom(setterClass) && (jsonObject.objectValue(key).isPresent())) {
            value = mapAsMap((ParameterizedType) method.getGenericParameterTypes()[0],jsonObject.requiredObject(key));
        } else {
            value = mapit(jsonObject.value(key).get(),setterClass);
            value = convertIfNessesary(value, setterClass);
        }
        method.invoke(instance,value);
        return true;
    }

}
