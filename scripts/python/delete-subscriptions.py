import json
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

def get_subscriptions(token, prefix="testuser"):
    """Fetches all users with userPrincipalName starting with 'testuser'."""
    headers = {
        "Authorization": f"Bearer {token}"
    }

    url = f"{GRAPH_ENDPOINT}/subscriptions"
    subscriptions = []

    while url:
        resp = requests.get(url, headers=headers)
        if resp.status_code != 200:
            raise Exception(f"Failed to fetch subscriptions: {resp.status_code} {resp.text}")
        data = resp.json()
        subscriptions.extend(data.get("value", []))
        url = data.get("@odata.nextLink")  # for paging

    print(f"🔍 Found {len(subscriptions)} subscriptions")
    return subscriptions

def batch_delete_subscriptions(token, subscriptions, batch_size=MAX_BATCH_SIZE, retry_delay=5):
    headers = {
        "Authorization": f"Bearer {token}",
        "Content-Type": "application/json"
    }

    subscription_ids = [sub["id"] for sub in subscriptions]

    for i in range(0, len(subscription_ids), batch_size):
        batch = subscription_ids[i:i + batch_size]
        print(f"\n🧹 Sending delete batch {i // batch_size + 1} ({len(batch)} subscriptions)...")

        batch_request = {
            "requests": []
        }

        for subscription_id in batch:
            req_id = str(uuid.uuid4())
            batch_request["requests"].append({
                "id": req_id,
                "method": "DELETE",
                "url": f"/subscriptions/{subscription_id}",
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
    args = parser.parse_args()

    token = get_token(args.tenant_id, args.client_id, args.client_secret)
    subscriptions = get_subscriptions(token)

    if not subscriptions:
        print("✅ No subscriptions to delete.")
        return
    
    batch_delete_subscriptions(token, subscriptions)


if __name__ == "__main__":
    main()
