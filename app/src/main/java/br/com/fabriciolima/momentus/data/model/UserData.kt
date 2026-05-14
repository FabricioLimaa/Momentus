package br.com.fabriciolima.momentus.data.model

import androidx.annotation.Keep
import com.google.firebase.firestore.PropertyName

/**
 * Representa os dados do perfil de um usuário armazenados no Firestore.
 */
@Keep
data class UserData(
    @get:PropertyName("display_name")
    @set:PropertyName("display_name")
    var displayName: String? = null,
    
    var email: String? = null,
    var points: Int = 0,
    var streak: Int = 0,

    @get:PropertyName("terms_accepted")
    @set:PropertyName("terms_accepted")
    var termsAccepted: Boolean = false,

    @get:PropertyName("terms_accepted_version")
    @set:PropertyName("terms_accepted_version")
    var termsAcceptedVersion: Int = 0
) {
    // Construtor sem argumentos para o Firestore
    constructor() : this(null, null, 0, 0, false, 0)
}
