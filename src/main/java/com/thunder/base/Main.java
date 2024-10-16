package com.thunder.base;

import com.thunder.base.diff.DiffManager;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Main {

    public static void main(String[] args) {
        record SportObject(String name) {}
        record Event(Integer id, String name, SportObject sportObject) {}
        record Order(UUID id, String name, String comment, Event event) {}

        var sportObject = new SportObject("obj1");
        var event1 = new Event(1, "event1", sportObject);
        var event2 = new Event(2, "event2", sportObject);
        var order1 = new Order(UUID.randomUUID(), "order1", "comment1", event1);
        var uuid = UUID.randomUUID();
        var order2 = new Order(uuid, "order1", null, event2);
        var order3 = new Order(uuid, "order1", null, event2);

        var diff = DiffManager.diff(List.of(order2), List.of(order3));

        var map1 = Map.of(
                "key1", order1
        );
        var map2 = new HashMap<>();
        map2.put("key1", order1);

        var mapsDiff = DiffManager.diff(map1, map2);
        System.out.println(mapsDiff);
    }

}