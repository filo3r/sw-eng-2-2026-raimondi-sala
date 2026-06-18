# 🚲 Best Bike Paths

**Best Bike Paths (BBP)** is a software system developed as part of the **Software Engineering 2** course (A.Y. 2025-2026) at Politecnico di Milano. 
It supports cyclists in recording and browsing an inventory of bike paths.

Registered users can record their trips and review statistics like distance, average speed, and other metrics, optionally enriched with weather data. 
They can also contribute publishable information about bike paths, including status and obstacles such as potholes. 
Anyone, registered or not, can pick an origin and destination and see the available bike paths on a map, ranked by score.

This repository contains the full deliverable: the source code of the prototype together with the project documentation produced during development.

---

## 📋 Prerequisites

Before proceeding, make sure you have the following available:

- **Java Development Kit**: Version 25 or higher
- **Bun**: Visit [bun.sh](https://bun.sh) for installation
- **Mapbox API Key**: Visit [mapbox.com](https://www.mapbox.com) to obtain an API key

---

## 🚀 Quick Start

1. Download both pre-compiled binaries from the [releases page](https://github.com/filo3r/sw-eng-2-2026-raimondi-sala/releases/tag/BestBikePaths-v1.0.0).
2. Start the backend:
   ```bash
   java -jar bbp-backend.jar --mapbox.api.key=YOUR_MAPBOX_KEY
   ```
3. Start the frontend:

   On Linux and macOS only, make the executable runnable first (Windows users can skip this):
   ```bash
   chmod +x bbp-frontend-<platform>
   ```
   Then launch it:
   ```bash
   ./bbp-frontend-<platform> --mapbox.api.key=YOUR_MAPBOX_KEY
   ```
   > Replace `<platform>` with your specific file suffix (e.g., `linux-x64`).
4. Open [http://localhost:3000](http://localhost:3000) in your browser
   > If you specified a custom port with --frontend.port, use that port instead of 3000.

---

## 🏗️ Build from Source

### ⚙️ Backend 

Navigate to the backend directory:

```bash
cd implementation/backend/bbp
```

Build the executable JAR file:

```bash
./mvnw clean package -DskipTests
```
#### Run the Backend

Execute the application with your Mapbox API key:

```bash
java -jar target/bbp-backend.jar --mapbox.api.key=YOUR_MAPBOX_KEY
```

---

### 💻 Frontend

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

#### Run the Frontend

Execute the frontend with your Mapbox API key:

```bash
./bbp-frontend --mapbox.api.key=YOUR_MAPBOX_KEY
```

#### Access the Application

Open [http://localhost:3000](http://localhost:3000) in your browser
> If you specified a custom port with --frontend.port, use that port instead of 3000.

---

## 🎛️ Configuration Options

### Backend Parameters

- `--mapbox.api.key`: Your Mapbox API key (required)
- `--server.port`: Backend port (default: 8080)
- `--client.port`: Frontend port for CORS configuration (default: 3000, must match frontend configuration)

### Frontend Parameters

- `--mapbox.api.key`: Your Mapbox API key (required)
- `--frontend.port`: Frontend port (default: 3000)
- `--backend.port`: Backend port to connect to (default: 8080, must match backend configuration)

---

## 📚 Documentation

This project was developed starting from the official assignment specifications provided for the course:

- [**R&DD Assignment**](https://github.com/filo3r/sw-eng-2-2026-raimondi-sala/blob/main/specification/Assignment-RDD-AY2025-2026.pdf)
- [**I&T Assignment**](https://github.com/filo3r/sw-eng-2-2026-raimondi-sala/blob/main/specification/Assignment-IT-AY2025-2026.pdf)

For more in-depth information about the system, you can also look at the documents we produced during development:

- [**Requirement Analysis and Specification Document**](https://github.com/filo3r/sw-eng-2-2026-raimondi-sala/blob/main/DeliveryFolder/RASDv1.pdf)
- [**Design Document**](https://github.com/filo3r/sw-eng-2-2026-raimondi-sala/blob/main/DeliveryFolder/DDv3.pdf)
- [**Implementation and Test Deliverable**](https://github.com/filo3r/sw-eng-2-2026-raimondi-sala/blob/main/DeliveryFolder/ITDv1.pdf)

---

## 👥 Contributors

- [Filippo Raimondi](https://github.com/filo3r)
- [Matteo Sala](https://github.com/m-tteo)
