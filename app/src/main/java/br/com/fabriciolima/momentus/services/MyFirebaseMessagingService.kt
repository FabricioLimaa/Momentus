package br.com.fabriciolima.momentus.services

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import android.util.Log

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // Lidar com mensagens FCM aqui.
        // Nem todas as mensagens virão com uma notificação.
        // Elas também podem conter um payload de dados.
        Log.d("FCM", "De: ${remoteMessage.from}")

        // Verificar se a mensagem contém um payload de dados.
        if (remoteMessage.data.isNotEmpty()) {
            Log.d("FCM", "Payload de dados da mensagem: ${remoteMessage.data}")
            // Processar o payload de dados aqui, por exemplo, atualizar a UI, acionar uma ação.
        }

        // Verificar se a mensagem contém um payload de notificação.
        remoteMessage.notification?.let {
            Log.d("FCM", "Corpo da Notificação da Mensagem: ${it.body}")
            // Exibir a notificação ou lidar com ela conforme necessário.
            // Por exemplo, você pode criar uma notificação local.
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "Token atualizado: $token")

        // Se você deseja enviar mensagens para esta instância do aplicativo ou
        // gerenciar as inscrições deste aplicativo no lado do servidor, envie o
        // token de registro FCM para o servidor do seu aplicativo.
        // Você normalmente enviaria este token para o seu backend aqui.
        sendRegistrationToServer(token)
    }

    private fun sendRegistrationToServer(token: String?) {
        // Implemente este caminho para enviar o token para o servidor do seu aplicativo.
        // Por exemplo, usando uma biblioteca de requisições de rede como Retrofit ou Ktor.
        Log.d("FCM", "Enviando token para o servidor: $token")
    }
}