# SharePlate - Complete MVP

## Backend
Java 21 + Spring Boot 4.1.1 + Spring Web + JPA/Hibernate + PostgreSQL + Security.

1. Create PostgreSQL database `shareplate`.
2. Edit `src/main/resources/application.properties`.
3. Replace `YOUR_POSTGRES_PASSWORD`.
4. Import as an existing Maven project in Eclipse.
5. Run `ShareplateApplication.java` as Spring Boot App.

## Frontend
Open a terminal in `frontend`:
`npm install`
`npm run dev`

Backend: http://localhost:8080
Frontend: http://localhost:5173

## Implemented
User registration with roles, BCrypt password hashing, food listings, NGO claim flow, pickup tasks, one-time handover code, collection/delivery status, PostgreSQL persistence, and a simple React dashboard.

This is a portfolio/MVP build, not a production deployment. JWT login, organization verification, maps, notifications, advanced matching, Flyway migrations, and full audit/reporting can be added next.
