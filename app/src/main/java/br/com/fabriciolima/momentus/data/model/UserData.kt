package br.com.fabriciolima.momentus.data.model

import com.google.firebase.firestore.PropertyName

/**
 * Representa os dados do perfil de um usuário armazenados no Firestore.
 */
data class UserData(
    @get:PropertyName("display_name")
    @set:PropertyName("display_name")
    var displayName: String? = null,
    var email: String? = null,
    var points: Int = 0
) {
    // Construtor sem argumentos para o Firestore
    constructor() : this(null, null, 0)
}
