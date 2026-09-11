package com.example.topologyinventory.framework.adapters.output.h2;

import com.example.topologyinventory.application.ports.output.RouterManagementOutputPort;
import com.example.topologyinventory.domain.entity.Router;
import com.example.topologyinventory.domain.vo.Id;
import com.example.topologyinventory.framework.adapters.output.h2.data.RouterData;
import com.example.topologyinventory.framework.adapters.output.h2.data.RouterTypeData;
import com.example.topologyinventory.framework.adapters.output.h2.data.SwitchData;
import com.example.topologyinventory.framework.adapters.output.h2.mappers.RouterH2Mapper;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.hibernate.reactive.mutiny.Mutiny;

import java.util.List;

/**
 * Output adapter que implementa {@link RouterManagementOutputPort} con <b>Hibernate Reactive
 * puro</b> (sin Panache, decisión D1) sobre MySQL. Encapsula la tecnología de persistencia y
 * traduce a/desde el dominio con {@link RouterH2Mapper}, de modo que el núcleo nunca ve tipos de
 * base de datos.
 *
 * <p><b>Cableado (CDI).</b> Bean {@code @ApplicationScoped} descubierto por Jandex e inyectado
 * donde {@code RouterManagementInputPort} declara {@link RouterManagementOutputPort}. Al pasar a
 * reactivo, dos pilares de la era bloqueante cambian:
 * <ul>
 *   <li>{@code @Inject EntityManager} → {@code @Inject Mutiny.SessionFactory}: se abre una sesión
 *       reactiva por operación con {@code withSession}/{@code withTransaction}.</li>
 *   <li>{@code @Transactional} → {@code withTransaction(...)}: la transacción se demarca
 *       programáticamente dentro del lambda; desaparece la JTA bloqueante (Narayana).</li>
 * </ul>
 *
 * <p><b>Materialización profunda del agregado (lectura).</b> Bajo Hibernate Reactive las
 * colecciones {@code @OneToMany} no se navegan de forma síncrona: hay que iniciarlas con
 * {@code session.fetch}. Por eso {@code retrieveRouter} no es un {@code find} plano, sino un
 * {@code find} seguido de fetch dependiente del tipo: un CORE materializa sus routers hijos (los
 * ids bastan para la respuesta superficial); un EDGE materializa sus switches y, por cada uno,
 * sus redes (dos niveles), porque el {@code SwitchResponse} serializa las redes completas.
 *
 * <p><b>Escritura sin cascade (SC1).</b> {@code persistRouter} inserta solo la fila raíz: las
 * colecciones {@code @OneToMany} son de solo lectura, así que Hibernate no escribe hijos.
 * Persistir el agregado con hijos (cascade) es la deuda que salda SC2.
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
        return sessionFactory.withTransaction((session, tx) -> session.persist(routerData))
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
}
