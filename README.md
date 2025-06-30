<!-- omit from toc -->
# EntraStuff - Microsoft Graph API PoC 📊
<!-- omit from toc -->
## Table of Contents:
- [Intro](#intro)
  - [Endpoints](#endpoints)
- [Prerequisites](#prerequisites)
- [Setup Infrastructure](#setup-infrastructure)
- [Spin Up Database Locally](#spin-up-database-locally)
- [Run Application](#run-application)
- [Test User Creation and Deletion](#test-user-creation-and-deletion)
- [Test Webhook](#test-webhook)


## Intro
A Proof of Concept (PoC) for integrating with Microsoft Graph API to back up Entra ID user data. This Spring Boot application exposes endpoints to manage user backups and receive real-time updates via webhooks.

### Endpoints
- `POST /api/backup-users`: Backs up all Entra ID users to a local PostgreSQL database.
- `GET /api/backups`: Retrieves info about all stored backups.
- `POST /api/restore-users/{id}`: Restores a given backup, backup chosen by its ID.
- `POST /api/create-subscription`: Registers a webhook for Entra ID user change notifications.
- `POST /api/webhook`: Handles Graph API validation and change notifications.


## Prerequisites
- **Java 24**: Install [JDK 24](https://www.oracle.com/java/technologies/javase/jdk24-archive-downloads.html).
- **Docker**: Install [Docker Desktop](https://www.docker.com/products/docker-desktop/) for PostgreSQL.
- **Terraform**: Install [Terraform](https://www.terraform.io/downloads.html) for Azure setup.
- **ngrok**: Install [ngrok](https://ngrok.com/download) for webhook testing.
- **Python 3.10+**: Install [Python](https://www.python.org/downloads/) for testing scripts.
- **IntelliJ Community Edition**: Developed with [IntelliJ IDEA CE](https://www.jetbrains.com/idea/download/).
- **Azure Account**: Required for Entra ID and Graph API access.
- **curl & jq**: For testing endpoints. Install via `apt install curl jq` (Linux) or equivalent.
- **OpenSSL**: For generating credentials (`openssl rand -base64 12`).

## Setup Infrastructure
1. **Navigate to Terraform Directory**:
   ```bash
   cd terraform
   ```
2. **Log in to Azure**:
   ```bash
   az login
   ```
3. **Run Terraform**:
   ```bash
   terraform init
   terraform plan
   terraform apply
   ```
4. **Save Outputs**:
   - Note `entra_tenant_id`, `entra_client_id` from Terraform output.
   - Run `terraform output -raw client_secret` to get the client secret.
   - Save these for the `.env` file.

5. **Optional: Verify Azure App Registration**:
   - Go to [Azure Portal](https://portal.azure.com) > **Entra ID** > **App registrations**.
   - Find the app with the `entra_client_id` from Terraform.
   - Ensure it has `Directory.Read.All` and `User.Read.All` API permissions with admin consent.

## Spin Up Database Locally
1. **Navigate to Docker Compose Directory**:
   ```bash
   cd docker-compose
   ```
2. **Start PostgreSQL**:
   ```bash
   docker-compose up -d
   ```
3. **Check Logs**:
   ```bash
   docker-compose logs
   ```
   - Ensure PostgreSQL is running (look for "database system is ready to accept connections").
4. **Optional: Connect to Database**:
   - Use [DBeaver](https://dbeaver.io/download/) or similar.
   - Host: `localhost`, Port: `5432`, Username: `postgres`, Password: (see `docker-compose.yml`).

## Run Application
1. **Create `.env` File**:
   - Copy `.env.example` to `.env`:
     ```bash
     cp .env.example .env
     ```
   - Edit `.env` with Terraform outputs and generated credentials:
     ```plaintext
     DB_URL=jdbc:postgresql://localhost:5432/postgres
     DB_USERNAME=postgres
     DB_PASSWORD=<from docker-compose.yml>
     ENTRA_TENANT_ID=<from terraform output>
     ENTRA_CLIENT_ID=<from terraform output>
     ENTRA_CLIENT_SECRET=<from terraform output -raw client_secret>
     AUTH_USERNAME=<run: openssl rand -base64 12>
     AUTH_PASSWORD=<run: openssl rand -base64 16>
     BASE_URL=<add later for webhook testing>
     ```
2. **Start Application**:
   ```bash
   ./gradlew bootRun
   ```
3. **Test Endpoints**:
   ```bash
   curl -vu <AUTH_USERNAME>:<AUTH_PASSWORD> -X POST http://localhost:8080/api/backup-users | jq '.'
   curl -vu <AUTH_USERNAME>:<AUTH_PASSWORD> http://localhost:8080/api/backups | jq '.'
   curl -vu <AUTH_USERNAME>:<AUTH_PASSWORD> -X POST http://localhost:8080/api/restore-users/2 | jq '.'
   ```

## Test User Creation and Deletion
The `./scripts/python` directory contains `create-users.py` and `delete-users.py` to quickly create or delete users in Entra ID for creating many users at once.  
These scripts use the values in the `.env` file for authentication towards the Graph API.

1. **Create Python Virtual Environment**:
   ```bash
   python -m venv .venv
   source .venv/bin/activate  # Linux/Mac
   .venv\Scripts\activate     # Windows
   ```
2. **Install Requirements**:
   ```bash
   cd scripts/python
   pip install -r requirements.txt
   ```
3. **Run Scripts**:
    - Check the scripts for available arguments (e.g., `--help`).
    - Update `--domain` to your Entra ID domain (e.g., `yourcompany.onmicrosoft.com`).
        - Find domain in [Azure Portal](https://portal.azure.com) > **Entra ID** > **Users** > user’s `userPrincipalName`.
    - Example:
      ```bash
      python create-users.py --domain yourcompany.onmicrosoft.com
      python delete-users.py --domain yourcompany.onmicrosoft.com
      ```

You can also find other python scripts in the same folder.  
E.g. for cleaning up subscriptions.


## Test Webhook
To register the webhook endpoint with Azure Entra/the Graph API, the webhook endpoint must be served over https on the public internet.  
`ngrok` is a simple tool for publishing a port on your localhost onto the Internet.

1. **Start ngrok**:
   ```bash
   ngrok http 8080
   ```
   - Copy the ngrok URL (e.g., `c4e5-2a02-1406-243-a916-9dad-f22a-9c0b-bb06.ngrok-free.app`).
2. **Update `.env`**:
   - Add `BASE_URL=<ngrok URL>` (without `https://`).
3. **Restart Application**:
   ```bash
   (ctr+c to stop previous)
   ./gradlew bootRun
   ```
4. **Create Subscription**:
   ```bash
   curl -vu <AUTH_USERNAME>:<AUTH_PASSWORD> -X POST "http://localhost:8080/api/create-subscription" | jq '.'
   ```
   - This registers the webhook endpoint with Graph API to receive user change notifications.
5. **Verify Notifications**:
   - Update a user in Entra ID (e.g., change `displayName` in Azure Portal).
   - Check logs for `Saved audit log for event type: ...`.
   - Query `audit_logs`: `SELECT * FROM audit_logs;`.

Happy coding! 🚀