package com.example.topologyinventory.framework.adapters.output.h2.data;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.util.List;

/**
 * Espejo de persistencia del agregado {@code Router} del dominio.
 *
 * Modela la tabla {@code routers} y sus dos relaciones: la auto-relación (un
 * CoreRouter contiene otros routers, vía la columna {@code router_parent_core_id})
 * y la relación con {@code switches}. No contiene lógica de negocio: es una
 * estructura plana orientada a la base de datos que el {@code RouterH2Mapper}
 * traduce desde y hacia la entidad de dominio.
 *
 * <p>Persistencia gestionada por Hibernate Reactive (Quarkus). El identificador y la FK al core
 * padre se declaran como {@link String} sobre columnas {@code VARCHAR(36)} (Fase 8): se retiró el
 * {@code columnDefinition = "uuid"}, específico de H2, para poder correr sobre MySQL. El dominio
 * sigue usando {@code UUID} dentro de su value object {@code Id}; la conversión a texto ocurre en
 * el mapper, en la frontera. Los enums se mapean con {@link Enumerated} (no como {@code @Embedded}), y las
 * asociaciones {@link OneToMany} comparten la FK escalar ya mapeada, en solo lectura
 * ({@code insertable=false, updatable=false}), de modo que Hibernate no genere ni tabla de
 * join ni columnas duplicadas. Bajo Hibernate Reactive esas colecciones se navegan de forma
 * explícita ({@code session.fetch}), nunca perezosamente en el hilo del mapper. La
 * persistencia es solo por la raíz; las colecciones aún no se cascan (deuda que salda SC2).
 */
@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "routers")
public class RouterData implements Serializable {

    @Id
    @Column(name = "router_id", length = 36, updatable = false)
    private String routerId;

    @Column(name = "router_parent_core_id", length = 36)
    private String routerParentCoreId;

    @Enumerated(EnumType.STRING)
    @Column(name = "router_vendor")
    private VendorData routerVendor;

    @Enumerated(EnumType.STRING)
    @Column(name = "router_model")
    private ModelData routerModel;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "address",
                    column = @Column(name = "router_ip_address")),
            @AttributeOverride(name = "protocol",
                    column = @Column(name = "router_ip_protocol")),
    })
    private IPData ip;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "address",
                    column = @Column(name = "location_address")),
            @AttributeOverride(name = "city",
                    column = @Column(name = "location_city")),
            @AttributeOverride(name = "state",
                    column = @Column(name = "location_state")),
            @AttributeOverride(name = "zipcode",
                    column = @Column(name = "location_zipcode")),
            @AttributeOverride(name = "country",
                    column = @Column(name = "location_country")),
            @AttributeOverride(name = "latitude",
                    column = @Column(name = "location_latitude")),
            @AttributeOverride(name = "longitude",
                    column = @Column(name = "location_longitude")),
    })
    private LocationData routerLocation;

    @Enumerated(EnumType.STRING)
    @Column(name = "router_type")
    private RouterTypeData routerType;

    /**
     * Switches conectados a este router (edge). Asociación unidireccional
     * respaldada por la columna {@code switches.router_id}, ya mapeada como
     * escalar en {@link SwitchData}; se declara en solo lectura para que Hibernate
     * no intente volver a gobernar esa columna ni crear una tabla de join.
     */
    @OneToMany
    @JoinColumn(name = "router_id", insertable = false, updatable = false)
    @Setter
    private List<SwitchData> switches;

    /**
     * Routers hijos de este core. Auto-relación respaldada por la columna
     * {@code routers.router_parent_core_id} (self-FK), también en solo lectura.
     * Sustituye al antiguo {@code @JoinTable(name = "routers")}, que bajo Hibernate
     * colisionaba con la propia tabla de la entidad.
     */
    @OneToMany
    @JoinColumn(name = "router_parent_core_id", insertable = false, updatable = false)
    @Setter
    private List<RouterData> routers;
}
