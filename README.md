<div align="center">

# 🍲 SharePlate

**Full-Stack Food Rescue & Surplus Distribution Platform**  
*Connecting donors, NGOs, and volunteers to minimize food waste through coordinated role-based distribution.*

[![Live Demo](https://img.shields.io/badge/🌐_LIVE_DEMO-shareplate.vercel.app-22C55E?style=for-the-badge&logo=vercel&logoColor=white)](https://shareplate-green.vercel.app)
[![Backend](https://img.shields.io/badge/Spring_Boot-3.x-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)](https://spring.io/)
[![Database](https://img.shields.io/badge/PostgreSQL-16+-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Java](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://www.oracle.com/java/)

</div>

---

## 📌 Problem & Overview

Food surplus from commercial establishments and large gatherings frequently ends up in landfills due to logistical delays and a lack of verified coordination channels. 

**SharePlate** provides an end-to-end operational backend and intuitive web dashboard that bridges the gap between donors, charitable organizations, and verified volunteers. It automates food listing lifecycles, ensures secure pickups via single-use handover verification codes, and prevents duplicate claims through transactional database states.

---

## ⚙️ Core Architecture & Features

* **Role-Based Access Control (RBAC):** Distinct business flows and endpoint security tailored for `DONOR`, `NGO`, and `VOLUNTEER` roles.
* **Food Listing & Claims Lifecycle:** Real-time status transitions (`AVAILABLE` → `CLAIMED` → `PICKED_UP` → `DELIVERED`).
* **Secure Handover Verification:** One-time verification token system validating physical food exchange between volunteers and recipients.
* **Security & Auth:** Secure password encryption using **BCrypt** hashing with Spring Security filters.
* **Relational Schema:** Normalized PostgreSQL schema using Spring Data JPA with entity relationship mappings (`@OneToMany`, `@ManyToOne`) and automated timestamps.

---

## 🛠️ Tech Stack

* **Backend:** Java 21, Spring Boot 3.x, Spring Data JPA, Hibernate, Spring Security, BCrypt
* **Database:** PostgreSQL
* **Frontend:** React, Vite, Tailwind CSS / Modern CSS3
* **Build & Tools:** Maven, Git, Postman

---

## 🔌 Key REST API Endpoints

| Method | Endpoint | Access / Role | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/auth/register` | Public | Register new user with designated role |
| `POST` | `/api/auth/login` | Public | Authenticate user credentials |
| `GET` | `/api/listings` | Authenticated | Fetch active food listings |
| `POST` | `/api/listings` | `DONOR` | Create a new food surplus listing |
| `POST` | `/api/listings/{id}/claim` | `NGO` | Claim an active food listing |
| `PATCH`| `/api/tasks/{id}/status` | `VOLUNTEER` | Update pickup/delivery verification state |

---

## 🚀 Getting Started (Local Development)

### 1. Prerequisites
* JDK 21 installed
* PostgreSQL 15+ installed and running locally
* Node.js 18+ and npm

### 2. Database Setup
```sql
CREATE DATABASE shareplate;
