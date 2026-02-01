# Best Bike Path


## 🛠 Prerequisites

Before proceeding, ensure the following tools are installed on your machine:

- **Java Development Kit**: Version 25 or higher
- **Bun**: Visit [bun.sh](https://bun.sh) for installation
- **PostgreSQL**: Visit [postgresql.org](https://www.postgresql.org) for installation
- **Mapbox API Key**: Visit [mapbox.com](https://www.mapbox.com) to get an API key

---

## ⚙️ Backend

### 1. Build

Navigate to the backend directory:
```bash
cd implementation/backend/bbp
```

Build the executable JAR:
```bash
./mvnw clean package -DskipTests
```

### 2. Run

Run the compiled application:
```bash
java -jar target/bbp-backend.jar
```

> **Note**: The backend server will start on port `8080`.

---

## 💻 Frontend

### 1. Build

Navigate to the frontend directory:
```bash
cd implementation/frontend/bbp
```

Install dependencies and compile the standalone executable:
```bash
bun install
bun run build:prod
```

### 2. Run

Run the generated binary. You must provide your Mapbox API key:
```bash
./bbp-frontend mapbox.api.key=YOUR_KEY
```

#### Optional Configuration

You can specify custom ports if they differ from the defaults (Frontend: 3000, Backend: 8080):
```bash
./bbp-frontend mapbox.api.key=YOUR_KEY --frontend.port=3000 --backend.port=8080
```

### 3. Access

The application will be accessible at:
```
http://localhost:3000
```

---

## 📚 Documentation

To have more in-depth information about this project, you can have a look at the documents that we have drawn up during the development of the project:

- [**Requirement Analysis and Specification Document (RASD)**](https://github.com/m-tteo/sw-eng-2-raimondi-sala/blob/main/DeliveryFolder/RASDv1.pdf)
- [**Design Document (DD)**](https://github.com/m-tteo/sw-eng-2-raimondi-sala/blob/main/DeliveryFolder/DDv2.pdf)
- [**Implementation and Test Deliverable**](https://github.com/m-tteo/sw-eng-2-raimondi-sala/blob/main/DeliveryFolder/ITD.pdf)
---

## 👥 Contributors

- [Filippo Raimondi](https://github.com/filo3r)
- [Matteo Sala](https://github.com/m-tteo)
