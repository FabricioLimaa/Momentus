package br.com.fabriciolima.momentus.data.model

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

/**
 * Representa uma categoria ou "Rotina" à qual um evento pertence.
 * A anotação @Keep garante que o Proguard não ofuscará esta classe, o que é crucial
 * para a deserialização do Firestore.
 */
@Keep
@Entity(tableName = "tabela_rotinas")
data class Rotina(
    @PrimaryKey
    val id: String = java.util.UUID.randomUUID().toString(),
    val nome: String = "",
    val descricao: String? = null,
    val duracaoPadraoMinutos: Int = 60,
    val cor: String = "#CCCCCC",
    val tag: String? = null
) : Serializable
