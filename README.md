# Eden

Eden is a human-made music network where listeners discover artists, follow their work, buy music directly, and join paid artist communities. It serves listeners, artists, bands, DJs, producers, and engineers without forcing each person into a single account type.

## Prerequisites

Install the following before running Eden:

| Area | Version |
| --- | --- |
| Backend | Java 21 (LTS) |
| Frontend | Node.js 20.19+ or 22.12+ |
| Database | Docker Desktop with Docker Compose |

## Start the local database

Eden uses a PostgreSQL container for local development. From the project root, run:

```sh
docker compose up -d
```

Confirm that PostgreSQL is ready:

```sh
docker compose ps
```

The `postgres` service should report a healthy status. Database data is retained in the `eden-postgres-data` Docker volume when the container stops.

Local connection settings are defined in `src/main/resources/application-local.properties`. Local development does not require a `.env` file or access to the hosted database.

## Run Eden

Start the backend and frontend in separate terminals.

### Backend

```sh
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

On Windows PowerShell:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

The backend listens on `http://localhost:8080` by default. To run its tests:

```sh
./mvnw test -Dspring.profiles.active=local
```

On Windows PowerShell:

```powershell
.\mvnw.cmd test "-Dspring.profiles.active=local"
```

### Frontend

```bash
cd frontend
npm ci
npm run dev
```

Vite prints the local URL when it starts, normally `http://localhost:5173`.

## Stop the local database

When you finish developing, run:

```sh
docker compose down
```

This preserves the local database. To permanently delete the local database and start fresh, run `docker compose down --volumes`.

## Hosted database

The deployed application continues to use the hosted Supabase PostgreSQL database. Its password must be supplied through the `DB_PASSWORD` environment variable. Do not activate the `local` profile in the deployed environment.

## Verify your setup

1. Run `docker compose ps` and confirm that PostgreSQL is healthy.
2. Start the backend with the `local` profile and confirm it connects to `jdbc:postgresql://localhost:5432/eden`.
3. Start the frontend and open the local URL shown by Vite.
4. Run `npm run lint` and `npm run build` from `frontend` before opening a pull request.

## Troubleshooting

- **Docker cannot connect:** open Docker Desktop and wait until its engine is running.
- **Port 5432 is already in use:** stop the other PostgreSQL instance or container before starting Eden's database.
- **Local database authentication failed:** confirm the values in `application-local.properties` match those in `compose.yaml`.
- **`Could not resolve placeholder 'DB_PASSWORD'`:** the backend was started without the `local` profile and is trying to use the hosted database configuration.
- **Application port already in use:** stop the existing process using port 8080 or 5173, or set a different port through the relevant Spring Boot or Vite configuration.
