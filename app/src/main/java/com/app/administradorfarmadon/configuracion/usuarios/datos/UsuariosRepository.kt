package com.app.administradorfarmadon.configuracion.usuarios.datos
import com.app.administradorfarmadon.compartido.datos.FarmadonFirestore

import android.util.Log
import com.app.administradorfarmadon.compartido.datos.EcosistemaPaths
import com.app.administradorfarmadon.configuracion.sucursales.datos.Sucursal
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

private const val TAG = "UsuariosRepository"

class UsuariosRepository(
    private val db: FirebaseFirestore = FarmadonFirestore.db,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    private fun getSecondaryAuth(): FirebaseAuth {
        val app = try {
            FirebaseApp.getInstance("SecondaryAuthApp")
        } catch (e: Exception) {
            android.util.Log.w("FARMADON_AUTH", "SecondaryAuthApp no existe, usando default: ${e.message}", e)
            val defaultApp = FirebaseApp.getInstance()
            FirebaseApp.initializeApp(
                defaultApp.applicationContext,
                defaultApp.options,
                "SecondaryAuthApp"
            )
        }
        return FirebaseAuth.getInstance(app)
    }

    suspend fun obtenerClienteIdActual(): String {
        // Fuente de verdad: la sesión ya resolvió el tenant (clienteId) en el login
        // y lo guardó en SessionManager. Usarlo evita consultas frágiles que dejarían
        // la pantalla ciega ("No se pudo identificar la farmacia") si el doc global
        // no trae el campo. Solo si la sesión está vacía, se consulta Firestore.
        val sesionClienteId = com.app.administradorfarmadon.autenticacion.login.datos.SessionManager.clienteIdGarantizado
        if (sesionClienteId.isNotBlank()) return sesionClienteId

        val uid = auth.currentUser?.uid ?: return ""
        val userDoc = UsuariosPaths.usuariosGlobal(db).document(uid).get().await()
        return userDoc.getString("clienteId")
            ?: (userDoc.get("clienteIds") as? List<*>)?.firstOrNull()?.toString()
            ?: ""
    }

    fun observarUsuarios(clienteId: String, incluirDadasDeBaja: Boolean = false): Flow<List<UsuarioFarmacia>> = callbackFlow {
        if (clienteId.isBlank()) {
            trySend(emptyList())
            awaitClose {}
            return@callbackFlow
        }

        val subcoleccionRef = UsuariosPaths.usuariosSubcoleccion(db, clienteId)
        val listener = subcoleccionRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                // Verdad, no silencio: el error sube para que la pantalla lo informe
                // (no se muestra "Sin colaboradores" cuando hubo un fallo real de lectura).
                Log.e(TAG, "Error escuchando usuarios subcoleccion: ${error.message}", error)
                close(error)
                return@addSnapshotListener
            }

            val items = snapshot?.documents?.mapNotNull { doc ->
                try {
                    val rolNombre = doc.getString("rolNombre") ?: doc.getString("rol") ?: "Colaborador"
                    if (rolNombre.equals("Dueño", ignoreCase = true) || rolNombre.equals("Dueno", ignoreCase = true)) {
                        return@mapNotNull null
                    }
                    val dadoDeBaja = doc.getBoolean("dadoDeBaja") ?: false
                    if (dadoDeBaja && !incluirDadasDeBaja) {
                        return@mapNotNull null
                    }
                    @Suppress("UNCHECKED_CAST")
                    val permisosModulos = (doc.get("permisosModulos") as? Map<String, Boolean>) ?: emptyMap()
                    UsuarioFarmacia(
                        id = doc.id,
                        clienteId = doc.getString("clienteId") ?: clienteId,
                        nombre = doc.getString("nombre") ?: "",
                        dni = doc.getString("dni") ?: "",
                        telefono = doc.getString("telefono") ?: "",
                        email = doc.getString("email") ?: "",
                        rolId = doc.getString("rolId") ?: "",
                        rolNombre = rolNombre,
                        sucursalId = doc.getString("sucursalId") ?: "todas",
                        sucursalNombre = doc.getString("sucursalNombre") ?: "Todas las Sedes",
                        acceso = doc.getBoolean("acceso") ?: true,
                        estado = doc.getString("estado") ?: if (doc.getBoolean("acceso") == false) "SUSPENDIDO" else "ACTIVO",
                        dadoDeBaja = dadoDeBaja,
                        fechaCreacion = doc.get("fechaCreacion"),
                        actualizadoEn = doc.get("actualizadoEn"),
                        permisosModulos = permisosModulos
                    )
                } catch (e: Exception) {
                    null
                }
            } ?: emptyList()

            trySend(items)
        }

        awaitClose { listener.remove() }
    }

    fun observarRoles(): Flow<List<RolFarmacia>> = callbackFlow {
        val listener = EcosistemaPaths.rolesFarmacia(db)
            .orderBy("orden", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error escuchando roles de farmacia: ${error.message}")
                    trySend(obtenerRolesFallback())
                    return@addSnapshotListener
                }

                val roles = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        val nombre = doc.getString("nombre") ?: ""
                        if (nombre.equals("Dueño", ignoreCase = true) || nombre.equals("Dueno", ignoreCase = true)) {
                            null
                        } else {
                            RolFarmacia(
                                id = doc.id,
                                nombre = nombre,
                                descripcion = doc.getString("descripcion") ?: "",
                                esSistema = doc.getBoolean("esSistema") ?: false,
                                orden = doc.getLong("orden")?.toInt() ?: 0
                            )
                        }
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()

                if (roles.isEmpty()) {
                    trySend(obtenerRolesFallback())
                } else {
                    trySend(roles)
                }
            }

        awaitClose { listener.remove() }
    }

    fun observarSucursales(clienteId: String): Flow<List<Sucursal>> = callbackFlow {
        if (clienteId.isBlank()) {
            trySend(emptyList())
            awaitClose {}
            return@callbackFlow
        }

        val listener = UsuariosPaths.sucursales(db, clienteId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // Verdad, no silencio: el error sube para que la pantalla lo informe.
                    Log.e(TAG, "Error escuchando sucursales para usuarios: ${error.message}", error)
                    close(error)
                    return@addSnapshotListener
                }

                val lista = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        Sucursal(
                            id = doc.id,
                            nombre = doc.getString("nombre") ?: "Sede",
                            direccion = doc.getString("direccion") ?: "",
                            telefono = doc.getString("telefono") ?: "",
                            esPrincipal = doc.getBoolean("esPrincipal") ?: false,
                            activa = doc.getBoolean("activa") ?: true,
                            codigoInterno = doc.getString("codigoInterno") ?: ""
                        )
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()

                trySend(lista)
            }

        awaitClose { listener.remove() }
    }

    suspend fun guardarUsuario(
        clienteId: String,
        usuarioId: String?,
        nombre: String,
        dni: String,
        telefono: String,
        email: String,
        password: String?,
        rolId: String,
        rolNombre: String,
        sucursalId: String,
        sucursalNombre: String,
        acceso: Boolean,
        permisosModulos: Map<String, Boolean> = emptyMap()
    ) {
        val actorUid = auth.currentUser?.uid ?: "anon"
        val actorEmail = auth.currentUser?.email ?: "admin@farmacia"

        val dniLimpio = dni.trim()
        val emailFinal = email.trim().lowercase()
        if (emailFinal.isBlank()) {
            throw IllegalArgumentException("El correo electrónico es obligatorio para el registro y recuperación de acceso.")
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(emailFinal).matches()) {
            throw IllegalArgumentException("El formato del correo electrónico '$emailFinal' no es válido.")
        }

        val esCreacion = usuarioId.isNullOrBlank()
        val finalIdCheck = usuarioId ?: ""

        // 2. Validación de Unicidad de DNI en todo el ecosistema
        val existingDni = UsuariosPaths.usuariosGlobal(db)
            .whereEqualTo("dni", dniLimpio)
            .get()
            .await()
        if (!existingDni.isEmpty && existingDni.documents.any { it.id != finalIdCheck }) {
            throw IllegalArgumentException("El DNI $dniLimpio ya se encuentra registrado con otro colaborador en el sistema.")
        }

        // 3. Validación de Unicidad de Correo Electrónico
        val existingEmail = UsuariosPaths.usuariosGlobal(db)
            .whereEqualTo("email", emailFinal)
            .get()
            .await()
        if (!existingEmail.isEmpty && existingEmail.documents.any { it.id != finalIdCheck }) {
            throw IllegalArgumentException("El correo electrónico $emailFinal ya se encuentra registrado con otro colaborador.")
        }

        // 4. Validación de No Duplicidad de Nombre + Rol en la misma Farmacia
        val existingNameRole = UsuariosPaths.usuariosSubcoleccion(db, clienteId)
            .whereEqualTo("rolId", rolId)
            .get()
            .await()
        if (!existingNameRole.isEmpty && existingNameRole.documents.any {
                it.id != finalIdCheck && it.getString("nombre").equals(nombre.trim(), ignoreCase = true)
            }) {
            throw IllegalArgumentException("Ya existe un colaborador registrado con el nombre '$nombre' y rol '$rolNombre' en esta farmacia.")
        }

        var authUid = usuarioId ?: ""
        var credencialCreada: com.google.firebase.auth.FirebaseUser? = null

        if (esCreacion) {
            if (password.isNullOrBlank()) {
                throw IllegalArgumentException("La contraseña de acceso es requerida para registrar al colaborador.")
            }
            if (password.trim().length < 6) {
                throw IllegalArgumentException("La contraseña debe tener al menos 6 caracteres.")
            }

            val secondaryAuth = getSecondaryAuth()
            try {
                val authResult = secondaryAuth.createUserWithEmailAndPassword(emailFinal, password.trim()).await()
                credencialCreada = authResult.user
                authUid = credencialCreada?.uid ?: throw IllegalStateException("No se pudo generar las credenciales de acceso.")
            } catch (e: Exception) {
                Log.e(TAG, "Error en Firebase Auth para colaborador: ${e.message}", e)
                val msg = if (e.message?.contains("email address is already in use", ignoreCase = true) == true) {
                    "El correo $emailFinal ya se encuentra registrado con otro usuario en el sistema."
                } else {
                    e.message ?: "Error al registrar credenciales de acceso."
                }
                throw Exception(msg)
            }
        }

        val finalId = authUid

        try {
        db.runTransaction { tx ->
            val subcoleccionRef = UsuariosPaths.usuariosSubcoleccion(db, clienteId)
            val globalUsuariosRef = UsuariosPaths.usuariosGlobal(db)

            val docSubRef = subcoleccionRef.document(finalId)
            val docGlobalRef = globalUsuariosRef.document(finalId)

            val snapshotSub = tx.get(docSubRef)
            val datosPrevios = if (snapshotSub.exists()) snapshotSub.data else null

            // Payload para la subcolección del tenant
            val payloadTenant = mutableMapOf<String, Any>(
                "clienteId" to clienteId,
                "farmaciaId" to clienteId,
                "nombre" to nombre.trim(),
                "dni" to dni.trim(),
                "telefono" to telefono.trim(),
                "email" to emailFinal,
                "rolId" to rolId,
                "rolNombre" to rolNombre,
                "sucursalId" to sucursalId,
                "sucursalNombre" to sucursalNombre,
                "acceso" to acceso,
                "estado" to if (acceso) "ACTIVO" else "SUSPENDIDO",
                "dadoDeBaja" to false,
                "actualizadoEn" to FieldValue.serverTimestamp(),
                "permisosModulos" to permisosModulos
            )

            // Payload para el índice global de autenticación
            val payloadGlobal = mutableMapOf<String, Any>(
                "clienteId" to clienteId,
                "nombre" to nombre.trim(),
                "dni" to dni.trim(),
                "telefono" to telefono.trim(),
                "email" to emailFinal,
                "rol" to rolNombre,
                "rolId" to rolId,
                "sucursalId" to sucursalId,
                "sucursalNombre" to sucursalNombre,
                "acceso" to acceso,
                "estado" to if (acceso) "ACTIVO" else "SUSPENDIDO",
                "dadoDeBaja" to false,
                "updatedAt" to FieldValue.serverTimestamp(),
                "permisosModulos" to permisosModulos
            )

            if (esCreacion) {
                payloadTenant["fechaCreacion"] = FieldValue.serverTimestamp()
                payloadTenant["creadoPor"] = actorUid
                payloadGlobal["createdAt"] = FieldValue.serverTimestamp()
                payloadGlobal["creadoPor"] = actorUid

                tx.set(docSubRef, payloadTenant)
                tx.set(docGlobalRef, payloadGlobal)
            } else {
                tx.update(docSubRef, payloadTenant)
                tx.set(docGlobalRef, payloadGlobal, com.google.firebase.firestore.SetOptions.merge())
            }

            // Registro dual de auditoría
            val auditGlobalRef = EcosistemaPaths.auditoriaClientes(db).document()
            val auditLocalRef = UsuariosPaths.auditoriaLocal(db, clienteId).document(auditGlobalRef.id)

            val accionAudit = if (esCreacion) "CREACION_USUARIO" else "EDICION_USUARIO"
            val motivoAudit = if (esCreacion) "Registro de colaborador $nombre ($rolNombre)" else "Modificación de datos de $nombre"

            val payloadAudit = mapOf(
                "seccion" to "personal_usuarios",
                "actorUid" to actorUid,
                "actorEmail" to actorEmail,
                "actorRol" to "ADMINISTRADOR",
                "actorType" to "HUMAN",
                "tenantId" to clienteId,
                "entidad" to "usuario_farmacia",
                "entidadId" to finalId,
                "usuarioNombre" to nombre,
                "rolNombre" to rolNombre,
                "accion" to accionAudit,
                "motivo" to motivoAudit,
                "antes" to (datosPrevios ?: emptyMap<String, Any>()),
                "despues" to payloadTenant,
                "timestamp" to FieldValue.serverTimestamp()
            )

            tx.set(auditGlobalRef, payloadAudit)
            tx.set(auditLocalRef, payloadAudit)
        }.await()
        } catch (e: Exception) {
            // U2 · Rollback atómico del alta de personal (espejo del fix H2):
            // si la ficha no pudo guardarse, la llave recién nacida se destruye
            // para no atrapar el correo del candidato para siempre (Regla R3).
            if (esCreacion && credencialCreada != null) {
                val llaveRecienNacida = credencialCreada
                try {
                    llaveRecienNacida.delete().await()
                    Log.w(TAG, "U2 Rollback de acceso aplicado para $emailFinal tras fallo de guardado")
                } catch (delEx: Exception) {
                    Log.e(TAG, "CRíTICO U2: no se pudo revertir la llave de $emailFinal; queda huérfana y requerirá soporte.", delEx)
                }
            }
            throw e
        }
    }

    suspend fun cambiarEstadoAcceso(
        clienteId: String,
        usuarioId: String,
        nuevoAcceso: Boolean,
        motivo: String
    ) {
        val actorUid = auth.currentUser?.uid ?: "anon"
        val actorEmail = auth.currentUser?.email ?: "admin@farmacia"

        if (usuarioId == actorUid && !nuevoAcceso) {
            throw IllegalArgumentException("Operación inválida: Un usuario no puede suspender su propia cuenta activa.")
        }

        db.runTransaction { tx ->
            val docSubRef = UsuariosPaths.usuariosSubcoleccion(db, clienteId).document(usuarioId)
            val docGlobalRef = UsuariosPaths.usuariosGlobal(db).document(usuarioId)

            val snap = tx.get(docSubRef)
            val nombre = snap.getString("nombre") ?: "Colaborador"
            val estadoStr = if (nuevoAcceso) "ACTIVO" else "SUSPENDIDO"

            tx.update(docSubRef, mapOf(
                "acceso" to nuevoAcceso,
                "estado" to estadoStr,
                "actualizadoEn" to FieldValue.serverTimestamp()
            ))

            tx.set(docGlobalRef, mapOf(
                "acceso" to nuevoAcceso,
                "estado" to estadoStr,
                "updatedAt" to FieldValue.serverTimestamp()
            ), com.google.firebase.firestore.SetOptions.merge())

            // Auditoría
            val auditGlobalRef = EcosistemaPaths.auditoriaClientes(db).document()
            val auditLocalRef = UsuariosPaths.auditoriaLocal(db, clienteId).document(auditGlobalRef.id)

            val payloadAudit = mapOf(
                "seccion" to "personal_usuarios",
                "actorUid" to actorUid,
                "actorEmail" to actorEmail,
                "actorRol" to "ADMINISTRADOR",
                "actorType" to "HUMAN",
                "tenantId" to clienteId,
                "entidad" to "usuario_farmacia",
                "entidadId" to usuarioId,
                "usuarioNombre" to nombre,
                "accion" to if (nuevoAcceso) "ACTIVACION_ACCESO" else "SUSPENSION_ACCESO",
                "motivo" to motivo,
                "timestamp" to FieldValue.serverTimestamp()
            )

            tx.set(auditGlobalRef, payloadAudit)
            tx.set(auditLocalRef, payloadAudit)
        }.await()
    }

    /**
     * Baja de colaborador (U1): NO borra documentos ni la identidad de acceso.
     * Sella la ficha (dadoDeBaja=true, acceso apagado) para que salga de la lista,
     * pierda la sesión en vivo y quede disponible para RECONTRATAR más adelante
     * editando el mismo registro. Fail-closed en login y arranque (acceso=false).
     */
    suspend fun darDeBajaUsuario(
        clienteId: String,
        usuarioId: String,
        nombreUsuario: String,
        motivo: String
    ) {
        val actorUid = auth.currentUser?.uid ?: "anon"
        val actorEmail = auth.currentUser?.email ?: "admin@farmacia"

        if (usuarioId == actorUid) {
            throw IllegalArgumentException("Operación inválida: Un usuario no puede dar de baja su propia cuenta activa.")
        }

        db.runTransaction { tx ->
            val docSubRef = UsuariosPaths.usuariosSubcoleccion(db, clienteId).document(usuarioId)
            val docGlobalRef = UsuariosPaths.usuariosGlobal(db).document(usuarioId)

            val sello = mapOf(
                "dadoDeBaja" to true,
                "acceso" to false,
                "estado" to "DADO_DE_BAJA"
            )

            tx.update(docSubRef, sello + mapOf("actualizadoEn" to FieldValue.serverTimestamp()))
            tx.set(docGlobalRef, sello + mapOf("updatedAt" to FieldValue.serverTimestamp()), com.google.firebase.firestore.SetOptions.merge())

            // Registro dual de auditoría de baja
            val auditGlobalRef = EcosistemaPaths.auditoriaClientes(db).document()
            val auditLocalRef = UsuariosPaths.auditoriaLocal(db, clienteId).document(auditGlobalRef.id)

            val payloadAudit = mapOf(
                "seccion" to "personal_usuarios",
                "actorUid" to actorUid,
                "actorEmail" to actorEmail,
                "actorRol" to "ADMINISTRADOR",
                "actorType" to "HUMAN",
                "tenantId" to clienteId,
                "entidad" to "usuario_farmacia",
                "entidadId" to usuarioId,
                "usuarioNombre" to nombreUsuario,
                "accion" to "BAJA_USUARIO",
                "motivo" to motivo,
                "timestamp" to FieldValue.serverTimestamp()
            )

            tx.set(auditGlobalRef, payloadAudit)
            tx.set(auditLocalRef, payloadAudit)
        }.await()
    }

    suspend fun enviarRestablecimientoPassword(email: String) {
        val emailLimpio = email.trim().lowercase()
        if (emailLimpio.isBlank()) throw IllegalArgumentException("El colaborador no tiene un correo electrónico configurado.")
        auth.sendPasswordResetEmail(emailLimpio).await()
    }

    private fun obtenerRolesFallback(): List<RolFarmacia> {
        return listOf(
            RolFarmacia(id = "cajero", nombre = "Cajero", descripcion = "Atención al cliente y facturación en caja", esSistema = true, orden = 1),
            RolFarmacia(id = "farmaceutico", nombre = "Farmacéutico", descripcion = "Dispensación de medicamentos y gestión técnica", esSistema = true, orden = 2),
            RolFarmacia(id = "administrador", nombre = "Administrador", descripcion = "Control de inventario, compras y personal", esSistema = true, orden = 3)
        )
    }
}
