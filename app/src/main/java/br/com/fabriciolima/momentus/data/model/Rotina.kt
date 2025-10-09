package br.com.fabriciolima.momentus.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

/**
 * Representa uma categoria ou "Rotina" à qual um evento pertence.
 * Implementa Serializable para poder ser passado entre Activities via Intent.
 */
@Entity(tableName = "tabela_rotinas")
data class Rotina(
    @PrimaryKey
    val id: String = java.util.UUID.randomUUID().toString(),
    val nome: String,
    val descricao: String?,
    val duracaoPadraoMinutos: Int = 60,
    val cor: String = "#CCCCCC", // Cinza como padrão
    
    // CORREÇÃO: Adicionando a propriedade 'tag' que estava faltando na UI
    val tag: String?

) : Serializable // CORREÇÃO: Garantindo que a classe é serializável
