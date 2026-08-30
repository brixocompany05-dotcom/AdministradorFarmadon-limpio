package com.app.administradorfarmadon.compartido.datos

import com.google.firebase.firestore.FirebaseFirestore

/**
 * íšnico punto de acceso a Cloud Firestore en toda la app.
 *
 * Por qué existe (regla de negocio, no capricho):
 * - Todas las clases que tocan Firestore obtienen aquí su conexión. Un solo punto
 *   garantiza una sola inicialización y un solo lugar donde razonar el acceso.
 *
 * Caché local:
 * - Viene ACTIVADA DE FÁBRICA en el SDK de Firebase; no se configura nada aquí.
 *   Es lo que permite seguir vendiendo sin señal y sincronizar al volver (R11).
 * - Su higiene vive en SessionManager.limpiarSesion(): al cerrar sesión se borra
 *   la copia del disco para que el siguiente usuario de la tablet no vea reflejos
 *   de datos borrados ni de otra farmacia (R1).
 */
object FarmadonFirestore {
    val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
}
