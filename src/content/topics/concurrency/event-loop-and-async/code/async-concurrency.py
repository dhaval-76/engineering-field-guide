import asyncio, httpx

async def fetch_all(urls: list[str]) -> list[str]:
    limit = asyncio.Semaphore(10)                       # at most 10 requests in flight
    async with httpx.AsyncClient(timeout=2.0) as client:
        async def one(url: str) -> str:
            async with limit:
                r = await client.get(url)               # yields to the loop while waiting
                return r.text
        async with asyncio.TaskGroup() as tg:           # Python 3.11+: structured concurrency
            tasks = [tg.create_task(one(u)) for u in urls]
    return [t.result() for t in tasks]

# CPU-bound work: the GIL lets only one thread run Python bytecode at a time, so use processes.
from concurrent.futures import ProcessPoolExecutor
async def crunch(data):
    loop = asyncio.get_running_loop()
    with ProcessPoolExecutor() as pool:
        return await loop.run_in_executor(pool, heavy_computation, data)

# Blocking library inside async code? Offload it: await asyncio.to_thread(blocking_call, arg)
