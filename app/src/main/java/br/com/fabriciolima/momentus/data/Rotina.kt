// ARQUIVO: data/Rotina.kt (CÓDIGO COMPLETO)

package br.com.fabriciolima.momentus.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable
import java.util.UUID

@Entity(tableName = "tabela_de_rotinas")
data class Rotina(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val nome: String,
    val duracaoPadraoMinutos: Int,
    val cor: String,
    // --- MODIFICAÇÃO INICIA AQUI ---
    // Adicionamos os novos campos que aparecerão na lista.
    // Eles podem ser nulos para manter a compatibilidade com as rotinas que já existem.
    val descricao: String?,
    val tag: String?
    // --- MODIFICAÇÃO TERMINA AQUI ---
) : Serializable