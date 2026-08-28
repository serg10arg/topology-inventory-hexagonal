package com.example.topologyinventory.application.ports.output;

import com.example.topologyinventory.domain.entity.Router;
import com.example.topologyinventory.domain.vo.Id;

/**
 * Puerto de salida para la persistencia de routers. Materializa la inversión de
 * dependencia: la aplicación declara aquí <em>qué</em> necesita (recuperar y
 * persistir un router) y el hexágono de framework aportará el <em>cómo</em>
 * (base de datos concreta) implementando esta interfaz. Solo existe un puerto de
 * salida para el router porque {@link Router} es la raíz del agregado: switches y
 * redes se persisten a través de él, no por separado.
 */
public interface RouterManagementOutputPort {

    /**
     * Recupera un router por su identidad.
     *
     * @param id identidad del router
     * @return el {@link Router} recuperado
     */
    Router retrieveRouter(Id id);

    /**
     * Persiste el router (y, con él, su agregado).
     *
     * @param router router a persistir
     * @return el {@link Router} persistido
     */
    Router persistRouter(Router router);
}