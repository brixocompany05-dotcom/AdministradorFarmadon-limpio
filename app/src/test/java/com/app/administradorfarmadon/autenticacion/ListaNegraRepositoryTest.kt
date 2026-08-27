package com.app.administradorfarmadon.autenticacion

import com.app.administradorfarmadon.autenticacion.login.datos.ListaNegraRepository
import com.app.administradorfarmadon.autenticacion.login.datos.ErrorVerificacionListaNegra
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ListaNegraRepositoryTest {

    @Test
    fun `consultar devuelve doc si email esta baneado verificando hash sha256`() = runBlocking {
        val db = mockk<FirebaseFirestore>()
        val col = mockk<CollectionReference>()
        val docRef = mockk<DocumentReference>()
        val snapshot = mockk<DocumentSnapshot>()
        
        val email = "baneado@test.com"
        // El hash real producido por sha256() es el que debe coincidir.
        // SHA-256 de "baneado@test.com" (ya verificado en el error log anterior)
        val expectedHash = "fe70a7498bfcb0ba26c628bc7a3c574ebb39df5941e5fa7497463f1880f7573b"

        every { db.collection("brixo").document("clientes_restringidos").collection("lista_negra") } returns col
        every { col.document(any()) } returns docRef
        
        every { docRef.get(Source.SERVER) } returns Tasks.forResult(snapshot)
        every { snapshot.exists() } returns true
        
        val result = ListaNegraRepository.consultar(db, "", email)
        
        assertEquals(snapshot, result)
        // Verificamos que se llamó con el hash correcto generado por la función de extensión
        verify { col.document(expectedHash) }
    }

    @Test
    fun `consultar devuelve doc si ruc esta baneado`() = runBlocking {
        val db = mockk<FirebaseFirestore>()
        val col = mockk<CollectionReference>()
        val docRefEmail = mockk<DocumentReference>()
        val docRefRuc = mockk<DocumentReference>()
        val snapshotEmail = mockk<DocumentSnapshot>()
        val snapshotRuc = mockk<DocumentSnapshot>()
        
        val email = "normal@test.com"
        val ruc = "20123456789"

        every { db.collection("brixo").document("clientes_restringidos").collection("lista_negra") } returns col
        every { col.document(any()) } returns docRefEmail
        every { docRefEmail.get(Source.SERVER) } returns Tasks.forResult(snapshotEmail)
        every { snapshotEmail.exists() } returns false
        
        every { col.document(ruc) } returns docRefRuc
        every { docRefRuc.get(Source.SERVER) } returns Tasks.forResult(snapshotRuc)
        every { snapshotRuc.exists() } returns true
        
        val result = ListaNegraRepository.consultar(db, ruc, email)
        assertEquals(snapshotRuc, result)
    }

    @Test
    fun `consultar devuelve null si nada esta baneado`() = runBlocking {
        val db = mockk<FirebaseFirestore>()
        val col = mockk<CollectionReference>()
        val docRef = mockk<DocumentReference>()
        val snapshot = mockk<DocumentSnapshot>()

        every { db.collection("brixo").document("clientes_restringidos").collection("lista_negra") } returns col
        every { col.document(any()) } returns docRef
        every { docRef.get(Source.SERVER) } returns Tasks.forResult(snapshot)
        every { snapshot.exists() } returns false
        
        val result = ListaNegraRepository.consultar(db, "ruc", "email@test.com")
        assertNull(result)
    }

    @Test(expected = ErrorVerificacionListaNegra::class)
    fun `consultar relanza excepcion como ErrorVerificacionListaNegra`() = runBlocking {
        val db = mockk<FirebaseFirestore>()
        val col = mockk<CollectionReference>()
        val docRef = mockk<DocumentReference>()

        every { db.collection("brixo").document("clientes_restringidos").collection("lista_negra") } returns col
        every { col.document(any()) } returns docRef
        every { docRef.get(Source.SERVER) } throws Exception("Red fallida")
        
        ListaNegraRepository.consultar(db, "ruc", "email")
        Unit
    }
}
