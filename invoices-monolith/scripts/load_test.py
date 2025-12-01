import urllib.request
import urllib.error
import json
import concurrent.futures
import time
import random
import string
import sys

# Default to localhost, but allow override via env var or arg could be added later
BASE_URL = "http://localhost:8080/api"

def generate_random_string(length=10):
    return ''.join(random.choices(string.ascii_letters + string.digits, k=length))

def register_user():
    email = f"loadtest_{generate_random_string()}@example.com"
    password = "Password123!"
    payload = {
        "email": email,
        "password": password,
        "firstName": "Load",
        "lastName": "Test",
        "registrationType": "NEW_COMPANY",
        "companyName": f"Company {generate_random_string()}",
        "taxId": f"B{generate_random_string(8)}",
        "companyAddress": "123 Test St",
        "companyPhone": "555-0123",
        "companyEmail": email
    }
    
    req = urllib.request.Request(
        f"{BASE_URL}/auth/register",
        data=json.dumps(payload).encode('utf-8'),
        headers={'Content-Type': 'application/json'}
    )
    
    try:
        with urllib.request.urlopen(req) as response:
            if response.status == 201:
                data = json.loads(response.read().decode('utf-8'))
                print(f"Registered user: {email}")
                return data['token']
    except urllib.error.HTTPError as e:
        print(f"Registration failed: {e.read().decode('utf-8')}")
        return None
    except urllib.error.URLError as e:
        print(f"Connection failed: {e.reason}")
        return None

def get_invoices(token):
    req = urllib.request.Request(
        f"{BASE_URL}/invoices",
        headers={
            'Authorization': f'Bearer {token}',
            'Content-Type': 'application/json'
        }
    )
    try:
        start_time = time.time()
        with urllib.request.urlopen(req) as response:
            duration = time.time() - start_time
            # Read body to ensure request completes
            response.read()
            return response.status, duration
    except urllib.error.HTTPError as e:
        return e.code, 0
    except Exception as e:
        return 500, 0

def run_load_test(concurrency=50):
    print("Starting load test...")
    
    # Check if server is up
    try:
        urllib.request.urlopen(f"{BASE_URL.replace('/api', '/actuator/health')}", timeout=2)
    except Exception:
        print("Warning: Server might not be running at localhost:8080. Proceeding anyway...")

    token = register_user()
    if not token:
        print("Could not get token. Is the server running?")
        return

    print(f"Running {concurrency} concurrent requests to GET /api/invoices...")
    
    start_total = time.time()
    with concurrent.futures.ThreadPoolExecutor(max_workers=concurrency) as executor:
        futures = [executor.submit(get_invoices, token) for _ in range(concurrency)]
        
        results = []
        for future in concurrent.futures.as_completed(futures):
            results.append(future.result())
    
    total_time = time.time() - start_total
            
    success_count = sum(1 for status, _ in results if status == 200)
    avg_duration = sum(duration for _, duration in results) / len(results) if results else 0
    
    print("-" * 30)
    print(f"Load test completed in {total_time:.2f} seconds")
    print(f"Total Requests: {len(results)}")
    print(f"Successful Requests: {success_count}")
    print(f"Failed Requests: {len(results) - success_count}")
    print(f"Average Request Duration: {avg_duration:.4f} seconds")
    print(f"Requests/Second: {len(results) / total_time:.2f}")
    print("-" * 30)

if __name__ == "__main__":
    run_load_test()
