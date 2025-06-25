import requests
import sys

def get_token(tenant_id, client_id, client_secret):
    token_url = f"https://login.microsoftonline.com/{tenant_id}/oauth2/v2.0/token"
    headers = {"Content-Type": "application/x-www-form-urlencoded"}
    data = {
        "client_id": client_id,
        "scope": "https://graph.microsoft.com/.default",
        "client_secret": client_secret,
        "grant_type": "client_credentials"
    }

    response = requests.post(token_url, data=data, headers=headers)

    if response.status_code != 200:
        print(f"Failed to get token: {response.status_code} {response.text}", file=sys.stderr)
        sys.exit(1)

    return response.json().get("access_token")
