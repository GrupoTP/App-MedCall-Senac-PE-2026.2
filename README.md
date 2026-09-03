```markdown
# 🏫 App Recife Saudável

**Projeto Integrador — 3º Período | Senac PE**  
**Agosto–Dezembro 2026**  
**Recife, Pernambuco, Brasil**

## 📌 Sobre o Projeto

O **App Recife Saudável** é um aplicativo móvel desenvolvido para facilitar a **pré-triagem** e o **agendamento de consultas** em clínicas parceiras de Recife, reduzindo filas na recepção e melhorando a experiência do paciente. O projeto integra as disciplinas de **User Experience (UX)**, **Mobile**, **Cliente-Server** e **Análise e Projeto de Sistemas**. [cite:web:19][cite:web:22]

## 🎯 Objetivos

- Permitir que pacientes realizem **pré-triagem** via chatbot ou formulário inteligente.
- Facilitar o **agendamento de consultas** em clínicas parceiras.
- Oferecer uma interface **acessível**, com cores calmantes e alta legibilidade, seguindo as normas **WCAG**. [cite:web:17][cite:web:20][cite:web:23]
- Reduzir o tempo de espera nas recepções de clínicas e hospitais.

## 🧩 Funcionalidades

### Pré-Triagem
- **Dados Pessoais**: Nome completo, data de nascimento, telefone (com opção de WhatsApp) e contato de emergência.
- **Queixa Principal**: Motivo da consulta ou sintomas atuais e duração.
- **Histórico Médico**: Doenças anteriores, cirurgias, alergias e tratamentos em curso.
- **Uso de Medicamentos**: Lista de remédios de uso contínuo ou suplementos.

### Agendamento de Consultas
- **Dados do Paciente**: Nome, telefone e e-mail (com possibilidade de edição).
- **Informações da Consulta**: Data, horário, especialidade médica e descrição breve do caso.

### Diferenciais
- Interface com **tons frios e suaves** (azul-gelo, verde-sálvia, lavanda) para acalmar o paciente. [cite:web:19][cite:web:22]
- **Alta legibilidade** e contraste de cores conforme normas **WCAG AA/AAA**. [cite:web:17][cite:web:20][cite:web:23]
- Tela de seleção de perfil (Médico, ADM, Enfermeiro, Paciente).
- **Nome social** editável nas configurações.
- Registro de **data de criação** e **último login** do usuário.
- Sinalização visual clara para **campos obrigatórios**.

## 🛠️ Tecnologias

- **Frontend Mobile**: React Native
- **Backend**: Java (Cliente-Server)
- **Design & Prototipação**: Figma
- **Versionamento**: GitHub
- **Estilo**: HTML, CSS, JavaScript

## 👥 Equipe

- **Thayná Batista da Silva**
- **Poliana Fontes**

**Professor Focal**: Prof. Ícaro Santos Ferreira  
**Instituição**: Faculdade de Análise e Desenvolvimento de Sistemas — Senac PE

## 📂 Estrutura do Repositório

```
├── app/                  # Código-fonte do aplicativo (React Native)
├── server/               # Código do servidor (Java/REST)
├── docs/                 # Documentação (UML, requisitos, fluxos)
├── design/               # Arquivos do Figma e assets
├── README.md             # Este arquivo
└── LICENSE               # Licença do projeto
```

## 🚀 Como Rodar o Projeto

### Pré-requisitos
- Node.js e npm instalados
- React Native CLI configurado
- Java JDK 11+
- Servidor de testes (ex.: Spring Boot ou similar)

### Passos

1. Clone o repositório:
   ```bash
   git clone https://github.com/GrupoTP/app-recife-saudavel.git
   ```

2. Instale as dependências do frontend:
   ```bash
   cd app
   npm install
   ```

3. Configure o servidor backend (consulte `server/README.md`).

4. Execute o aplicativo:
   ```bash
   npx react-native run-android
   # ou
   npx react-native run-ios
   ```

## 📄 Licença

Este projeto está sob a licença MIT. Consulte o arquivo [LICENSE](LICENSE) para mais detalhes.

## 📞 Contato

- **GitHub**: [https://github.com/GrupoTP](https://github.com/GrupoTP)
- **Figma**: [https://www.figma.com/make/HgsxSju4V0c9LixmfcUxm7/App-Recife-Saud%C3%A1vel-Development](https://www.figma.com/make/HgsxSju4V0c9LixmfcUxm7/App-Recife-Saud%C3%A1vel-Development?fullscreen=1&t=qW8P0oS0T6RXnKZX-1&code-node-id=0-6)

---

**Desenvolvido com ❤️ por Thayná Batista da Silva e Poliana Fontes**  
**Senac PE — 2026**
```

***

### Referências

- Normas WCAG para contraste de cores e acessibilidade: [WebAIM](https://www.webaim.org/resources/contrastchecker/), [WCAG 2.2](https://www.w3.org/TR/WCAG22/). 
- Paletas de cores para saúde e bem-estar: [Appy Pie](https://www.appypie.com/blog/mobile-app-color-schemes), [PracXcel](https://pracxcel.com.au/colour-combinations-for-healthcare-websites/).
