package io.cominotti.example;

import java.util.UUID;

public final class OrderId {
    private final UUID value;

    public OrderId(UUID value) {
        this.value = value;
    }

    public UUID value() {
        return value;
    }
}
