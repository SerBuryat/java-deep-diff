package com.thunder.base;

import com.thunder.base.diff.DiffManager;
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
        var order2 = new Order(UUID.randomUUID(), "order1", null, event2);

        var diff = DiffManager.diff(order1, order2);
        System.out.println(diff);
    }

}