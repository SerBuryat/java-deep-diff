package com.thunder.base.utils.diff;

import static com.thunder.base.utils.diff.ResultNode.ResultNodeState.ADDED;
import static com.thunder.base.utils.diff.ResultNode.ResultNodeState.CHANGED;
import static com.thunder.base.utils.diff.ResultNode.ResultNodeState.REMOVED;
import static com.thunder.base.utils.diff.ResultNode.ResultNodeState.UNTOUCHED;
import static java.util.Collections.emptyList;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class DiffManager {

    // todo - make customizable to custom or other 'simple types' as `UUID` or `Number`?
    private static final List<Class<?>> CLASSES_FOR_SIMPLE_DIFF =
            List.of(
                    String.class, Boolean.class, Byte.class, Short.class, Integer.class,
                    Long.class, Float.class, Double.class, Character.class
            );

    // todo - make custom diff rules for some user classes to skip some fields or value or etc.?

    // todo - or make `null`?
    static final String ROOT_FIELD_NAME = "ROOT";

    static ResultNode diff(Object oldValue, Object newValue) {
        return diff(ROOT_FIELD_NAME, oldValue, newValue);
    }

    private static ResultNode diff(String fieldName, Object oldObject, Object newObject) {
        // equals or both == null
        if(Objects.equals(oldObject, newObject)) {
            return new ResultNode(fieldName, oldObject, newObject, UNTOUCHED, emptyList());
        }

        // some == null
        if(Objects.isNull(oldObject)) {
            return new ResultNode(fieldName, null, newObject, ADDED, emptyList());
        }
        if(Objects.isNull(newObject)) {
            return new ResultNode(fieldName, oldObject, null, REMOVED, emptyList());
        }

        // todo - what about parent and child classes (extends, implements)?
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
                    fieldName, oldObject, newObject, diffSimple(oldObject, newObject), emptyList()
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

    private static ResultNode.ResultNodeState diffSimple(Object baseValue, Object comparableValue) {
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
            return new ResultNode(fieldName, oldArray, newArray, UNTOUCHED, emptyList());
        }

        var resultNodeChildren = new ArrayList<ResultNode>();
        for(int i = 0; i < oldArray.length; i++) {
            var oldArrayItem = oldArray[i];
            if(i >= newArray.length) {
                resultNodeChildren.add(
                        new ResultNode(String.valueOf(i), oldArrayItem, null, REMOVED, emptyList())
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
                        new ResultNode(String.valueOf(i), null, newArrayItem, ADDED, emptyList())
                );
            }
        }

        return new ResultNode(
                fieldName, oldArray, newArray,
                getStateBasedOnChildrenStates(resultNodeChildren), resultNodeChildren
        );
    }

    private static ResultNode diffMap(String fieldName, Map<?,?> oldMap, Map<?,?> newMap) {
        if(oldMap.equals(newMap)) {
            // todo - need to fill with children nodes? (every children-entry is UNTOUCHED)
            return new ResultNode(fieldName, oldMap, newMap, UNTOUCHED, emptyList());
        }

        var resultNodeChildren = new ArrayList<ResultNode>();

        oldMap.forEach((oldKey, oldValue) -> {
            // if no `oldMap` keys in `newMap` -> REMOVED
            if(newMap.get(oldKey) == null) {
                resultNodeChildren.add(
                        new ResultNode(
                                "key",
                                oldKey,
                                null,
                                REMOVED,
                                List.of(
                                        new ResultNode(
                                                "value", oldValue, null, REMOVED, emptyList()
                                        )
                                )
                        )
                );
            } else {
                // If `oldMap` keys found in `newMap` -> CHANGED or UNTOUCHED
                var valuesDiff = diff("value",  oldMap.get(oldKey), newMap.get(oldKey));
                resultNodeChildren.add(
                        new ResultNode(
                                "key",
                                oldKey,
                                // here oldKey == newKey
                                oldKey,
                                valuesDiff.state(),
                                List.of(valuesDiff)
                        )
                );
            }
        });

        // if no `newMap` keys in `oldMap` -> REMOVED
        newMap.forEach((newKey, newValue) -> {
            if(oldMap.get(newKey) == null) {
                resultNodeChildren.add(
                        new ResultNode(
                                "key",
                                null,
                                newKey,
                                ADDED,
                                List.of(
                                        new ResultNode(
                                                "value", null, newValue, ADDED, emptyList()
                                        )
                                )
                        )
                );
            }
        });

        return new ResultNode(
                fieldName, oldMap, newMap, getStateBasedOnChildrenStates(resultNodeChildren), resultNodeChildren
        );
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

    private static ResultNode.ResultNodeState getStateBasedOnChildrenStates(List<ResultNode> children) {
        return children.stream().allMatch(child -> child.state() == UNTOUCHED)
                ? UNTOUCHED
                : CHANGED;
    }

}
