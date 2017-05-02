done = function(summary, latency, requests)
    io.write(string.format("%s,%s,%s,%d,%d,%d,%d,%d,%d\n",
        os.getenv("CONFIG"),
        os.getenv("POOL_SIZE"),
        os.getenv("MAX_THREADS"),
        summary.requests,
        latency.mean,
        latency.stdev,
        latency:percentile(50),
        latency:percentile(90),
        latency:percentile(99)))
end