from dotenv import load_dotenv
import os
import argparse
import time
from helpers.get_token import get_token
import requests
import uuid

BATCH_ENDPOINT = "https://graph.microsoft.com/v1.0/$batch"

def generate_users(domain, count):
    return [{
        "accountEnabled": True,
        "displayName": f"Test User {i}",
        "mailNickname": f"testuser{i:05}",
        "userPrincipalName": f"testuser{i:05}@{domain}",
        "passwordProfile": {
            "forceChangePasswordNextSignIn": True,
            "password": "P@ssw0rd1234!"
        }
    } for i in range(count)]

def batch_create_users(token, users, batch_size=20, retry_delay=5):
    headers = {
        "Authorization": f"Bearer {token}",
        "Content-Type": "application/json"
    }

    for i in range(0, len(users), batch_size):
        batch = users[i:i + batch_size]
        print(f"\n📦 Sending batch {i // batch_size + 1} ({len(batch)} users)...")

        # Prepare batch request
        batch_request = {
            "requests": []
        }

        for j, user in enumerate(batch):
            req_id = str(uuid.uuid4())
            batch_request["requests"].append({
                "id": req_id,
                "method": "POST",
                "url": "/users",
                "headers": {
                    "Content-Type": "application/json"
                },
                "body": user
            })

        # Send batch request
        print(f"Sending request {i}:")
        resp = requests.post(BATCH_ENDPOINT, headers=headers, json=batch_request)
        if resp.status_code != 200:
            print(f"❌ Batch request failed: {resp.status_code} {resp.text}")
            print("Retrying after delay...")
            time.sleep(retry_delay)
            resp = requests.post(BATCH_ENDPOINT, headers=headers, json=batch_request)

        if resp.status_code == 200:
            responses = resp.json().get("responses", [])
            for res in responses:
                user_id = res["id"]
                status = res["status"]
                if 200 <= status < 300:
                    print(f"✅ Created (id {user_id})")
                else:
                    print(f"❌ Failed (id {user_id}): {status}, {res.get('body', {}).get('error', {}).get('message')}")
        else:
            print("⚠️ Skipping this batch due to repeated failure.")


def main():
    # Load environment variables from .env file
    load_dotenv()

    parser = argparse.ArgumentParser()
    parser.add_argument("--tenant-id", required=False, default=os.getenv("ENTRA_TENANT_ID"))
    parser.add_argument("--client-id", required=False, default=os.getenv("ENTRA_CLIENT_ID"))
    parser.add_argument("--client-secret", required=False, default=os.getenv("ENTRA_CLIENT_SECRET"))
    parser.add_argument("--domain", required=False, default="yourdomain.onmicrosoft.com")
    parser.add_argument("--count", type=int, default=10)
    parser.add_argument("--batch-size", type=int, default=20)
    args = parser.parse_args()

    token = get_token(args.tenant_id, args.client_id, args.client_secret)
    users = generate_users(args.domain, args.count)
    batch_create_users(token, users, args.batch_size)

if __name__ == "__main__":
    main()
