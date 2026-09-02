package com.example.topologyinventory.framework.adapters.output.h2.data;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

/**
 * Espejo de persistencia de la entidad {@code Switch} del dominio.
 *
 * Modela la tabla {@code switches} y su relación con {@code networks}. Guarda el
 * {@code router_id} del EdgeRouter al que pertenece: la persistencia del switch
 * ocurre siempre a través de la raíz del agregado (el router), nunca de forma
 * independiente.
 *
 * <p>Persistencia gestionada por Hibernate ORM. UUID nativo (sin converter),
 * enums con {@link Enumerated}, y la colección {@link OneToMany} de redes en solo
 * lectura sobre la columna {@code networks.switch_id}.
 */
@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "switches")
public class SwitchData implements Serializable {

    @Id
    @Column(name = "switch_id", columnDefinition = "uuid", updatable = false)
    private UUID switchId;

    @Column(name = "router_id")
    private UUID routerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "switch_vendor")
    private VendorData switchVendor;

    @Enumerated(EnumType.STRING)
    @Column(name = "switch_model")
    private ModelData switchModel;

    @Enumerated(EnumType.STRING)
    @Column(name = "switch_type")
    private SwitchTypeData switchType;

    @OneToMany
    @JoinColumn(name = "switch_id", insertable = false, updatable = false)
    private List<NetworkData> networks;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "address",
                    column = @Column(name = "switch_ip_address")),
            @AttributeOverride(name = "protocol",
                    column = @Column(name = "switch_ip_protocol")),
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
    private LocationData switchLocation;
}
