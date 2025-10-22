# Momentus 🚀

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Kotlin Version](https://img.shields.io/badge/Kotlin-1.9.0-blue.svg)](https://kotlinlang.org/)
[![Platform](https://img.shields.io/badge/Platform-Android-green.svg)]()

**Transforme sua rotina ideal em eventos concretos na sua Agenda Google. Agende seus blocos de tempo uma vez e gere seu calendário para semanas ou meses com um único clique.**

<br/>

---

## 📋 Índice

- [Sobre o Projeto](#-sobre-o-projeto)
  - [O Problema](#o-problema)
  - [A Solução](#a-solução)
- [✨ Funcionalidades Principais](#-funcionalidades-principais)
- [📸 Telas](#-telas)
- [🛠️ Tecnologias e Arquitetura](#️-tecnologias-e-arquitetura)
- [🚀 Começando](#-começando)
  - [Pré-requisitos](#pré-requisitos)
  - [Configuração](#configuração)
- [📄 Licença](#-licença)
- [👨‍💻 Autor](#-autor)

---

## 🎯 Sobre o Projeto

O Momentus nasceu para otimizar a gestão de tempo e produtividade, automatizando a criação de uma agenda de rotinas.

### O Problema

Muitas pessoas seguem rotinas diárias ou semanais (estudo, trabalho, exercícios, etc.), mas perdem um tempo precioso adicionando manualmente esses eventos repetitivos em suas agendas digitais, como o Google Calendar.

### A Solução

O **Momentus** resolve isso de forma elegante. Ele funciona como uma ponte inteligente entre sua rotina ideal e sua agenda real:

1.  **Defina sua Rotina:** Crie "blocos de tempo" para suas atividades recorrentes.
2.  **Monte sua Semana Perfeita:** Organize visualmente esses blocos em um cronograma semanal.
3.  **Exporte com um Clique:** O app gera todos os eventos na sua Agenda Google para o período que você desejar, evitando duplicatas.

---

## ✨ Funcionalidades Principais

- **🎨 Gerenciador de Rotinas:** Crie, edite e delete blocos de rotina, personalizando nome, duração e cor.
- **🗓️ Cronograma Visual:** Organize suas rotinas em uma interface intuitiva com abas para cada dia da semana.
- **🔐 Integração Segura com Google:** Faça login com sua Conta Google via OAuth 2.0 para dar permissão ao app de gerenciar *apenas* sua agenda.
- **🤖 Geração Automática de Eventos:** Selecione um período de datas e deixe o app popular sua agenda primária do Google.
- **✔️ Prevenção de Duplicatas:** O app verifica sua agenda de forma inteligente e não cria eventos que já existem com o mesmo nome e horário.
- **⭐ Experiência de Usuário Aprimorada:**
  - Feedback visual com indicadores de carregamento.
  - Gestos de deslizar para deletar com opção de "Desfazer".
  - Interface limpa com "estados vazios" amigáveis.
  - Seletor de cores para fácil personalização.

---

## 📸 Telas

<p align="center">
  <img src="screenshots/02-login.png" width="200" alt="Tela de Login">
  <img src="screenshots/04-calendar.png" width="200" alt="Tela Principal do Calendário">
  <img src="screenshots/01-new-event.png" width="200" alt="Criação de Novo Evento">
  <img src="screenshots/03-categories.png" width="200" alt="Gerenciamento de Categorias">
</p>
<p align="center">
  <img src="screenshots/06-templates.png" width="200" alt="Criação de Templates de Rotina">
  <img src="screenshots/07-navigation-drawer.png" width="200" alt="Menu de Navegação">
  <img src="screenshots/08-widget.png" width="200" alt="Widget na Tela Inicial">
  <img src="screenshots/05-loading.png" width="200" alt="Tela de Carregamento">
</p>

---

## 🛠️ Tecnologias e Arquitetura

Este projeto foi construído utilizando tecnologias modernas e seguindo as boas práticas de desenvolvimento Android.

- **Linguagem:** **[Kotlin](https://kotlinlang.org/)** (100% Kotlin-first)
- **Arquitetura:** **MVVM (Model-View-ViewModel)**
  - Separação clara de responsabilidades, facilitando a manutenção e testes.
- **UI:** **Jetpack Compose** com **Material Design 3**
  - A interface é construída de forma declarativa, resultando em um código mais conciso e poderoso.
- **Componentes Principais:**
  - `ViewModel` e `StateFlow`: Para gerenciamento de estado reativo e ciclo de vida consciente, utilizando o poder das Coroutines.
  - `Hilt`: Para injeção de dependência, simplificando o gerenciamento de dependências e melhorando a testabilidade.
  - `Room Persistence Library`: Para o banco de dados local (SQLite) e persistência de dados offline.
  - `Coroutines`: Para gerenciamento de operações assíncronas em toda a aplicação.
  - `Glance`: Para criação de widgets da tela inicial modernos e reativos.
- **APIs e Integração:**
  - **Firebase Authentication**: Para autenticação segura e simplificada com o Google.
  - **Cloud Firestore**: Para armazenamento de dados na nuvem e sincronização em tempo real entre dispositivos.
  - **Google Calendar API**: Para manipulação de eventos na agenda do usuário.

---

## 🚀 Começando

Para executar uma cópia local deste projeto, siga estes passos.

### Pré-requisitos

- Android Studio (versão mais recente recomendada)
- Uma Conta Google

### Configuração

1.  **Clone o repositório:**
    ```sh
    git clone https://github.com/seu-usuario/Momentus.git
    ```
2.  **Abra no Android Studio:**
    - Abra o Android Studio e selecione `Open`.
    - Navegue até a pasta que você acabou de clonar e selecione-a.
    - Aguarde o Gradle sincronizar o projeto.

3.  **Configure as Credenciais da API do Google:**
    - Siga os passos do [Google Cloud Console](https://console.cloud.google.com/apis/credentials) para criar um **ID do cliente OAuth 2.0** do tipo **Android**.
    - **Importante:** Você precisará adicionar a sua chave **SHA-1 de debug** (e de release, se for gerar um APK assinado) nas configurações da credencial para que o login com o Google funcione.
      - Para obter sua SHA-1 de debug, você pode executar o comando `./gradlew signingReport` no terminal do Android Studio.

4.  **Rode o Aplicativo:**
    - Clique no botão de **Play (▶)** para instalar e rodar o app em um emulador ou dispositivo físico.

---

## 📄 Licença

Distribuído sob a licença MIT. Veja `LICENSE.txt` para mais informações.

---

## 👨‍💻 Autor

**Fabricio Lima**

- [LinkedIn](https://www.linkedin.com/in/fabricio-lima-s-s/)
- [GitHub](https://github.com/fabriciolimac)
