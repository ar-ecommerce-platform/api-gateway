# 🚪 API Gateway

**Central entry point** for all requests into the e-commerce backend.  

---

## 🧭 Overview

- Uses **Spring Cloud Gateway** to route to `auth-service`, `user-service`, etc.
- Connects with **Eureka Discovery Server** for dynamic service resolution.
- Acts as the first line for **security, monitoring, and resilience**.


---

## ▶️ Run Locally

```bash
./gradlew bootRun
```

Visit:  
👉 [http://localhost:8080](http://localhost:8080)

---

## 🧪 Example Routes

| Endpoint | Routed To |
|-----------|-----------|
| `/users/**` | User Service |
| `/auth/**` | Auth Service |

---

## 🧰 Related Services

| Service | Port | Purpose |
|----------|------|----------|
| Discovery Server | 8761 | Service registry |
| Config Server | 8888 | Centralized configuration management |
| API Gateway | 8080 | Routes traffic |
| Auth Service | 8081 | Auth / JWT |
| User Service | 8082 | User data |
