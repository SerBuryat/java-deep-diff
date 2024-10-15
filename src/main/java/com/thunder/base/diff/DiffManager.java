package com.thunder.base.diff;

import static com.thunder.base.diff.ResultNodeState.ADDED;
import static com.thunder.base.diff.ResultNodeState.CHANGED;
import static com.thunder.base.diff.ResultNodeState.REMOVED;
import static com.thunder.base.diff.ResultNodeState.UNTOUCHED;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class DiffManager {

    // make customizable?
    private static final List<Class<?>> CLASSES_FOR_SIMPLE_DIFF =
            List.of(
                    String.class, Boolean.class, Byte.class, Short.class, Integer.class,
                    Long.class, Float.class, Double.class, Character.class, UUID.class
            );

    static final String ROOT_FIELD_NAME = "ROOT";

    public static ResultNode diff(Object oldValue, Object newValue) {
        return diff(ROOT_FIELD_NAME, oldValue, newValue);
    }

    private static ResultNode diff(String fieldName, Object oldObject, Object newObject) {
        // equals or both == null
        if(Objects.equals(oldObject, newObject)) {
            return new ResultNode(fieldName, oldObject, newObject, UNTOUCHED, Collections.emptyList());
        }

        // some == null
        if(Objects.isNull(oldObject)) {
            return new ResultNode(fieldName, null, newObject, ADDED, Collections.emptyList());
        }
        if(Objects.isNull(newObject)) {
            return new ResultNode(fieldName, oldObject, null, REMOVED, Collections.emptyList());
        }

        // not equals classes
        var oldObjectClass = oldObject.getClass();
        var newObjectClass = newObject.getClass();
        if(!oldObject.getClass().equals(newObject.getClass())) {
            throw new IllegalArgumentException(
                    String.format(
                            "Classes mismatch exception! (oldObject: '%s', newObject: '%s')",
                            oldObjectClass, newObjectClass
                    )
            );
        }

        // class == primitive or included in classes list
        if(oldObjectClass.isPrimitive() || CLASSES_FOR_SIMPLE_DIFF.contains(oldObjectClass)) {
            return new ResultNode(
                    fieldName, oldObject, newObject, diffSimple(oldObject, newObject), Collections.emptyList()
            );
        }

        // class == Array
        if(oldObjectClass.isArray()) {
            return diffArray(fieldName, DiffUtils.toObjectArray(oldObject), DiffUtils.toObjectArray(newObject));
        }
        // class == Collection
        if(oldObject instanceof Collection<?>) {
            return diffArray(fieldName, ((Collection<?>) oldObject).toArray(), ((Collection<?>) newObject).toArray());
        }

        // class == Map
        if(oldObject instanceof Map<?,?>) {
            return diffMap(fieldName, (Map<?,?>) oldObject, (Map<?,?>) newObject);
        }

        return diffObject(fieldName, oldObjectClass, oldObject, newObjectClass, newObject);
    }

    private static ResultNodeState diffSimple(Object baseValue, Object comparableValue) {
        if(Objects.isNull(baseValue) && Objects.nonNull(comparableValue)) {
            return ADDED;
        }
        if(Objects.nonNull(baseValue) && Objects.isNull(comparableValue)) {
            return REMOVED;
        }
        if(!baseValue.equals(comparableValue)) {
            return CHANGED;
        }
        return UNTOUCHED;
    }

    private static ResultNode diffArray(String fieldName, Object[] oldArray, Object[] newArray) {
        if(Arrays.equals(oldArray, newArray)) {
            return new ResultNode(fieldName, oldArray, newArray, UNTOUCHED, Collections.emptyList());
        }

        var resultNodeChildren = new ArrayList<ResultNode>();
        for(int i = 0; i < oldArray.length; i++) {
            var oldArrayItem = oldArray[i];
            if(i >= newArray.length) {
                resultNodeChildren.add(
                        new ResultNode(String.valueOf(i), oldArrayItem, null, REMOVED, Collections.emptyList())
                );
            }
            var newArrayItem = newArray[i];

            resultNodeChildren.add(
                    diff(String.format("array[%s]", i), oldArrayItem, newArrayItem)
            );
        }

        if(oldArray.length < newArray.length) {
            for (int i = oldArray.length;i < newArray.length - 1; i++) {
                var newArrayItem = newArray[i];
                resultNodeChildren.add(
                        new ResultNode(String.valueOf(i), null, newArrayItem, ADDED, Collections.emptyList())
                );
            }
        }

        return new ResultNode(
                fieldName, oldArray, newArray,
                getStateBasedOnChildrenStates(resultNodeChildren), resultNodeChildren
        );
    }

    private static ResultNode diffMap(String fieldName, Map<?,?> oldValue, Map<?,?> newValue) {
        // todo
        return null;
    }

    private static ResultNode diffObject(String fieldName,
                                         Class<?> oldObjectClass, Object oldObject,
                                         Class<?> newObjectClass, Object newObject) {
        var resultNodeChildren = new ArrayList<ResultNode>();
        for(Field oldObjectField : oldObjectClass.getDeclaredFields()) {
            var oldObjectFieldName = oldObjectField.getName();
            try {
                var newObjectField = newObjectClass.getDeclaredField(oldObjectFieldName);
                resultNodeChildren.add(
                        diff(
                                oldObjectFieldName,
                                getFieldValue(oldObjectField, oldObject),
                                getFieldValue(newObjectField, newObject)
                        )
                );
            } catch (NoSuchFieldException e) {
                throw new RuntimeException(
                        String.format("New object doesn't have field: '%s', but old object has", oldObjectFieldName)
                );
            }
        }

        return new ResultNode(
                fieldName, oldObject, newObject, getStateBasedOnChildrenStates(resultNodeChildren), resultNodeChildren
        );
    }

    private static Object getFieldValue(Field field, Object object) {
        try {
            field.setAccessible(true);
            return field.get(object);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (NullPointerException e) {
            return null;
        }
    }

    private static ResultNodeState getStateBasedOnChildrenStates(List<ResultNode> children) {
        return children.stream().allMatch(child -> child.state() == UNTOUCHED)
                ? UNTOUCHED
                : CHANGED;
    }

}
