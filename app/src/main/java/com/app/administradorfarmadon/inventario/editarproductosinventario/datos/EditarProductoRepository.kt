package com.app.administradorfarmadon.inventario.editarproductosinventario.datos
import com.app.administradorfarmadon.compartido.datos.FarmadonFirestore

import android.util.Log
import com.app.administradorfarmadon.autenticacion.login.datos.SessionManager
import com.app.administradorfarmadon.compartido.datos.FarmadonPaths
import com.app.administradorfarmadon.inventario.compartido.logica.CodigoBarraHelper
import com.app.administradorfarmadon.inventario.compartido.logica.ProductoParser
import com.app.administradorfarmadon.inventario.compartido.modelo.MoldeProductos
import com.app.administradorfarmadon.inventario.crearproductogeneral.datos.CatalogoEmpaques
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Repositorio de Edición de Producto en Cloud Firestore (Enterprise SaaS).
 * Garantiza aislamiento estricto por cliente, transacciones atómicas y compatibilidad 100% de esquema.
 * Blindaje anti-carrera vía índice atómico de códigos.
 */
class EditarProductoRepository(
    private val db: FirebaseFirestore = FarmadonFirestore.db,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    companion object {
        private const val TAG = "EditarProductoRepo"
    }

    suspend fun cargarProducto(clienteId: String, productoId: String): Result<MoldeProductos> {
        return try {
            if (clienteId.isBlank() || productoId.isBlank()) {
                return Result.failure(IllegalArgumentException("Identificador de farmacia o producto no válido."))
            }

            val doc = FarmadonPaths.inventario(db, clienteId, SessionManager.sucursalIdEfectiva).document(productoId)
                .get()
                .await()

            if (!doc.exists()) {
                return Result.failure(IllegalStateException("El producto no existe en el inventario de esta farmacia."))
            }

            val producto = ProductoParser.parseToMolde(doc)
                ?: return Result.failure(IllegalStateException("No se pudo leer el producto."))
            Result.success(producto)
        } catch (e: Exception) {
            Log.e(TAG, "Error cargando producto: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Busca si un código ya pertenece a OTRO producto (feedback rápido fuera de transacción).
     */
    suspend fun buscarProductoPorCodigoBarras(clienteId: String, codigoBarras: String, currentProductoId: String): Pair<String, String>? {
        return CodigoBarraHelper.buscarDuplicadoOutside(db, clienteId, codigoBarras, currentProductoId)
    }

    /**
     * Valida si ya existe exactamente la MISMA presentación (Mismo nombre + mismo envase + mismo contenido)
     * en OTRO producto de la farmacia.
     */
    suspend fun verificarFichaIdenticaExistente(
        clienteId: String,
        productoId: String,
        nombre: String,
        empaque: String,
        medidaConcentracion: String
    ): Boolean {
        if (clienteId.isBlank() || nombre.isBlank()) return false
        
        val snapshot = FarmadonPaths.inventario(db, clienteId, SessionManager.sucursalIdEfectiva)
            .whereEqualTo("nombre", nombre.trim())
            .whereEqualTo("empaque", empaque.trim())
            .whereEqualTo("medidaConcentracion", medidaConcentracion.trim())
            .get()
            .await()
            
        return snapshot.documents.any { it.id != productoId }
    }

    suspend fun guardarEdicionProducto(
        clienteId: String,
        productoId: String,
        nombre: String,
        principioActivo: String,
        tipoProducto: String,
        categoriaNombre: String,
        laboratorio: String,
        empaque: String,
        contenido: String = "",
        contenidoUnidad: String = "",
        medidaConcentracion: String,
        codigoBarras: String,
        requiereReceta: Boolean,
        esRefrigerado: Boolean,
        permiteFraccionar: Boolean
    ): Result<Unit> {
        return try {
            if (clienteId.isBlank() || productoId.isBlank()) {
                return Result.failure(IllegalArgumentException("Datos de sesión no válidos."))
            }
            // R1: el tenant de la sesión es el clienteId de la farmacia (RUC), no el uid de auth.
            if (clienteId != SessionManager.clienteIdGarantizado) return Result.failure(SecurityException("Aislamiento entre farmacias: el registro no pertenece a tu farmacia."))
            if (nombre.isBlank()) {
                return Result.failure(IllegalArgumentException("El nombre del producto no puede estar vacío."))
            }

            val catFinal = categoriaNombre.trim()
            val empFinal = empaque.trim()
            val uniTempE = contenidoUnidad.ifBlank {
                val (_, u) = com.app.administradorfarmadon.inventario.crearproductogeneral.datos.CatalogoEmpaques.separarContenidoYUnidad(medidaConcentracion)
                u
            }
            if (empFinal.isNotBlank() && uniTempE.isNotBlank()) {
                val fam2 = com.app.administradorfarmadon.inventario.crearproductogeneral.datos.CatalogoEmpaques.detectarFamiliaFisica(empFinal, uniTempE)
                if (!fam2.empaquesCompatibles.any { it.equals(empFinal, ignoreCase = true) } || !fam2.unidadesCompatibles.any { it.equals(uniTempE, ignoreCase = true) }) {
                    return Result.failure(IllegalArgumentException("Envase '$empFinal' no combina con unidad '$uniTempE'. Usa: ${fam2.empaquesCompatibles.take(3).joinToString(", ")} para $uniTempE."))
                }
            }
            if (permiteFraccionar && empFinal.isNotBlank() && uniTempE.isNotBlank() && empFinal.lowercase() in listOf("caja", "blíster", "blister", "sobre") && uniTempE.lowercase() in listOf("tab", "cáp", "cap", "sob")) {
                return Result.failure(IllegalArgumentException("Caja/Blíster sellado no puede ser fraccionable. Cambia a Bolsa/Granel si necesitas vender por unidad."))
            }
            val labFinal = laboratorio.trim()
            val (cantFromConcentracion, unitFromConcentracion) = CatalogoEmpaques.separarContenidoYUnidad(medidaConcentracion)
            val cantVal = contenido.ifBlank { cantFromConcentracion }
            val unidadVal = contenidoUnidad.ifBlank { unitFromConcentracion }
            val slugVal = nombre.trim().lowercase().replace("\\s+".toRegex(), "-")
            val etiquetasList = listOf(catFinal, empFinal, tipoProducto).filter { it.isNotBlank() }

            // CONTRATO DE EDICIÓN: nunca se guarda un producto sin categoría, envase, contenido o unidad.
            if (categoriaNombre.trim().isBlank()) {
                return Result.failure(IllegalArgumentException("Elige la categoría del producto (es obligatoria)."))
            }
            if (empFinal.isBlank()) {
                return Result.failure(IllegalArgumentException("Elige el envase (empaque) del producto."))
            }
            if (cantVal.isBlank()) {
                return Result.failure(IllegalArgumentException("Indica el contenido (cantidad) del producto."))
            }
            if (unidadVal.isBlank()) {
                return Result.failure(IllegalArgumentException("Elige la unidad de medida del producto."))
            }

            // 1. Validar si ya existe exactamente la MISMA presentación en otro producto
            val duplicadoIdentico = verificarFichaIdenticaExistente(
                clienteId = clienteId,
                productoId = productoId,
                nombre = nombre,
                empaque = empFinal,
                medidaConcentracion = medidaConcentracion
            )
            if (duplicadoIdentico) {
                return Result.failure(IllegalArgumentException("Ya existe otro producto en tu inventario con la presentación '$empFinal · $medidaConcentracion'."))
            }

            val docRef = FarmadonPaths.inventario(db, clienteId, SessionManager.sucursalIdEfectiva).document(productoId)

            // El código es la identidad de la etiqueta: editar jamás deja el producto sin código.
            // Si se borra el campo, se conserva el anterior; si nunca tuvo código, se crea uno único.
            val docPrevio = docRef.get().await()
            val codPrevio = CodigoBarraHelper.limpiar(CodigoBarraHelper.leerCodigo(docPrevio))
            val codFinal = CodigoBarraHelper.limpiar(codigoBarras).ifBlank {
                if (codPrevio.isNotBlank()) codPrevio else CodigoBarraHelper.generarCodigoInternoUnico(db, clienteId)
            }

            // Validación rápida fuera de transacción (UX) —” el blindaje real está dentro
            if (codFinal != codPrevio) {
                val codExistente = buscarProductoPorCodigoBarras(clienteId, codFinal, productoId)
                if (codExistente != null) {
                    return Result.failure(IllegalArgumentException("El código de barras '$codFinal' ya está asignado a '${codExistente.second}'."))
                }
            }

            val actorUid = auth.currentUser?.uid.orEmpty()
            val actorEmail = auth.currentUser?.email.orEmpty()

            db.runTransaction { tx ->
                val snapshot = tx.get(docRef)
                if (!snapshot.exists()) {
                    throw IllegalStateException("El producto fue eliminado o ya no existe.")
                }

                val codAnterior = CodigoBarraHelper.limpiar(CodigoBarraHelper.leerCodigo(snapshot))
                val codigosSecundariosActuales = (snapshot.get("codigosSecundarios") as? List<*>)?.mapNotNull { it?.toString() }?.toMutableList() ?: mutableListOf()
                val codNuevoLimpio = codFinal

                val nombreAnterior = snapshot.getString("nombre") ?: ""
                val empaqueAnterior = snapshot.getString("empaque") ?: ""
                val medidaAnterior = snapshot.getString("medidaConcentracion") ?: snapshot.getString("concentracion") ?: ""
                val claveFichaAnterior = CodigoBarraHelper.claveFicha(nombreAnterior, empaqueAnterior, medidaAnterior)
                val claveFichaNueva = CodigoBarraHelper.claveFicha(nombre.trim(), empFinal, medidaConcentracion.trim())

                // BLINDAJE ATÓMICO: si cambia la ficha o el código, verifica que los nuevos no tengan dueño dentro del candado
                if (claveFichaNueva != claveFichaAnterior) {
                    CodigoBarraHelper.verificarFichaUnicidadEnTransaccion(tx, db, clienteId, claveFichaNueva)
                }
                if (codNuevoLimpio.isNotBlank() && codNuevoLimpio != codAnterior) {
                    CodigoBarraHelper.verificarUnicidadEnTransaccion(tx, db, clienteId, codNuevoLimpio, productoId)
                }

                val esGeneral = tipoProducto.trim().equals("GENERAL", ignoreCase = true)
                val principioActivoFinal = if (esGeneral) "" else principioActivo.trim()
                val requiereRecetaFinal = if (esGeneral) false else requiereReceta
                val esRefrigeradoFinal = if (esGeneral) false else esRefrigerado
                val clasificacionControlFinal = if (esGeneral) "VENTA_LIBRE" else if (requiereRecetaFinal) "CONTROLADO" else "VENTA_LIBRE"
                val temperaturaAlmacenamientoFinal = if (esRefrigeradoFinal) "REFRIGERACION" else "AMBIENTE"

                // Etiqueta buscadora coherente (misma que al nacer)
                val nombreBuscadoEdit = nombre.trim().lowercase()
                val contenidoBuscadoEdit = cantVal.trim().lowercase()
                val unidadBuscadaEdit = unidadVal.trim().lowercase()
                val busquedaIndiceEdit = listOf(
                    nombreBuscadoEdit,
                    "$contenidoBuscadoEdit$unidadBuscadaEdit",
                    "$contenidoBuscadoEdit $unidadBuscadaEdit",
                    codNuevoLimpio.lowercase()
                ).filter { it.isNotBlank() }.joinToString(" ").replace(Regex("\\s+"), " ").trim()
                val busquedaTokensEdit = busquedaIndiceEdit.split(Regex("\\s+")).filter { it.isNotBlank() }.distinct()

                val updates = hashMapOf<String, Any>(
                    "nombre" to nombre.trim(),
                    "slug" to slugVal,
                    "principioActivo" to principioActivoFinal,
                    "tipoProducto" to if (esGeneral) "GENERAL" else "MEDICAMENTO",
                    "categoriaNombre" to catFinal,
                    "categoriaPrincipal" to catFinal,
                    "categoriasLista" to listOf(catFinal),
                    "etiquetas" to etiquetasList,
                    "laboratorio" to labFinal,
                    // R12: jamás inventar un proveedor. Si no hay proveedor real, se guarda
                    // vacío; el vínculo nace solo cuando la mercadería llega de un proveedor.
                    "proveedorBaseNombre" to (snapshot.getString("proveedorBaseNombre") ?: ""),
                    "empaque" to empFinal,
                    "contenido" to cantVal,
                    "contenidoUnidad" to unidadVal,
                    "concentracion" to medidaConcentracion.trim(),
                    "concentracionUnidad" to unidadVal,
                    "medidaConcentracion" to medidaConcentracion.trim(),
                    "unidadBase" to (if (unidadVal.isNotBlank()) unidadVal else empFinal),
                    "unidadVisualInventario" to empFinal,
                    "codigo" to codNuevoLimpio,
                    "codigoBarras" to codNuevoLimpio,
                    "tieneCodigoBarra" to codNuevoLimpio.isNotBlank(),
                    "requiereReceta" to requiereRecetaFinal,
                    "esRefrigerado" to esRefrigeradoFinal,
                    "temperaturaAlmacenamiento" to temperaturaAlmacenamientoFinal,
                    "clasificacionControl" to clasificacionControlFinal,
                    "permiteFraccionar" to permiteFraccionar,
                    "sugerenciasEnvase" to CatalogoEmpaques.obtenerEmpaquesCompatibles(empFinal, unidadVal),
                    "sugerenciasPerfil" to CatalogoEmpaques.obtenerUnidadesCompatibles(empFinal, unidadVal),
                    "busquedaIndice" to busquedaIndiceEdit,
                    "busquedaTokens" to busquedaTokensEdit,
                    "actualizadoEn" to FieldValue.serverTimestamp(),
                    "actualizadoPor" to actorUid
                )

                // Preservar código anterior como alias para que las etiquetas físicas sigan funcionando en caja
                if (codAnterior.isNotBlank() && codAnterior != codNuevoLimpio && !codigosSecundariosActuales.contains(codAnterior)) {
                    codigosSecundariosActuales.add(codAnterior)
                    updates["codigosSecundarios"] = codigosSecundariosActuales
                }

                // Si ya se habían impreso etiquetas físicas de este producto, marcar pendiente de reimpresión
                val yaSeImprimieronEtiquetasAntes = snapshot.get("etiquetaUltimaImpresionEn") != null ||
                        snapshot.getBoolean("etiquetasImpresasPreviamente") == true

                if (codAnterior.isNotBlank() && codNuevoLimpio.isNotBlank() && codAnterior != codNuevoLimpio && yaSeImprimieronEtiquetasAntes) {
                    updates["etiquetaPendienteReimpresion"] = true
                    updates["etiquetaPendienteDetalle"] = "Código actualizado a $codNuevoLimpio"
                }

                // SINCRONIZAR LA PRESENTACIÓN BASE con el nombre/código/envase nuevos.
                // La presentación base es la que coincide con presentacionPrincipalId
                // (o la primera si el producto es antiguo y no tiene la marca). Se conserva
                // su precioventa para no borrar el precio que fijó el usuario en caja.
                // Esto evita que el ticket y el Kardex queden con el nombre/código viejos.
                val principalId = (snapshot.get("presentacionPrincipalId") as? String)
                    ?.takeIf { it.isNotBlank() } ?: productoId
                val presRawList = (snapshot.get("presentaciones") as? List<*>) ?: emptyList<Any>()
                val presentacionesFinal: List<Map<String, Any>> = if (presRawList.isEmpty()) {
                    listOf(
                        mapOf(
                            "presentacionId" to productoId,
                            "nombre" to nombre.trim(),
                            "empaque" to empFinal,
                            "cantidad" to (cantVal.toIntOrNull() ?: 1).coerceAtLeast(1),
                            "unidadMedida" to unidadVal.ifBlank { empFinal },
                            "codigoBarras" to codNuevoLimpio,
                            "precioventa" to 0.0
                        )
                    )
                } else {
                    presRawList.filterIsInstance<Map<*, *>>().map { raw ->
                        @Suppress("UNCHECKED_CAST")
                        val mutable = (raw as Map<String, Any>).toMutableMap()
                        if (mutable["presentacionId"] == principalId) {
                            mutable["nombre"] = nombre.trim()
                            mutable["empaque"] = empFinal
                            mutable["cantidad"] = (cantVal.toIntOrNull() ?: 1).coerceAtLeast(1)
                            mutable["unidadMedida"] = unidadVal.ifBlank { empFinal }
                            mutable["codigoBarras"] = codNuevoLimpio
                        }
                        mutable
                    }
                }
                updates["presentacionPrincipalId"] = principalId
                updates["presentaciones"] = presentacionesFinal

                // Códigos de presentaciones: conservar y mantener índice único, sin huérfanos.
                val presentacionesPreviasEdit = (snapshot.get("presentaciones") as? List<*>) ?: emptyList<Any?>()
                val codigosPreviosPresentaciones = presentacionesPreviasEdit.mapNotNull { item ->
                    (item as? Map<*, *>)?.get("codigoBarras") as? String
                }.map { CodigoBarraHelper.limpiar(it) }.filter { it.isNotBlank() }.toSet()
                val codigosNuevosPresentaciones = presentacionesFinal.mapNotNull { item ->
                    item["codigoBarras"] as? String
                }.map { CodigoBarraHelper.limpiar(it) }.filter { it.isNotBlank() }.toSet()
                val codigosPresentacionesEliminar = codigosPreviosPresentaciones - codigosNuevosPresentaciones

                for (codigo in codigosNuevosPresentaciones) {
                    CodigoBarraHelper.verificarUnicidadEnTransaccion(tx, db, clienteId, codigo, productoId)
                }

                tx.update(docRef, updates)

                codigosPresentacionesEliminar.forEach { codigo ->
                    CodigoBarraHelper.borrarIndiceEnTransaccion(tx, db, clienteId, codigo)
                }
                codigosNuevosPresentaciones.forEach { codigo ->
                    CodigoBarraHelper.crearIndiceEnTransaccion(tx, db, clienteId, codigo, productoId, nombre.trim())
                }

                // Mantener índice atómico de fichas sincronizado
                if (claveFichaNueva != claveFichaAnterior) {
                    if (claveFichaAnterior.isNotBlank()) {
                        CodigoBarraHelper.borrarIndiceFichaEnTransaccion(tx, db, clienteId, claveFichaAnterior)
                    }
                    CodigoBarraHelper.crearIndiceFichaEnTransaccion(tx, db, clienteId, claveFichaNueva, productoId)
                }

                // Mantener índice atómico de códigos sincronizado
                if (codAnterior.isNotBlank() && codAnterior != codNuevoLimpio) {
                    CodigoBarraHelper.borrarIndiceEnTransaccion(tx, db, clienteId, codAnterior)
                }
                if (codNuevoLimpio.isNotBlank() && codNuevoLimpio != codAnterior) {
                    CodigoBarraHelper.crearIndiceEnTransaccion(tx, db, clienteId, codNuevoLimpio, productoId, nombre.trim())
                } else if (codNuevoLimpio.isNotBlank() && codAnterior.isBlank()) {
                    // Producto que no tenía código y ahora sí → crear índice
                    CodigoBarraHelper.crearIndiceEnTransaccion(tx, db, clienteId, codNuevoLimpio, productoId, nombre.trim())
                }

                // Registro atómico de auditoría
                val auditRef = FarmadonPaths.auditoria(db, clienteId, SessionManager.sucursalIdEfectiva).document()

                tx.set(
                    auditRef,
                    hashMapOf(
                        "evento" to "EDICION_PRODUCTO",
                        "productoId" to productoId,
                        "productoNombre" to nombre.trim(),
                        "tipoProducto" to tipoProducto,
                        "categoriaNombre" to catFinal,
                        "actorUid" to actorUid,
                        "actorEmail" to actorEmail,
                        "fecha" to FieldValue.serverTimestamp()
                    )
                )
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error guardando edición de producto: ${e.message}", e)
            Result.failure(e)
        }
    }
}
