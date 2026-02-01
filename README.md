# Best Bike Path

## 🚀 Quick Start

1. Download both pre-compiled binaries from releases
2. Start the backend:
   ```bash
   java -jar bbp-backend.jar --mapbox.api.key=YOUR_KEY
   ```
3. Start the frontend:

   for linux, before launching the executable run
   ```bash
   chmod +x bbp-frontend
   ```
   ```bash
   ./bbp-frontend --mapbox.api.key=YOUR_KEY
   ```
5. Open [http://localhost:3000](http://localhost:3000) in your browser

---

## Installation

## 🛠 Prerequisites

Before proceeding, ensure the following tools are installed on your machine:

- **Java Development Kit (JDK)**: Version 25 or higher
- **Bun**: Visit [bun.sh](https://bun.sh) for installation
- **PostgreSQL**: Visit [postgresql.org](https://www.postgresql.org) for installation
- **Mapbox API Key**: Visit [mapbox.com](https://www.mapbox.com) to obtain an API key

---

## ⚙️ Backend Setup

### Build from Source

Navigate to the backend directory:

```bash
cd implementation/backend/bbp
```

Build the executable JAR file:

```bash
./mvnw clean package -DskipTests
```

### Download Pre-compiled JAR

Alternatively, download the pre-compiled JAR file from the [releases page](https://github.com/m-tteo/sw-eng-2-raimondi-sala/releases/tag/BestBikePaths-v1.0.0).

### Run the Backend

Execute the application with your Mapbox API key:

```bash
java -jar target/bbp-backend.jar --mapbox.api.key=YOUR_MAPBOX_KEY
```

---

## 💻 Frontend Setup

### Build from Source

Navigate to the frontend directory:

```bash
cd implementation/frontend/bbp
```

Install dependencies:

```bash
bun install
```

Build the production executable:

```bash
bun run build:prod
```

### Download Pre-compiled Executable

Alternatively, download the pre-compiled executable from the [releases page](https://github.com/m-tteo/sw-eng-2-raimondi-sala/releases/tag/BestBikePaths-v1.0.0).

Make the file executable:

```bash
chmod +x bbp-frontend
```

### Run the Frontend

Execute the frontend with your Mapbox API key:

```bash
./bbp-frontend --mapbox.api.key=YOUR_MAPBOX_KEY
```

### Access the Application

Open your browser and navigate to:

```
http://localhost:3000
```

---

## 🔧 Configuration Options

### Backend Parameters

- `--mapbox.api.key`: Your Mapbox API key
- `--server.port`: Backend port (default: 8080)

### Frontend Parameters

- `--mapbox.api.key`: Your Mapbox API key
- `--frontend.port`: Frontend port (default: 3000)
- `--backend.port`: Backend port to connect to (default: 8080)

---


## 📚 Documentation

To have more in-depth information about this project, you can have a look at the documents that we have drawn up during the development of the project:

- [**Requirement Analysis and Specification Document**](https://github.com/m-tteo/sw-eng-2-raimondi-sala/blob/main/DeliveryFolder/RASDv1.pdf)
- [**Design Document**](https://github.com/m-tteo/sw-eng-2-raimondi-sala/blob/main/DeliveryFolder/DDv2.pdf)
- [**Implementation and Test Deliverable**](https://github.com/m-tteo/sw-eng-2-raimondi-sala/blob/main/DeliveryFolder/ITD.pdf)

---

## 👥 Contributors

- [Filippo Raimondi](https://github.com/filo3r)
- [Matteo Sala](https://github.com/m-tteo)
