package com.example.topologyinventory.framework.adapters.output.h2;

import com.example.topologyinventory.application.ports.output.RouterManagementOutputPort;
import com.example.topologyinventory.domain.entity.Router;
import com.example.topologyinventory.domain.vo.Id;
import com.example.topologyinventory.framework.adapters.output.h2.data.RouterData;
import com.example.topologyinventory.framework.adapters.output.h2.data.RouterTypeData;
import com.example.topologyinventory.framework.adapters.output.h2.data.SwitchData;
import com.example.topologyinventory.framework.adapters.output.h2.mappers.RouterH2Mapper;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.hibernate.reactive.mutiny.Mutiny;

import java.util.ArrayList;
import java.util.List;

/**
 * Output adapter que implementa {@link RouterManagementOutputPort} con <b>Hibernate Reactive
 * puro</b> (sin Panache, decisión D1) sobre MySQL. Encapsula la tecnología de persistencia y
 * traduce a/desde el dominio con {@link RouterH2Mapper}, de modo que el núcleo nunca ve tipos de
 * base de datos.
 *
 * <p><b>Cableado (CDI).</b> Bean {@code @ApplicationScoped} descubierto por Jandex e inyectado
 * donde {@code RouterManagementInputPort} declara {@link RouterManagementOutputPort}. Bajo
 * Hibernate Reactive, {@code @Inject EntityManager} → {@code @Inject Mutiny.SessionFactory} y
 * {@code @Transactional} → {@code withSession}/{@code withTransaction}.
 *
 * <p><b>Simetría lectura/escritura profundas (Fase 8).</b> El agregado se recorre entero en ambas
 * direcciones, y ambas travesías comparten la misma restricción: una {@link Mutiny.Session}
 * <em>no admite operaciones concurrentes</em>.
 * <ul>
 *   <li><b>Lectura</b> ({@code retrieveRouter}): {@code find} + {@code session.fetch} por niveles
 *       (CORE → routers hijos; EDGE → switches → redes), con los fetch de redes serializados
 *       ({@code usingConcurrencyOf(1)}).</li>
 *   <li><b>Escritura</b> ({@code persistRouter}): se aplana el árbol {@code *Data} en orden de
 *       dependencia de FK (padre antes que hijo) y se persiste fila a fila con
 *       {@code transformToUniAndConcatenate} —secuencial— dentro de una única transacción. Esto
 *       es <b>cascade manual</b> (decisión D6): las asociaciones {@code @OneToMany} siguen en
 *       solo lectura y la relación se materializa escribiendo la FK escalar de cada fila; no se
 *       usa {@code CascadeType} de JPA.</li>
 * </ul>
 * Un router recién creado sin hijos aplana a una lista de un solo elemento, así que la ruta
 * {@code /router/create} se comporta igual que en SC1.
 */
@ApplicationScoped
public class RouterManagementH2Adapter implements RouterManagementOutputPort {

    /** Factoría de sesiones reactivas de Hibernate Reactive, inyectada por Quarkus. */
    @Inject
    Mutiny.SessionFactory sessionFactory;

    @Override
    public Uni<Router> retrieveRouter(Id id) {
        return sessionFactory.withSession(session ->
                session.find(RouterData.class, id.getId().toString())
                        .flatMap(routerData -> {
                            if (routerData == null) {
                                return Uni.createFrom().nullItem();
                            }
                            if (routerData.getRouterType() == RouterTypeData.CORE) {
                                // CORE: basta materializar los routers hijos (la respuesta expone ids).
                                return session.fetch(routerData.getRouters())
                                        .map(children ->
                                                RouterH2Mapper.coreWithChildren(routerData, children));
                            }
                            // EDGE: materializar switches y, por cada uno, sus redes (dos niveles).
                            return session.fetch(routerData.getSwitches())
                                    .flatMap(switches -> fetchNetworks(session, switches))
                                    .map(switches ->
                                            RouterH2Mapper.edgeWithSwitches(routerData, switches));
                        }));
    }

    @Override
    public Uni<Router> persistRouter(Router router) {
        var routerData = RouterH2Mapper.routerDomainToData(router);
        List<Object> aggregate = flatten(routerData);
        return sessionFactory.withTransaction((session, tx) ->
                        Multi.createFrom().iterable(aggregate)
                                // Secuencial a propósito: Concatenate, no Merge. Persistir en
                                // paralelo sobre la misma sesión reactiva rompería su máquina de
                                // estados, igual que el fetch en paralelo la rompía en la lectura.
                                .onItem().transformToUniAndConcatenate(session::persist)
                                .onItem().ignoreAsUni())
                .replaceWith(router);
    }

    /**
     * Inicia de forma reactiva la colección de redes de cada switch, devolviendo la lista de
     * switches ya con sus redes materializadas, lista para el mapper.
     *
     * <p><b>Concurrencia 1, a propósito.</b> Se arma una {@code Uni} por switch y se agrupan con
     * {@code Uni.join()}, pero con {@code usingConcurrencyOf(1)}: una sesión de Hibernate Reactive
     * <em>no admite operaciones simultáneas</em>. Abanicar los fetch en paralelo sobre la misma
     * sesión rompe su máquina de estados y lanza
     * {@code IllegalStateException: Illegal pop() with non-matching JdbcValuesSourceProcessingState}.
     * Serializándolos, cada fetch espera al anterior y la sesión atiende de uno en uno.
     */
    private Uni<List<SwitchData>> fetchNetworks(Mutiny.Session session, List<SwitchData> switches) {
        if (switches == null || switches.isEmpty()) {
            return Uni.createFrom().item(switches);
        }
        List<Uni<SwitchData>> perSwitch = switches.stream()
                .map(switchData -> session.fetch(switchData.getNetworks()).replaceWith(switchData))
                .toList();
        return Uni.join().all(perSwitch).usingConcurrencyOf(1).andCollectFailures();
    }

    /**
     * Aplana el árbol del agregado en una lista de entidades {@code *Data} en orden de dependencia
     * de clave foránea: cada fila referida aparece antes que la que la referencia. Es la travesía
     * de escritura, espejo de la de lectura.
     *
     * @param routerData raíz del agregado ya traducida por el mapper
     * @return las entidades a persistir, padre antes que hijo
     */
    private List<Object> flatten(RouterData routerData) {
        List<Object> ordered = new ArrayList<>();
        collect(routerData, ordered);
        return ordered;
    }

    /**
     * Recorre el árbol en profundidad, padre primero, acumulando entidades en {@code ordered}:
     * el router antes que sus switches, cada switch antes que sus redes, y un core antes que sus
     * routers hijos (que a su vez se recorren igual).
     */
    private void collect(RouterData routerData, List<Object> ordered) {
        ordered.add(routerData);
        if (routerData.getSwitches() != null) {
            for (SwitchData switchData : routerData.getSwitches()) {
                ordered.add(switchData);
                if (switchData.getNetworks() != null) {
                    ordered.addAll(switchData.getNetworks());
                }
            }
        }
        if (routerData.getRouters() != null) {
            for (RouterData child : routerData.getRouters()) {
                collect(child, ordered);
            }
        }
    }
}
