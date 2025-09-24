// ARQUIVO: data/Rotina.kt
package br.com.fabriciolima.momentus.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable
import java.util.UUID

@Entity(tableName = "tabela_de_rotinas") // Define o nome da tabela
data class Rotina(
    @PrimaryKey // Diz ao Room que o campo 'id' é a chave única
    val id: String = UUID.randomUUID().toString(),
    val nome: String,
    val duracaoPadraoMinutos: Int,
    val cor: String
) : Serializable