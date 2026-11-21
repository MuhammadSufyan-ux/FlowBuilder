<p align="center">
  <img src="public/assets/aistudio/image_56fd21fc.png" alt="FlowBuilder Banner" width="100%" style="border-radius: 12px;" />
</p>

<h1 align="center">FlowBuilder</h1>

<p align="center">
  <strong>Offline-first visual flowchart maker & algorithm diagram studio for Android.</strong>
  <br />
  Designed with Jetpack Compose, Material 3, automated DAG layouts, and multi-provider AI generation.
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Kotlin-2.0+-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin" />
  <img src="https://img.shields.io/badge/Jetpack_Compose-Material_3-4285F4?style=for-the-badge&logo=android&logoColor=white" alt="Jetpack Compose" />
  <img src="https://img.shields.io/badge/Android-SDK_24%2B-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Android SDK" />
  <img src="https://img.shields.io/badge/Firebase-AI_%26_Firestore-FFCA28?style=for-the-badge&logo=firebase&logoColor=black" alt="Firebase" />
  <img src="https://img.shields.io/badge/Storage-Room_SQLite-009688?style=for-the-badge" alt="Room DB" />
  <a href="https://lavenderblush-monkey-546500.hostingersite.com/"><img src="https://img.shields.io/badge/Developer-Portfolio-FF5722?style=for-the-badge&logo=google-chrome&logoColor=white" alt="Portfolio" /></a>
</p>

---


## 📸 App Preview

<p align="center">
  <img src="public/assets/aistudio/image_719f5c48.png" alt="FlowBuilder Canvas Preview" width="85%" style="border-radius: 10px; box-shadow: 0 4px 20px rgba(0,0,0,0.15);" />
</p>

<p align="center">
  <img src="public/assets/aistudio/image_6814eda3.png" width="22%" alt="Screen 1" />
  &nbsp;
  <img src="public/assets/aistudio/image_bb7cca4f.png" width="22%" alt="Screen 2" />
  &nbsp;
  <img src="public/assets/aistudio/image_c7492642.png" width="22%" alt="Screen 3" />
  &nbsp;
  <img src="public/assets/aistudio/image_d182d647.png" width="22%" alt="Screen 4" />
</p>

---

## ⚡ Core Highlights

| Feature | Description |
| :--- | :--- |
| 🎨 **Dynamic Vector Canvas** | Infinite pan, smooth pinch-to-zoom (0.2x – 3.0x), reset viewport, and snap-to-grid with dots, lines, or cross grids. |
| 📐 **Smart Connectors** | Orthogonal 90° right-angle routing, smooth Bezier curves, and direct lines with multi-point anchor snapping. |
| 🤖 **Multi-Provider AI** | Built-in Gemini Flash via Firebase AI Logic, plus BYOK (Bring-Your-Own-Key) support for Groq, OpenRouter, and OpenAI. |
| 🔄 **Automatic DAG Layout** | Sugiyama-style hierarchical graph layout (Top-to-Bottom, Left-to-Right, Bottom-to-Top, Right-to-Left). |
| 🛡️ **Real-Time Validator** | Instant semantic diagnostics detecting orphaned nodes, unhandled decision branches, and cyclic traps. |
| 💾 **Offline-First Storage** | 100% local persistence using Room Database + SQLite, with optional cloud backup via Firestore. |
| 📤 **Multi-Format Export** | Render high-res PNGs (Light & Dark theme), print vector PDFs, or export editable JSON graphs. |

---

## 🧩 Comprehensive Shape Library

FlowBuilder includes a rich catalog of standard flow diagram shapes:

* **Core Shapes**: Terminator (Start/End Oval), Process (Rectangle), Decision (Diamond), Data I/O (Parallelogram)
* **Process & Control**: Subroutines, Delay timers, Preparation (Hexagon), Manual Input (Trapezoid)
* **Data & Storage**: Database cylinders, Document, Multi-Document, Internal Storage blocks
* **Connectors & Annotations**: On-page / Off-page circle connectors and freeform text blocks
* **Typography & Palette**: Custom fills, borders, and Google Fonts (*Jakarta Sans, Inter, Outfit, Space Grotesk, JetBrains Mono*)

