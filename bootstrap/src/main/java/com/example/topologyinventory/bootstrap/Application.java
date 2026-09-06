package com.example.topologyinventory.bootstrap;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.annotations.QuarkusMain;

/**
 * Punto de arranque de la aplicación bajo Quarkus (raíz de composición).
 *
 * <p>Anotada con {@link QuarkusMain}, es el punto de entrada real. En <em>server mode</em>,
 * {@link #main(String...)} delega en {@link Quarkus#run(String...)}: Quarkus arranca el
 * contenedor —Arc, el datasource Agroal, Hibernate ORM, el seed y el servidor HTTP de
 * RESTEasy Reactive— y <b>bloquea sirviendo</b> hasta recibir la señal de parada. El proceso
 * ya no termina por su cuenta: la aplicación pasa de ser un programa que corre y acaba a un
 * servicio que se queda arriba atendiendo peticiones.
 *
 * <p>Esta clase no es un bean ni orquesta ningún flujo: Arc cablea el grafo completo por sí
 * mismo —incluida la cadena driving REST (adapter → caso de uso → puerto de salida →
 * output adapter H2)— para atender cada request. Su única responsabilidad es lanzar el
 * contenedor; toda la resolución por CDI ocurre bajo demanda, en cada petición.
 *
 * <p><b>Desviación respecto a fases previas.</b> Hasta la fase anterior esta clase corría en
 * <em>command mode</em> (implementaba {@code QuarkusApplication}, recibía un adapter por
 * {@code @Inject} y ejecutaba un flujo demo que terminaba con código 0). Al introducir los
 * input adapters REST, ese {@code return} apagaba el servidor HTTP recién levantado; el paso a
 * server mode retira el demo (cubierto ahora por los tests REST) para que el proceso persista.
 *
 * <p>La base H2 es en memoria; con {@code DB_CLOSE_DELAY=-1} vive mientras viva la JVM. Ahora
 * que la JVM no termina, lo que una petición persiste sigue disponible para las siguientes
 * dentro del mismo arranque.
 */
@QuarkusMain
public class Application {

    public static void main(String... args) {
        Quarkus.run(args);
    }
}
