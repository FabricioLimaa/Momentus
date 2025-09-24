# Momentus 🚀

Um aplicativo Android para transformar sua rotina ideal em eventos concretos na sua Agenda Google. Agende seus blocos de tempo uma vez e gere seu calendário para semanas ou meses com um único clique.

---

### Sobre o Projeto

O Momentus nasceu da necessidade de automatizar a criação de uma agenda de rotinas diárias e semanais. Em vez de adicionar manualmente os mesmos eventos repetidamente no Google Calendar, este aplicativo permite que o usuário defina "blocos de rotina" (como estudos, trabalho, lazer) e os organize em um cronograma semanal perfeito.

Com a integração da API do Google Calendar, o app lê o cronograma salvo localmente e popula a agenda do usuário para qualquer período de datas, de forma inteligente e evitando a criação de eventos duplicados.

### 📸 Telas (Screenshots)

### ✨ Funcionalidades

* **Gerenciamento de Rotinas:** Crie, edite e delete blocos de rotina personalizados, definindo nome, duração e uma cor visual.
* **Cronograma Semanal Visual:** Organize suas rotinas em uma interface com abas para cada dia da semana.
* **Integração com Google Calendar:** Faça login seguro com sua Conta Google e dê permissão para o app gerenciar sua agenda.
* **Geração Automática de Eventos:** Selecione um período de datas e gere todos os eventos do seu cronograma na sua agenda primária do Google.
* **Prevenção de Duplicatas:** O app verifica sua agenda e não cria eventos que já existem com o mesmo nome e horário.
* **Experiência de Usuário Aprimorada:**
    * Feedback visual com indicadores de carregamento.
    * Gestos de deslizar para deletar com opção de "Desfazer".
    * Interface limpa com "estados vazios" amigáveis.
    * Seletor de cores visual.

### 🛠️ Tecnologias Utilizadas

* **Linguagem:** [Kotlin](https://kotlinlang.org/)
* **Arquitetura:** MVVM (Model-View-ViewModel)
* **UI:** Sistema de Views do Android (XML) com Material Design 3
* **Componentes Principais:**
    * ViewModel e LiveData para gerenciamento de estado reativo.
    * RecyclerView e ViewPager2 para listas e telas deslizáveis.
    * Room Persistence Library para o banco de dados local (SQLite).
    * Coroutines para operações assíncronas e em segundo plano.
* **APIs e Integração:**
    * Google Sign-In for Android para autenticação.
    * Google Calendar API para manipulação de eventos.

### 🚀 Começando

Para executar uma cópia local deste projeto, siga estes passos.

#### Pré-requisitos

* Android Studio (versão mais recente recomendada)
* Uma Conta Google

#### Configuração

1.  **Clone o repositório:**
    ```sh
    git clone [https://github.com/seu-usuario/Momentus.git](https://github.com/seu-usuario/Momentus.git)
    ```
2.  **Abra no Android Studio:**
    * Abra o Android Studio e selecione "Open an existing project".
    * Navegue até a pasta que você acabou de clonar e selecione-a.
    * Aguarde o Gradle sincronizar o projeto.

3.  **Configure as Credenciais da API do Google:**
    * Siga os passos do [Google Cloud Console](https://console.cloud.google.com/apis/credentials) para criar um **ID do cliente OAuth** para Android.
    * Você precisará adicionar a sua chave **SHA-1 de debug** (e de release, se for gerar um APK assinado) nas configurações da credencial para que o login com o Google funcione.

4.  **Rode o Aplicativo:**
    * Clique no botão de Play (▶) para instalar e rodar o app em um emulador ou dispositivo físico.

###  licença

Distribuído sob a licença MIT. Veja `LICENSE.txt` para mais informações.

---

Criado por **Fabricio Lima**