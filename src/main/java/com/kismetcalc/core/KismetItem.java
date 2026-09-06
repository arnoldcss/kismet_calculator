package com.kismetcalc.core;


public record KismetItem(
    String name,
    String itemId,
    int quality,
    int weight,
    int prime,
    long addedCost
) {
}
