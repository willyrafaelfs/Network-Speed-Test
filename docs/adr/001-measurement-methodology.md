# ADR 001: Measurement methodology

- **Status:** Accepted
- **Date:** 2026-11-15
- **Scope:** `data/remote` (ping, download and upload engines)

## Context

The first version of the engine opened **one TCP connection** per phase and reported the **average over the
whole 10 seconds**. That approach has three known biases:

1. **One connection rarely fills a fast link.** A single TCP flow is limited by its congestion window and
   receive window, and it backs off hard after packet loss. Tools such as Ookla Speedtest, Fast.com and the
   Cloudflare speed test all use several parallel connections.
2. **TCP slow start is counted.** The first one or two seconds are spent ramping up. Including them drags the
   average down, and the effect gets worse as the link gets faster.
3. **Upload counted bytes written, not bytes delivered.** `RequestBody.writeTo` returns as soon as bytes are
   copied into the kernel's socket send buffer, which can hold several megabytes. Early samples were
   inflated by however much fit into that buffer.

We also wanted to measure **latency under load** (bufferbloat), which a single idle ping can't capture.

## Decision

### Parallel streams over HTTP/1.1

Download and upload each run `streamCount` (default **4**) workers in parallel. Each worker owns one HTTP
request at a time, and the byte counters of all workers are summed.

The OkHttp client is pinned to **HTTP/1.1**. With HTTP/2, OkHttp would multiplex every worker onto a single
TCP connection, which would bring back bias 1. On HTTP/1.1, each in-flight request gets its own
connection.

Download responses are requested with `Accept-Encoding: identity`, so the bytes we count are the bytes on
the wire and not OkHttp's transparently decompressed output.

### Ramp-up exclusion, sliding windows, 90th percentile

Every `sampleInterval` (250 ms), the engine records how many bytes arrived during that interval. The final
result is then computed by the pure function `ThroughputCalculator.finalMbps`:

1. Discard everything before `rampUpMillis` (**2 s**) to remove TCP slow start.
2. Slide a **1 s window** across the remaining time in 250 ms steps, and compute Mbps for each window.
3. Report the **90th percentile** of those window rates.

The percentile approach follows the Cloudflare speed test, which reports the 90th percentile of its
measurements. It ignores short dips caused by Wi-Fi contention or a lost packet, while still requiring
the speed to be sustained for a whole second. The live estimate shown during the test uses the same
function, so the number doesn't jump when the phase ends.

### Upload: count only what the server acknowledged

The live gauge still uses bytes written to the socket, because that keeps it responsive. The **final**
upload result, however, uses only **completed requests**:

- Each worker repeatedly POSTs a body of a fixed, known size (random bytes, so it can't be compressed).
- A request counts once the server has responded. Its bytes are spread evenly between the moment the
  request started and the moment the response arrived. By then the data has left the send buffer and
  reached the server, so buffer inflation can't leak into the result.
- Request size starts at 64 KiB and doubles while a request finishes in under 1 s, up to 16 MiB. Slow links
  still complete requests, and fast links don't waste time on per-request round trips.
- When the phase ends, any request still in flight is cancelled and its bytes are dropped. The analysis
  window therefore stops at the **start of the oldest unfinished request**. After that point, at least one
  connection's bytes are missing, and the result would come out too low.

If no request completed after the ramp-up (an extremely slow link), the engine falls back to the average
of bytes written, and accepts that the result is less accurate.

### Latency under load and bufferbloat grade

While download and upload run, a separate connection sends the 0-byte ping request every 500 ms. The
**median** of those samples is reported per phase.

- The ping client has **its own connection pool**, and its connection is warmed up right before the load
  starts. A loaded ping then measures one round trip and not a TCP + TLS handshake. Without this, a
  throughput worker can grab the warm connection from the shared pool. On a link with heavy downlink
  bufferbloat, the probe's new handshake (about 4 round trips under load) took longer than the whole phase,
  so no sample completed and the grade silently ignored the worst phase. Real-device testing found this in
  Phase 2.
- If no ping has completed when the phase ends, the time the in-flight ping has waited so far is recorded as
  a **lower bound**. An empty value would hide exactly the case the grade exists to show.

The grade is based on the worst increase over idle latency, using the thresholds of the
[Waveform bufferbloat test](https://www.waveform.com/tools/bufferbloat):

| Grade | Latency increase |
|---|---|
| A+ | ≤ 5 ms |
| A | ≤ 30 ms |
| B | ≤ 60 ms |
| C | ≤ 200 ms |
| D | ≤ 400 ms |
| F | > 400 ms |

### Idle latency: HTTP round-trip time

ICMP needs raw sockets, which Android apps don't get without root. Latency is therefore the round-trip
time of an HTTP request to a 0-byte endpoint, measured after one warm-up request that absorbs DNS, TCP
and TLS setup. The result is the **median** of 10 samples. The mean was used until v1.4.0, but one slow
request (a retransmission, or a busy CPU right after install) could raise the idle latency. That in turn
made the bufferbloat grade look better than it was. Jitter is the mean absolute difference between
consecutive samples, as in RFC 3550, so it still reflects such spikes.

### Errors

The engine throws raw `IOException`s, and the repository translates them into a domain
`SpeedTestError` (`NoConnection`, `Timeout`, `ServerError(httpCode)`, `Unknown`), so the UI can show a
helpful message. A user cancelling the test is plain coroutine cancellation and never surfaces as an
error. Every in-flight OkHttp call is cancelled explicitly, because a blocking socket read doesn't react
to coroutine cancellation on its own.

## Consequences

- Results on fast links (>100 Mbps) should be much closer to multi-stream tools. Validating that against
  Ookla and Cloudflare on the same network is tracked as a separate benchmark task.
- Each phase opens 5 connections (4 for throughput and 1 for ping) instead of 1. That's negligible for
  the Cloudflare endpoint, but a self-hosted server would need to allow it.
- The calculation logic is pure and covered by unit tests. The engines are tested end-to-end against a
  bandwidth-throttled `MockWebServer`.
- Every parameter lives in `SpeedTestConfig`, so the upcoming Settings screen can expose the stream count
  and the test duration without touching the engine.

## Limitations

- HTTP RTT is slightly higher than ICMP ping, because it includes server request handling.
- Latency under load is measured on its own connection. It shows queueing in the network (bufferbloat),
  not head-of-line blocking inside the throughput connections.
- All traffic goes to Cloudflare's anycast edge. Results reflect the path to the nearest Cloudflare PoP,
  not to an arbitrary internet destination.
- A connection that fails mid-test fails the whole phase. Retrying individual streams is possible, but it
  would make results harder to interpret.

## Alternatives considered

- **Mean of the steady-state phase** (the M-Lab NDT style) instead of p90. It is simpler, but on Wi-Fi it
  is more sensitive to short contention dips. The percentile is configurable (`resultPercentile`), so
  this can be revisited after benchmarking.
- **One long chunked upload per stream** (the old approach) combined with a send-buffer size correction.
  It was rejected because the buffer size is auto-tuned by the kernel and can't be read reliably from an
  app.
- **NDT7 over WebSocket (M-Lab).** It provides TCP_INFO-based measurements from the server side, but it
  would need a different backend. The idea is listed as an optional `SpeedTestProvider` abstraction in the
  roadmap.
