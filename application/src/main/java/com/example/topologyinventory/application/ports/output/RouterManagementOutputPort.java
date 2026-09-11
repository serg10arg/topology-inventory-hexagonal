package com.example.topologyinventory.application.ports.output;

import com.example.topologyinventory.domain.entity.Router;
import com.example.topologyinventory.domain.vo.Id;
import io.smallrye.mutiny.Uni;

/**
 * Puerto de salida para la persistencia de routers. Materializa la inversión de dependencia:
 * la aplicación declara aquí <em>qué</em> necesita (recuperar y persistir un router) y el
 * hexágono de framework aportará el <em>cómo</em> (base de datos concreta) implementando esta
 * interfaz. Solo existe un puerto de salida para el router porque {@link Router} es la raíz del
 * agregado: switches y redes se persisten a través de él, no por separado.
 *
 * <p><b>Reactivo (Fase 8).</b> Ambas operaciones devuelven {@link Uni}: la persistencia pasó a
 * Hibernate Reactive, cuyo API solo expone tipos asíncronos. La {@code Uni} se compone hacia
 * arriba (use case → input port → adapter REST) sin bloquear ningún hilo. Es la primera vez que
 * un contrato de {@code application} depende de una librería reactiva; por eso este módulo
 * {@code requires io.smallrye.mutiny}.
 */
public interface RouterManagementOutputPort {

    /**
     * Recupera un router por su identidad, con su agregado materializado.
     *
     * @param id identidad del router
     * @return una {@link Uni} que emite el {@link Router} recuperado, o {@code null} si no existe
     */
    Uni<Router> retrieveRouter(Id id);

    /**
     * Persiste el router (la raíz del agregado).
     *
     * @param router router a persistir
     * @return una {@link Uni} que emite el {@link Router} persistido
     */
    Uni<Router> persistRouter(Router router);
}
