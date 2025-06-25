from dotenv import load_dotenv
import os
import argparse
import requests
import time
import uuid
from helpers.get_token import get_token

GRAPH_ENDPOINT = "https://graph.microsoft.com/v1.0"
BATCH_ENDPOINT = f"{GRAPH_ENDPOINT}/$batch"
MAX_BATCH_SIZE = 20

def get_test_users(token, prefix="testuser"):
    """Fetches all users with userPrincipalName starting with 'testuser'."""
    headers = {
        "Authorization": f"Bearer {token}"
    }

    url = f"{GRAPH_ENDPOINT}/users?$filter=startswith(userPrincipalName,'{prefix}')&$top=999"
    users = []

    while url:
        resp = requests.get(url, headers=headers)
        if resp.status_code != 200:
            raise Exception(f"Failed to fetch users: {resp.status_code} {resp.text}")
        data = resp.json()
        users.extend(data.get("value", []))
        url = data.get("@odata.nextLink")  # for paging

    print(f"🔍 Found {len(users)} users starting with '{prefix}'")
    return users

def batch_delete_users(token, users, batch_size=MAX_BATCH_SIZE, retry_delay=5):
    headers = {
        "Authorization": f"Bearer {token}",
        "Content-Type": "application/json"
    }

    user_ids = [user["id"] for user in users]

    for i in range(0, len(user_ids), batch_size):
        batch = user_ids[i:i + batch_size]
        print(f"\n🧹 Sending delete batch {i // batch_size + 1} ({len(batch)} users)...")

        batch_request = {
            "requests": []
        }

        for user_id in batch:
            req_id = str(uuid.uuid4())
            batch_request["requests"].append({
                "id": req_id,
                "method": "DELETE",
                "url": f"/users/{user_id}",
                "headers": {
                    "Content-Type": "application/json"
                }
            })

        resp = requests.post(BATCH_ENDPOINT, headers=headers, json=batch_request)
        if resp.status_code != 200:
            print(f"❌ Batch delete failed: {resp.status_code} {resp.text}")
            print("Retrying after delay...")
            time.sleep(retry_delay)
            resp = requests.post(BATCH_ENDPOINT, headers=headers, json=batch_request)

        if resp.status_code == 200:
            for res in resp.json().get("responses", []):
                status = res["status"]
                if 200 <= status < 300:
                    print(f"✅ Deleted (id {res['id']})")
                else:
                    print(f"❌ Failed to delete (id {res['id']}): {status}, {res.get('body', {}).get('error', {}).get('message')}")
        else:
            print("⚠️ Skipping this batch due to repeated failure.")


def main():
    # Load environment variables from .env file
    load_dotenv()

    parser = argparse.ArgumentParser()
    parser.add_argument("--tenant-id", required=False, default=os.getenv("ENTRA_TENANT_ID"))
    parser.add_argument("--client-id", required=False, default=os.getenv("ENTRA_CLIENT_ID"))
    parser.add_argument("--client-secret", required=False, default=os.getenv("ENTRA_CLIENT_SECRET"))
    parser.add_argument("--prefix", default="testuser")
    parser.add_argument("--batch-size", type=int, default=20)
    args = parser.parse_args()

    token = get_token(args.tenant_id, args.client_id, args.client_secret)
    users = get_test_users(token, prefix=args.prefix)

    if not users:
        print("✅ No users to delete.")
        return

    batch_delete_users(token, users, args.batch_size)


if __name__ == "__main__":
    main()
