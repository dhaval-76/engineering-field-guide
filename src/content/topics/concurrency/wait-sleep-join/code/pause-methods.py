import threading, time

time.sleep(0.2)                         # pause this thread

worker = threading.Thread(target=build_report, name="report-builder")
worker.start()
worker.join(timeout=5)                  # wait for it to finish
if worker.is_alive():
    log.warning("report still running")

cond = threading.Condition()            # Python's wait/notify
with cond:
    cond.wait_for(lambda: ready)        # loops on the predicate for you
