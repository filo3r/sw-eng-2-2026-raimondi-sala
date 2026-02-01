# Best Bike Path
## 🛠 Prerequisites

Before proceeding, ensure the following tools are installed on your machine:

- **Java Development Kit (JDK)**: Version 25 or higher
- **Bun**: Visit [bun.sh](https://bun.sh) for installation
- **PostgreSQL**: Visit [postgresql.org](https://www.postgresql.org) for installation
- **Mapbox API Key**: Visit [mapbox.com](https://www.mapbox.com) to obtain an API key

## 🚀 Quick Start

1. Download both pre-compiled binaries from [releases page](https://github.com/m-tteo/sw-eng-2-raimondi-sala/releases/tag/BestBikePaths-v1.0.0).
2. Start the backend:
   ```bash
   java -jar bbp-backend.jar --mapbox.api.key=YOUR_KEY
   ```
3. Start the frontend:

   for Linux and macOS only, before launching the executable run
   ```bash
   chmod +x bbp-frontend-<platform>
   ```
   > Replace <platform> with your specific file suffix (e.g., linux-x64). Windows users can skip this step.
   ```bash
   ./bbp-frontend-<platform> --mapbox.api.key=YOUR_KEY
   ```
   > Replace <platform> with your specific file suffix (e.g., linux-x64).
5. Open [http://localhost:3000](http://localhost:3000) in your browser
   > If you specified a custom port with --frontend.port, use that port instead of 3000.

---

## Build from source

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

Open your browser and navigate to:

```
http://localhost:3000
```
> If you specified a custom port with --frontend.port, use that port instead of 3000.

---

## 🔧 Configuration Options

### Backend Parameters

- `--mapbox.api.key`: Your Mapbox API key (required)
- `--server.port`: Backend port (default: 8080)
- `--client.port`: Frontend port for CORS configuration (default: 3000, must match frontend configuration)

### Frontend Parameters

- `--mapbox.api.key`: Your Mapbox API key
- `--frontend.port`: Frontend port (default: 3000)
- `--backend.port`: Backend port to connect to (default: 8080, must match backend configuration)

---


## 📚 Documentation

To have more in-depth information about this project, you can have a look at the documents that we have drawn up during the development of the project:

- [**Requirement Analysis and Specification Document**](https://github.com/m-tteo/sw-eng-2-raimondi-sala/blob/main/DeliveryFolder/RASDv1.pdf)
- [**Design Document**](https://github.com/m-tteo/sw-eng-2-raimondi-sala/blob/main/DeliveryFolder/DDv3.pdf)
- [**Implementation and Test Deliverable**](https://github.com/m-tteo/sw-eng-2-raimondi-sala/blob/main/DeliveryFolder/ITDv1.pdf)

---

## 👥 Contributors

- [Filippo Raimondi](https://github.com/filo3r)
- [Matteo Sala](https://github.com/m-tteo)
