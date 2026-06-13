# FlixMate: Enterprise Movie Ticket Booking Platform

FlixMate is a high-fidelity, production-ready enterprise movie ticket booking platform. It features real-time seat locking, dynamic ticket pricing, automated review sentiment analysis, revenue forecasts, and conversational support AI.

---

## 🛠️ Technology Stack

* **Frontend**: Next.js 15 (App Router), TypeScript, TailwindCSS, Zustand, TanStack Query, Framer Motion, SockJS, STOMP.
* **Backend**: Spring Boot 3, Java 21, Spring Security (JWT), Spring Data JPA, WebSockets STOMP, OpenPDF, ZXing (QR).
* **Database & Caching**: PostgreSQL (Neon/Local), Redis.
* **AI Engine**: Gemini 2.5 Flash API (with mathematical & rule-based collaborative local fallbacks).

---

## 🏗️ Folder Structure

```
FlixMate/
├── docker-compose.yml           # Runs PostgreSQL, Redis, Backend, and Frontend
├── flixmate-backend/
│   ├── src/main/java/           # Spring Boot Java 21 source code
│   ├── src/main/resources/      # application.yml configurations
│   ├── src/test/java/           # JUnit 5 & Mockito test files
│   └── Dockerfile               # Multi-stage JVM runtime builder
└── flixmate-frontend/
    ├── src/app/                 # Next.js App Router (home, book, checkout, ticket)
    ├── src/components/          # UI widgets, Navbar, and AI Chatbot
    ├── src/services/            # Axios API wrappers
    ├── src/store/               # Zustand auth stores
    └── Dockerfile               # Production Next.js builder
```

---

## 🚀 How to Run Locally

### Method 1: Using Docker Compose (Recommended)

Spins up PostgreSQL, Redis, Java backend, and Next.js frontend in containerized environments:

1. Clone the repository and navigate to the project root:
   ```bash
   cd FlixMate
   ```
2. Start the services:
   ```bash
   docker-compose up --build
   ```
3. Access the platform:
   * **Frontend Application**: `http://localhost:3000`
   * **Backend REST API**: `http://localhost:8080`
   * **Swagger API Documentation**: `http://localhost:8080/swagger-ui.html`

---

### Method 2: Running Services Individually

#### Prerequisites:
* **Java**: JDK 21 installed.
* **Node.js**: Node 20.x installed.
* **Docker**: Running PostgreSQL and Redis containers (use the docker-compose to start just database services: `docker compose up postgres redis`).

#### 1. Running the Backend Service
1. Navigate to the backend directory:
   ```bash
   cd flixmate-backend
   ```
2. Build and execute:
   * **On Windows**:
     ```bash
     mvn spring-boot:run
     ```
   * **With Maven Wrapper**:
     ```bash
     ./mvnw spring-boot:run
     ```

*The database seeder will automatically run on startup, creating a default administrator account (`admin@flixmate.com` / `admin123`) and customer account (`user@flixmate.com` / `user123`), populating a movie theater, screens, and booking records for testing.*

#### 2. Running the Frontend Service
1. Navigate to the frontend directory:
   ```bash
   cd flixmate-frontend
   ```
2. Install dependencies:
   ```bash
   npm install --legacy-peer-deps
   ```
3. Run the development server:
   ```bash
   npm run dev
   ```
4. Open `http://localhost:3000` in your web browser.

---

## 🎛️ REST API Documentation

### Authentication (`/api/auth`)
* `POST /api/auth/register` - Create user account (returns JWT).
* `POST /api/auth/login` - Sign in user (returns JWT and profile role).

### Movie Catalog (`/api/movies`)
* `GET /api/movies` - List all movies.
* `GET /api/movies/{id}` - Detailed movie description.

### Showtimes & Seats (`/api/showtimes`)
* `GET /api/showtimes` - List all showtimes with dynamic AI pricing.
* `GET /api/showtimes/movie/{movieId}` - Retrieve session times for a film.
* `GET /api/showtimes/{id}/seats` - Live seat locks map (AVAILABLE, HOLD, BOOKED).

### Bookings & Tickets (`/api/bookings`)
* `POST /api/bookings/hold` - Request temporary 10-minute hold locks on seat IDs (requires JWT).
* `POST /api/bookings/{id}/confirm` - Simulated Stripe checkout processor. Completes booking, marks seat as BOOKED, renders QR code, and sends PDF ticket to user's email.
* `GET /api/bookings/history` - User purchase log history.
* `GET /api/bookings/{id}/pdf` - Streaming endpoint to download ticket receipt.

### AI Endpoints (`/api`)
* `POST /api/chatbot/query` - Contextual chat assistance widget (requires JWT).
* `GET /api/movies/recommend` - Query movie recommendations via Gemini based on preferences.

### Admin Tools (`/api/admin`)
* `GET /api/admin/analytics` - Financial dashboard. Queries sales, calculates occupancy rates, processes sentiments, and outputs Gemini regression forecasting.
* `POST /api/admin/movies/sync?tmdbId={id}` - Syncs film data from TMDB in real-time.
* `POST /api/admin/theaters` - Seeds new theaters and screens with standard seating grids.