---

## 🤖 AI Studio Features

FlowBuilder is designed for rapid algorithm prototyping:

1. **Prompt to Flowchart**: Describe an idea in plain English (e.g., *"Design an order refund flow with fraud check"*), and the AI builds the entire graph.
2. **Contextual Diagram Assistant**: Real-time sidekick chat that analyzes current nodes and suggests missing logic.
3. **Step Expansion**: Select any node to automatically elaborate it into sub-steps.
4. **Logic Diagnostics**: Instant natural language explanations for validation warnings and disconnected paths.

---

## 🏗 Tech Stack & Architecture

FlowBuilder is built adhering strictly to Clean Architecture and modern Android development best practices:

```
flowbuilder/
├── app/src/main/java/com/example/
│   ├── data/
│   │   ├── export/         # High-resolution PNG, PDF & JSON exporters
│   │   ├── local/          # Room DB entities, DAOs & EncryptedSharedPreferences
│   │   ├── remote/         # Firestore synchronization, App Check & AI APIs
│   │   └── repository/     # Repositories (FlowAiRepository, FlowProjectRepository)
│   ├── domain/
│   │   ├── engine/         # Geometry math, DAG LayoutEngine, FlowValidator & DSL
│   │   ├── model/          # Node, Edge, Graph, and Template schemas
│   │   └── templates/      # Built-in industry flowchart templates
│   ├── ui/
│   │   ├── auth/           # Onboarding, Splash & Authentication flows
│   │   ├── chat/           # AI diagram assistant chat interface
│   │   ├── components/     # Custom canvas renderers, shapes & path geometry
│   │   ├── editor/         # Canvas gesture view, toolbars & property sheets
│   │   ├── home/           # Project gallery & template selector
│   │   ├── profile/        # Preferences & local API key manager
│   │   └── theme/          # Material 3 styling, color roles & typography
│   └── viewmodel/          # MVI/MVVM UI StateFlow managers
```

### Key Libraries Used

* **UI**: Jetpack Compose BOM, Material 3, Accompanist, Coil
* **Persistence**: Room Database (with KSP compiler), AndroidX DataStore
* **Security**: Android Keystore & EncryptedSharedPreferences for API keys
* **Cloud & Auth**: Firebase Auth, Cloud Firestore, Firebase App Check, Firebase AI Logic
* **Serialization**: Moshi Kotlin Codegen

---

## 🏁 Getting Started

### Prerequisites

* **Android Studio**: Meerkat (2024.3+) or newer
* **JDK**: Version 17+
* **Target Device**: Android 7.0 (API Level 24) or higher

### Setup & Run

1. **Clone the repository**:
   ```bash
   git clone https://github.com/MuhammadSufyan-ux/FlowBuilder.git
   cd FlowBuilder
   ```

2. **Open in Android Studio**:
   - Open Android Studio, click **File > Open**, and select the `FlowBuilder` folder.
   - Allow Gradle to sync the project dependencies automatically.

3. **Configure Environment (Optional)**:
   - For custom backend or AI endpoints, create a `.env` file in the root directory:
     ```bash
     cp .env.example .env
     ```

4. **Launch the App**:
   - Select your target device or emulator and press **Run (`Shift + F10`)**.

---

## 📋 Built-In Templates

FlowBuilder comes with several ready-to-use workflows right out of the box:
* 🔐 **Login & Authentication**: Credential check, password retry loop, and dashboard routing.
* 🧮 **Algorithm Basics**: Two-number comparison and branch decision logic.
* 💳 **ATM Cash Withdrawal**: PIN verification, balance checks, and dispense logic.
* 📦 **E-Commerce Order Flow**: Inventory hold, payment gateway, packing, and dispatch.
* 🔄 **Software Development Life Cycle (SDLC)**: Agile cycle diagram with testing loops.

---

## 👨‍💻 Developer & Access

This application is developed by **Muhammad Sufyan**. All rights reserved.

For project access, inquiries, or collaborations, please visit:
* 🌐 **Portfolio**: [lavenderblush-monkey-546500.hostingersite.com](https://lavenderblush-monkey-546500.hostingersite.com/)