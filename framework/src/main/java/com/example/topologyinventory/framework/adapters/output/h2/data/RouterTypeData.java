package com.example.topologyinventory.framework.adapters.output.h2.data;

import jakarta.persistence.Embeddable;

/** Espejo de persistencia del value object {@code RouterType} (EDGE / CORE). */
@Embeddable
public enum RouterTypeData {
    EDGE, CORE
}
