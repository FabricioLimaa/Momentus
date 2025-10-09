package br.com.fabriciolima.momentus.util

import android.content.Context
import br.com.fabriciolima.momentus.R
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.services.calendar.CalendarScopes

object GoogleAuthUtils {

    fun getGoogleSignInOptions(context: Context): GoogleSignInOptions {
        return GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            // Essencial para autenticação segura com Firebase
            .requestIdToken(context.getString(R.string.default_web_client_id))
            // Pede o email do usuário
            .requestEmail()
            // Pede permissão para ler e escrever na Agenda do Google
            .requestScopes(Scope(CalendarScopes.CALENDAR))
            .build()
    }
}
