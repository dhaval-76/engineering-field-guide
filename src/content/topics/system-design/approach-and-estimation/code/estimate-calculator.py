def estimate(requests_per_day, new_items_per_day, bytes_per_item, bytes_per_response,
             peak_factor=3, replicas=3):
    avg_qps = requests_per_day / 86_400
    peak_qps = avg_qps * peak_factor
    return {
        "avg_qps": round(avg_qps),
        "peak_qps": round(peak_qps),
        "storage_tb_per_year": new_items_per_day * bytes_per_item * 365 * replicas / 1e12,
        "egress_mb_per_s": peak_qps * bytes_per_response / 1e6,
    }

print(estimate(10e9, 100e6, 500, 500))
# {'avg_qps': 115741, 'peak_qps': 347222, 'storage_tb_per_year': 54.75, 'egress_mb_per_s':
# 173.6}
