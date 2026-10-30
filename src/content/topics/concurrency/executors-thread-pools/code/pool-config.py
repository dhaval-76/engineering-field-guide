from concurrent.futures import ThreadPoolExecutor, ProcessPoolExecutor, as_completed

# I/O-bound: threads (the GIL is released while waiting on network/disk)
with ThreadPoolExecutor(max_workers=32, thread_name_prefix="fetch") as pool:
    futures = [pool.submit(fetch, url) for url in urls]
    for f in as_completed(futures):
        print(f.result())          # re-raises the task's exception here

# CPU-bound: processes, to use multiple cores despite the GIL
with ProcessPoolExecutor() as pool:
    hashes = list(pool.map(hash_file, files))
