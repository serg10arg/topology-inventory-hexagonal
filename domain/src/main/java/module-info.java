/**
 * Descriptor del módulo del Domain hexagon.
 *
 * Exporta los paquetes que las capas superiores (Application y Framework)
 * consumirán: entities, value objects, domain services y specifications. Los
 * subpaquetes 'specification.shared' y 'exception' se dejan SIN exportar a
 * propósito: son detalle de implementación interno.
 *
 * 'requires static lombok' hace que Lombok solo esté presente en tiempo de
 * compilación (genera getters, builders…), no como dependencia en runtime.
 */
module domain {
    exports com.example.topologyinventory.domain.entity;
    exports com.example.topologyinventory.domain.service;
    exports com.example.topologyinventory.domain.specification;
    exports com.example.topologyinventory.domain.vo;

    requires static lombok;
}