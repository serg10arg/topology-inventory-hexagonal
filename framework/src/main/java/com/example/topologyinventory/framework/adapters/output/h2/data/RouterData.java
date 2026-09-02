package com.example.topologyinventory.framework.adapters.output.h2.data;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

/**
 * Espejo de persistencia del agregado {@code Router} del dominio.
 *
 * Modela la tabla {@code routers} y sus dos relaciones: la auto-relación (un
 * CoreRouter contiene otros routers, vía la columna {@code router_parent_core_id})
 * y la relación con {@code switches}. No contiene lógica de negocio: es una
 * estructura plana orientada a la base de datos que el {@code RouterH2Mapper}
 * traduce desde y hacia la entidad de dominio.
 *
 * <p>Persistencia gestionada por Hibernate ORM (Quarkus). El identificador se
 * mapea como {@code UUID} nativo de H2 ({@code columnDefinition = "uuid"}), sin
 * converter propietario. Los enums se mapean con {@link Enumerated} (no como
 * {@code @Embedded}), y las asociaciones {@link OneToMany} comparten la columna
 * FK escalar ya mapeada, en modo solo lectura ({@code insertable=false,
 * updatable=false}), de modo que Hibernate no genere ni tabla de join ni columnas
 * duplicadas. La persistencia es solo por la raíz del agregado (el router); las
 * colecciones no se cascan.
 */
@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "routers")
public class RouterData implements Serializable {

    @Id
    @Column(name = "router_id", columnDefinition = "uuid", updatable = false)
    private UUID routerId;

    @Column(name = "router_parent_core_id")
    private UUID routerParentCoreId;

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
