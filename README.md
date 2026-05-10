# 📈 FinTrack Desktop — Fintech Trading Platform

> A feature-rich JavaFX 21 desktop application for financial trading and management, built with Java 17 and powered by a broad ecosystem of integrations including Stripe, Google Calendar, AI (Vertex AI), blockchain (Web3j), OCR, and more.

---

## 📋 Table of Contents

- [About the Project](#about-the-project)
- [Key Features](#key-features)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Team & Modules](#team--modules)
- [Prerequisites](#prerequisites)
- [Getting Started](#getting-started)
  - [1. Clone the Repository](#1-clone-the-repository)
  - [2. Set Up the Database](#2-set-up-the-database)
  - [3. Configure Environment Variables](#3-configure-environment-variables)
  - [4. Run the Application](#4-run-the-application)
- [Building a Distributable JAR](#building-a-distributable-jar)
- [Branch Strategy](#branch-strategy)
- [Contributing](#contributing)
- [License](#license)

---

## About the Project

**FinTrack Desktop** is a collaborative academic project developed at **ESPRIT** (École Supérieure Privée d'Ingénierie et de Technologies) by team **3A15 – Champions**. It is the desktop counterpart to the team's web-based fintech platform, delivering the same financial trading and management capabilities as a native JavaFX application.

The application connects directly to a **MySQL** database, uses **Maven** for dependency management, and integrates a wide range of third-party services — from payment processing and cloud storage to AI inference and blockchain interaction.

---

## Key Features

- 🖥️ **Rich Desktop UI** — Built with JavaFX 21, ControlsFX, and Ikonli (FontAwesome 5 icons)
- 🔐 **Secure Authentication** — BCrypt password hashing, JWT tokens (Auth0 Java JWT), and Auth0 OAuth2
- 💳 **Payment Processing** — Stripe Java SDK for secure transactions
- ☁️ **Media Management** — Cloudinary for image uploads and storage
- 📄 **PDF Generation & Export** — iText 7 for professional document generation
- 📊 **CSV Data Import/Export** — OpenCSV for data management
- 🔢 **QR Code Generation** — ZXing (Google) for QR code creation and scanning
- 📷 **Webcam Support** — Live webcam capture via Sarxos for identity verification
- 🔍 **OCR / Document Scanning** — Tess4J (Tesseract) for reading identity documents
- 📅 **Google Calendar Integration** — Schedule and sync trading events
- 🤖 **AI Integration** — Google Cloud Vertex AI for intelligent features
- ⛓️ **Blockchain** — Web3j for Ethereum/Web3 interactions
- 📧 **Email Notifications** — Jakarta Mail (Gmail SMTP) for alerts and confirmations
- 📱 **SMS Notifications** — Twilio SDK for real-time SMS alerts
- 🌐 **HTTP Client** — Apache HttpClient for REST API communication
- 📦 **Environment Config** — dotenv-java for secure environment variable management

---

## Tech Stack

| Layer | Technology |
|---|---|
| **Language** | Java 17 |
| **UI Framework** | JavaFX 21.0.2 |
| **UI Components** | ControlsFX 11.1.2, Ikonli + FontAwesome 5 |
| **Build Tool** | Apache Maven 3 |
| **Database** | MySQL 8 (mysql-connector-j 8.3.0) |
| **Authentication** | BCrypt (jBCrypt), Auth0, Java JWT 4.4 |
| **Payments** | Stripe Java SDK 24.0 |
| **Media Storage** | Cloudinary HTTP 1.36 |
| **PDF Generation** | iText 7.2.5 |
| **CSV Processing** | OpenCSV 5.9 |
| **QR Codes** | ZXing 3.5.3 |
| **OCR** | Tess4J 5.10 (Tesseract) |
| **Webcam** | Sarxos Webcam Capture 0.3.12 |
| **AI / ML** | Google Cloud Vertex AI 0.1.0 |
| **Blockchain** | Web3j 4.10.0 |
| **Calendar** | Google Calendar API v3 |
| **Email** | Jakarta Mail 2.0.1 (Gmail SMTP) |
| **SMS** | Twilio SDK 10.1.0 |
| **HTTP Client** | Apache HttpClient 4.5.13 |
| **JSON** | Gson 2.10.1, org.json |
| **Config** | dotenv-java 3.0.0 |

---

## Project Structure

```
.
├── src/
│   └── main/
│       ├── java/
│       │   └── tn/esprit/Champions/
│       │       ├── controllers/     # JavaFX FXML controllers
│       │       ├── models/          # Entity/domain classes
│       │       ├── services/        # Business logic & DB access
│       │       ├── utils/           # Helpers (DB connection, etc.)
│       │       └── test/
│       │           └── Test.java    # Application entry point
│       └── resources/
│           ├── fxml/                # JavaFX layout files (.fxml)
│           ├── css/                 # Stylesheets
│           └── images/              # UI assets
├── certifs/                         # SSL/TLS certificates
├── uploads/                         # Local file upload directory
├── insert_test_user.sql             # Seed SQL for initial test user
├── SOLUTION.md                      # Troubleshooting notes
└── pom.xml                          # Maven project descriptor
```

---

## Team & Modules

This project was built collaboratively, with each team member owning a dedicated feature module:

| # | Module | Key Technologies Used |
|---|--------|-----------------------|
| 1 | **User Management & Auth** | BCrypt, Auth0, JWT, Webcam, Tess4J (OCR) |
| 2 | **Trading & Portfolio** | JavaFX charts, MySQL, Web3j (blockchain) |
| 3 | **Payments & Transactions** | Stripe, iText PDF, OpenCSV |
| 4 | **AI & Smart Features** | Google Vertex AI, ZXing QR codes |
| 5 | **Notifications & Calendar** | Twilio SMS, Jakarta Mail, Google Calendar API |
| 6 | **Dashboard & Media** | Cloudinary, ControlsFX, Ikonli UI components |

---

## Prerequisites

Make sure you have the following installed before running the project:

- [Java JDK 17+](https://adoptium.net/) — required (project targets Java 17)
- [Apache Maven 3.8+](https://maven.apache.org/download.cgi) — for building and dependency management
- [MySQL 8+](https://dev.mysql.com/downloads/) — local or remote database server
- [Tesseract OCR](https://github.com/tesseract-ocr/tesseract) — required by Tess4J for document scanning
  - Windows: install via the [Tesseract installer](https://github.com/UB-Mannheim/tesseract/wiki)
  - Linux: `sudo apt install tesseract-ocr`
  - macOS: `brew install tesseract`
- A webcam (optional, for identity verification features)
- [IntelliJ IDEA](https://www.jetbrains.com/idea/) recommended (`.idea/` config is included)

---

## Getting Started

### 1. Clone the Repository

```bash
git clone https://github.com/Aziz-Khaled/ChampionsPi.git
cd ChampionsPi
```

### 2. Set Up the Database

Create a MySQL database named `fintech` and run the seed file:

```sql
CREATE DATABASE fintech;
USE fintech;
```

Then import the initial user seed:

```bash
mysql -u root -p fintech < insert_test_user.sql
```

> The seed creates a test user with role `FORMATEUR`. Update credentials in `Test.java` if needed.

### 3. Configure Environment Variables

Create a `.env` file in the project root (it is loaded at runtime via dotenv-java):

```env
# Database
DB_URL=jdbc:mysql://localhost:3306/fintech
DB_USER=root
DB_PASSWORD=your_mysql_password

# Auth0
AUTH0_DOMAIN=your-tenant.auth0.com
AUTH0_CLIENT_ID=your_client_id
AUTH0_CLIENT_SECRET=your_client_secret

# JWT
JWT_SECRET=your_jwt_secret

# Stripe
STRIPE_SECRET_KEY=sk_test_...

# Cloudinary
CLOUDINARY_CLOUD_NAME=your_cloud_name
CLOUDINARY_API_KEY=your_api_key
CLOUDINARY_API_SECRET=your_api_secret

# Twilio
TWILIO_ACCOUNT_SID=your_sid
TWILIO_AUTH_TOKEN=your_token
TWILIO_FROM=+1234567890

# Gmail SMTP
MAIL_USER=your_gmail@gmail.com
MAIL_PASSWORD=your_app_password

# Google Cloud / Vertex AI
GOOGLE_CLOUD_PROJECT=your_gcp_project_id

# Tesseract OCR
TESSDATA_PREFIX=/usr/share/tesseract-ocr/4.00/tessdata
```

> For Google Calendar and Vertex AI, place your `credentials.json` service account file in `src/main/resources/` and configure the path in the relevant service class.

### 4. Run the Application

Using Maven and the JavaFX plugin:

```bash
mvn javafx:run
```

Or from IntelliJ IDEA: open the project, let Maven sync dependencies, then run the `Test` class (`tn.esprit.Champions.test.Test`).

---

## Building a Distributable JAR

To package the application as a fat JAR with all dependencies:

```bash
mvn clean package -DskipTests
```

The output will be in `target/fintech-1.0-SNAPSHOT.jar`.

To run it on another machine (Java 17 + JavaFX runtime required):

```bash
java --module-path /path/to/javafx-sdk/lib \
     --add-modules javafx.controls,javafx.fxml,javafx.web \
     -jar target/fintech-1.0-SNAPSHOT.jar
```

> For a fully self-contained installer (no JDK required on target machine), consider packaging with [jpackage](https://docs.oracle.com/en/java/javase/17/docs/specs/man/jpackage.html) to generate a `.exe`, `.dmg`, or `.deb` installer.

---

## Branch Strategy

| Branch | Purpose |
|--------|---------|
| `main` | ✅ Integrated and final version |
| `feature/*` | Individual module branches per team member |

---

## Contributing

This is an academic project. Team workflow:

1. Branch off `main` with a descriptive name (e.g. `feature/payment-module`)
2. Implement your feature and test locally
3. Open a pull request targeting `main`
4. Request a review from at least one teammate before merging

---

## License

This project is proprietary and developed for academic purposes at **ESPRIT – École Supérieure Privée d'Ingénierie et de Technologies**. All rights reserved.

---

<p align="center">
  Made with ❤️ by <strong>Team 3A15 – Champions</strong> · ESPRIT 2024/2025
</p>
