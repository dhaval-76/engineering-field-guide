import json, os, random, time
import httpx

CATEGORIES = {"billing", "bug", "account", "other"}
SYSTEM = (
    "You classify support tickets. Reply with JSON only: "
    '{"category": "billing|bug|account|other", "priority": "low|medium|high", "summary": "<= 20 words"}'
)


def classify(ticket_text: str) -> dict:
    body = {
        "model": "claude-sonnet-5-5",
        "max_tokens": 200,
        "temperature": 0,
        "system": SYSTEM,
        "messages": [{"role": "user", "content": f"<ticket>\n{ticket_text}\n</ticket>"}],
    }
    t = json.loads(call_with_retry(body))  # raises on invalid JSON
    if t.get("category") not in CATEGORIES:
        raise ValueError(f"bad category {t.get('category')}")
    return t


def call_with_retry(body: dict, attempts: int = 4) -> str:
    headers = {
        "x-api-key": os.environ["ANTHROPIC_API_KEY"],
        "anthropic-version": "2023-06-01",
        "content-type": "application/json",
    }
    for attempt in range(attempts):
        res = httpx.post(
            "https://api.anthropic.com/v1/messages", json=body, headers=headers, timeout=30
        )
        if res.status_code == 200:
            data = res.json()
            print("llm usage", data["usage"]["input_tokens"], data["usage"]["output_tokens"])
            return data["content"][0]["text"]
        retryable = res.status_code == 429 or res.status_code >= 500
        if not retryable or attempt == attempts - 1:
            raise RuntimeError(f"LLM call failed: {res.status_code}")
        retry_after = res.headers.get("retry-after")
        time.sleep(float(retry_after) if retry_after else 2**attempt * 0.5 + random.random() * 0.25)
