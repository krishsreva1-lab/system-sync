package com.krish.systemsync.vault

import android.content.Context
import com.krish.systemsync.security.CryptographyManager
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class NotesRepository(private val context: Context, private val isDummy: Boolean = false) {
    private val cryptoManager = CryptographyManager()
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val noteAdapter = moshi.adapter(Note::class.java)

    private val notesDir = File(context.filesDir, if (isDummy) "dummy_notes" else "notes").apply { mkdirs() }

    suspend fun saveNote(note: Note) = withContext(Dispatchers.IO) {
        val file = File(notesDir, "${note.id}.note")
        val json = noteAdapter.toJson(note)
        
        FileOutputStream(file).use { output ->
            cryptoManager.encrypt(json.byteInputStream(), output)
        }
    }

    suspend fun getNotes(): List<Note> = withContext(Dispatchers.IO) {
        notesDir.listFiles()?.filter { it.extension == "note" }?.mapNotNull { file ->
            try {
                FileInputStream(file).use { input ->
                    val output = java.io.ByteArrayOutputStream()
                    cryptoManager.decrypt(input, output)
                    val decrypted = output.toString("UTF-8")
                    noteAdapter.fromJson(decrypted)
                }
            } catch (e: Exception) {
                null
            }
        } ?: emptyList()
    }

    suspend fun deleteNote(id: String) = withContext(Dispatchers.IO) {
        File(notesDir, "$id.note").delete()
    }
}
