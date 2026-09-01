package com.app.administradorfarmadon.inventario.crearproductogeneral.datos
import com.app.administradorfarmadon.compartido.datos.FarmadonFirestore

import android.util.Log
import com.app.administradorfarmadon.autenticacion.login.datos.SessionManager
import com.app.administradorfarmadon.compartido.datos.FarmadonPaths
import com.app.administradorfarmadon.inventario.compartido.logica.CodigoBarraHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Repositorio Enterprise de Creación de Productos en Cloud Firestore.
 * Garantiza aislamiento estricto por cliente, transacciones atómicas y compatibilidad 100% de esquema.
 * Blindaje anti-carrera: el código de barras se verifica DENTRO de la transacción vía índice atómico.
 */
class CrearProductoRepository(
    private val db: FirebaseFirestore = FarmadonFirestore.db,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    companion object {
        private const val TAG = "CrearProductoRepo"
    }

    /**
     * Busca si un código ya existe en la farmacia (feedback rápido fuera de transacción).
     * Fuente única: CodigoBarraHelper.buscarDuplicadoOutside
     */
    suspend fun buscarProductoPorCodigoBarras(clienteId: String, codigoBarras: String, sucursalId: String = ""): Pair<String, String>? {
        val effectiveSucursalId = sucursalId.ifBlank { SessionManager.sucursalIdEfectiva }
        return CodigoBarraHelper.buscarDuplicadoOutside(db, clienteId, codigoBarras, "", effectiveSucursalId)
    }

    suspend fun verificarCodigoBarrasExistente(clienteId: String, codigoBarras: String, sucursalId: String = ""): Boolean {
        return buscarProductoPorCodigoBarras(clienteId, codigoBarras, sucursalId) != null
    }

    /**
     * Valida si ya existe exactamente la MISMA presentación (Mismo nombre + mismo envase + mismo contenido).
     * Permite registrar 'Sprite 500ml' y 'Sprite 1.5L' como dos productos legítimos y separados.
     */
    suspend fun verificarFichaIdenticaExistente(
        clienteId: String,
        nombre: String,
        empaque: String,
        medidaConcentracion: String,
        sucursalId: String = ""
    ): Boolean {
        if (clienteId.isBlank() || nombre.isBlank()) return false
        val effectiveSucursalId = sucursalId.ifBlank { SessionManager.sucursalIdEfectiva }
        val snapshot = FarmadonPaths.inventario(db, clienteId, effectiveSucursalId)
            .whereEqualTo("nombre", nombre.trim())
            .whereEqualTo("empaque", empaque.trim())
            .whereEqualTo("medidaConcentracion", medidaConcentracion.trim())
            .limit(1)
            .get()
            .await()
            
        return !snapshot.isEmpty
    }

    suspend fun guardarProducto(producto: ProductoInventario, sucursalId: String = ""): String {
        // R1: el tenant de la sesión es el clienteId de la farmacia (RUC), NO el uid de auth.
        // El uid de Firebase Auth (auth_xxx) es distinto al RUC por diseño; compararlos
        // como si fueran la misma entidad producía una falsa alarma de "sesión no coincide"
        // y bloqueaba a dueños legítimos al crear productos.
        val clienteIdSesion = SessionManager.clienteIdGarantizado
        if (clienteIdSesion.isBlank()) {
            throw IllegalStateException("No tienes una farmacia asignada. Tu cuenta aún no fue activada por BRIXO o hubo un error de sesión. Contacta a soporte.")
        }
        // El producto debe pertenecer a la farmacia de la sesión. Si viene sin clienteId
        // lo anclamos a la sesión; si viene con uno distinto, es un intento real de
        // escribir en otra farmacia y se bloquea (aislamiento verdadero, no maquillado).
        val clienteId = if (producto.clienteId.isBlank()) clienteIdSesion else producto.clienteId
        if (clienteId != clienteIdSesion) throw SecurityException("Aislamiento entre farmacias: el producto no pertenece a tu farmacia.")
        if (producto.nombre.isBlank()) throw IllegalArgumentException("El nombre del producto es obligatorio.")

        val actorUid = auth.currentUser?.uid.orEmpty()
        val actorEmail = auth.currentUser?.email.orEmpty()
        val effectiveSucursalId = sucursalId.ifBlank { SessionManager.sucursalIdEfectiva }

        val catFinal = producto.categoriaNombre.trim()
        val empFinal = producto.empaque.trim()
        // Blindaje de coherencia: envase + unidad deben pertenecer a la misma familia
        val uniTemp = producto.contenidoUnidad.ifBlank {
            val (_, u) = com.app.administradorfarmadon.inventario.crearproductogeneral.datos.CatalogoEmpaques.separarContenidoYUnidad(producto.medidaConcentracion)
            u
        }
        if (empFinal.isNotBlank() && uniTemp.isNotBlank()) {
            val fam = com.app.administradorfarmadon.inventario.crearproductogeneral.datos.CatalogoEmpaques.detectarFamiliaFisica(empFinal, uniTemp)
            val empOk2 = fam.empaquesCompatibles.any { it.equals(empFinal, ignoreCase = true) }
            val uniOk2 = fam.unidadesCompatibles.any { it.equals(uniTemp, ignoreCase = true) }
            if (!empOk2 || !uniOk2) {
                throw IllegalArgumentException("Envase '$empFinal' no combina con unidad '$uniTemp'. Usa: ${fam.empaquesCompatibles.take(3).joinToString(", ")} para $uniTemp.")
            }
        }
        // Sellado no puede marcarse como fraccionable (evita vender por unidad lo que viene cerrado).
        // Se cubre el vocabulario real del español farmacéutico para evitar huecos.
        val empaquesSellados = setOf("caja", "blíster", "blister", "sobre", "sachet")
        val unidadesSolidas = setOf(
            "tab", "tableta", "tabletas",
            "cáp", "cap", "cápsula", "capsulas", "cápsulas",
            "sob", "sobre", "sobres",
            "píldora", "pildora", "píldoras", "pildoras",
            "comprimido", "comprimidos",
            "gragea", "grageas"
        )
        if (producto.permiteFraccionar && empFinal.isNotBlank() && uniTemp.isNotBlank() && empFinal.lowercase() in empaquesSellados && uniTemp.lowercase() in unidadesSolidas) {
            throw IllegalArgumentException("'$empFinal' es empaque sellado: no puede venderse fraccionado. Usa Granel o Bolsa si necesitas vender por unidad.")
        }
        val labFinal = producto.laboratorio.trim()
        val (cantFromConcentracion, unitFromConcentracion) = CatalogoEmpaques.separarContenidoYUnidad(producto.medidaConcentracion)
        val cantVal = producto.contenido.ifBlank { cantFromConcentracion }
        val unidadVal = producto.contenidoUnidad.ifBlank { unitFromConcentracion }
        val slugVal = producto.nombre.trim().lowercase().replace("\\s+".toRegex(), "-")
        val etiquetasList = listOf(catFinal, empFinal, producto.tipoProducto).filter { it.isNotBlank() }

        // 1. Validar si ya existe exactamente la MISMA presentación (mismo nombre, envase y contenido)
        val duplicadoIdentico = verificarFichaIdenticaExistente(
            clienteId = clienteId,
            nombre = producto.nombre,
            empaque = empFinal,
            medidaConcentracion = producto.medidaConcentracion,
            sucursalId = effectiveSucursalId
        )
        if (duplicadoIdentico) {
            throw IllegalArgumentException("Ya tienes registrado '${producto.nombre}' con la presentación '$empFinal · ${producto.medidaConcentracion}'.")
        }

        // CONTRATO DE NACIMIENTO: categoría, envase, contenido y unidad son obligatorios.
        // Nunca debe nacer un producto con valores vacíos o "N/A" que contaminen otros módulos.
        if (producto.categoriaNombre.trim().isBlank()) {
            throw IllegalArgumentException("Elige la categoría del producto (es obligatoria).")
        }
        if (producto.empaque.trim().isBlank()) {
            throw IllegalArgumentException("Elige el envase (empaque) del producto.")
        }
        if (cantVal.isBlank()) {
            throw IllegalArgumentException("Indica el contenido (cantidad) del producto.")
        }
        if (unidadVal.isBlank()) {
            throw IllegalArgumentException("Elige la unidad de medida del producto.")
        }

        // 2. Validación rápida fuera de transacción (UX) —” el blindaje real está DENTRO de la transacción
        val codLimpioPrevio = CodigoBarraHelper.limpiar(producto.codigoBarras)
        if (codLimpioPrevio.isNotBlank()) {
            val existente = buscarProductoPorCodigoBarras(clienteId, producto.codigoBarras, effectiveSucursalId)
            if (existente != null) {
                throw IllegalArgumentException("El código de barras '${producto.codigoBarras}' ya está asignado a '${existente.second}'.")
            }
        }

        val inventarioRef = FarmadonPaths.inventario(db, clienteId, effectiveSucursalId)

        val nuevoDocRef = inventarioRef.document()
        val productoId = nuevoDocRef.id
        // Código interno: si el usuario no escanea uno, el sistema crea un código único (FMD-…)
        // para que la etiqueta se pueda imprimir y el escáner encuentre el producto.
        val codLimpio = CodigoBarraHelper.limpiar(producto.codigoBarras).ifBlank {
            CodigoBarraHelper.generarCodigoInternoUnico(db, clienteId)
        }
        val esGeneral = producto.tipoProducto.trim().equals("GENERAL", ignoreCase = true)
        val principioActivoFinal = if (esGeneral) "" else producto.principioActivo.trim()
        val requiereRecetaFinal = if (esGeneral) false else producto.requiereReceta
        val esRefrigeradoFinal = if (esGeneral) false else producto.esRefrigerado
        val clasificacionControlFinal = if (esGeneral) "VENTA_LIBRE" else if (requiereRecetaFinal) "CONTROLADO" else "VENTA_LIBRE"
        val tempAlmacenamientoFinal = if (esRefrigeradoFinal) "REFRIGERACION" else "AMBIENTE"

        // Etiqueta buscadora coherente: nombre + contenido + unidad (minúscula, con y sin espacio)
        val nombreBuscado = producto.nombre.trim().lowercase()
        val contenidoBuscado = cantVal.trim().lowercase()
        val unidadBuscada = unidadVal.trim().lowercase()
        val busquedaIndice = listOf(
            nombreBuscado,
            "$contenidoBuscado$unidadBuscada",
            "$contenidoBuscado $unidadBuscada",
            codLimpio.lowercase()
        ).filter { it.isNotBlank() }.joinToString(" ").replace(Regex("\\s+"), " ").trim()
        val busquedaTokens = busquedaIndice.split(Regex("\\s+")).filter { it.isNotBlank() }.distinct()

        // Payload 100% canónico, ordenado y compatible con todos los módulos de inventario, ventas y edición
        val payload = linkedMapOf<String, Any>(
            "id" to productoId,
            "indice" to productoId,
            "presentacionPrincipalId" to productoId,
            "clienteId" to clienteId,
            "farmaciaId" to clienteId,
            "sucursalId" to effectiveSucursalId,
            "nombre" to producto.nombre.trim(),
            "slug" to slugVal,
            "principioActivo" to principioActivoFinal,
            "codigo" to codLimpio,
            "codigoBarras" to codLimpio,
            "tieneCodigoBarra" to codLimpio.isNotBlank(),
            "codigosSecundarios" to emptyList<String>(),
            "tipoProducto" to if (esGeneral) "GENERAL" else "MEDICAMENTO",
            "categoriaId" to producto.categoriaId,
            "categoriaNombre" to catFinal,
            "categoriaPrincipal" to catFinal,
            "categoriasLista" to listOf(catFinal),
            "etiquetas" to etiquetasList,
            "laboratorio" to labFinal,
            "proveedorBaseNombre" to "",
            "empaque" to empFinal,
            "contenido" to cantVal,
            "contenidoUnidad" to unidadVal,
            "concentracion" to producto.medidaConcentracion.trim(),
            "concentracionUnidad" to unidadVal,
            "medidaConcentracion" to producto.medidaConcentracion.trim(),
            "unidadBase" to (if (unidadVal.isNotBlank()) unidadVal else empFinal),
            "unidadVisualInventario" to empFinal,
            "requiereReceta" to requiereRecetaFinal,
            "esRefrigerado" to esRefrigeradoFinal,
            "temperaturaAlmacenamiento" to tempAlmacenamientoFinal,
            "clasificacionControl" to clasificacionControlFinal,
            "sugerenciasEnvase" to CatalogoEmpaques.obtenerEmpaquesCompatibles(empFinal, unidadVal),
            "sugerenciasPerfil" to CatalogoEmpaques.obtenerUnidadesCompatibles(empFinal, unidadVal),
            "ubicacion" to "",
            "stock" to 0.0,
            "stockTotal" to 0.0,
            "stockMinimo" to 0.0,
            "stockMinimoBase" to 0.0,
            "vencimientoMasCercano" to "",
            "diasAlertaVencimiento" to 90,
            "fefoAutomatico" to true,
            "precioCompra" to 0.0,
            "activo" to true,
            "permiteFraccionar" to producto.permiteFraccionar,
            "estado" to "ACTIVO",
            // Presentación base por defecto (nombre comercial = producto.nombre, solo falta precio)
            "presentaciones" to listOf(
                mapOf(
                    "presentacionId" to productoId,
                    "nombre" to producto.nombre.trim(),
                    "empaque" to empFinal,
                    "cantidad" to (cantVal.toIntOrNull() ?: 1).coerceAtLeast(1),
                    "unidadMedida" to unidadVal.ifBlank { empFinal },
                    "codigoBarras" to codLimpio,
                    "precioventa" to 0.0
                )
            ),
            "busquedaIndice" to busquedaIndice,
            "busquedaTokens" to busquedaTokens,
            "creadoEn" to FieldValue.serverTimestamp(),
            "creadoPor" to actorUid,
            "actualizadoEn" to FieldValue.serverTimestamp()
        )

        val claveFicha = CodigoBarraHelper.claveFicha(producto.nombre, empFinal, producto.medidaConcentracion)

        db.runTransaction { tx ->
            // BLINDAJE ATÓMICO ficha idéntica (nombre+empaque+medida) —” evita duplicado fantasma en carrera sin parche
            CodigoBarraHelper.verificarFichaUnicidadEnTransaccion(tx, db, clienteId, claveFicha)

            // BLINDAJE ATÓMICO código de barras vía índice
            if (codLimpio.isNotBlank()) {
                CodigoBarraHelper.verificarUnicidadEnTransaccion(tx, db, clienteId, codLimpio, productoId)
            }

            tx.set(nuevoDocRef, payload)

            // Índices atómicos para futuras verificaciones sin carrera
            CodigoBarraHelper.crearIndiceFichaEnTransaccion(tx, db, clienteId, claveFicha, productoId)
            if (codLimpio.isNotBlank()) {
                CodigoBarraHelper.crearIndiceEnTransaccion(tx, db, clienteId, codLimpio, productoId, producto.nombre.trim())
            }

            val auditLocalRef = FarmadonPaths.auditoria(db, clienteId, effectiveSucursalId).document()

            tx.set(auditLocalRef, hashMapOf(
                "evento" to "CREACION_PRODUCTO",
                "productoId" to productoId,
                "productoNombre" to producto.nombre.trim(),
                "tipoProducto" to producto.tipoProducto,
                "categoriaNombre" to catFinal,
                "actorUid" to actorUid,
                "actorEmail" to actorEmail,
                "fecha" to FieldValue.serverTimestamp()
            ))
        }.await()

        Log.d(TAG, "Producto creado exitosamente con ID: $productoId en cliente: $clienteId sucursal: $effectiveSucursalId")
        return productoId
    }

}
