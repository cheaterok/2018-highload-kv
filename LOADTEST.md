Ниже приведены результаты измерения производительности программы под различными видами нагрузки:

## PUT

[Flamegraph](https://github.com/cheaterok/2018-highload-kv/blob/master/flamegraphs/flamegraph_put_1.svg)
```
wrk -c1 -t1 -d60s -s wrk_scripts/put.lua http://localhost:8081
Running 1m test @ http://localhost:8080
  1 threads and 1 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency     2.31ms    8.07ms 226.75ms   99.50%
    Req/Sec   537.27     98.70     1.10k    89.63%
  Latency Distribution
     50%    1.86ms
     75%    1.91ms
     90%    1.99ms
     99%    4.23ms
  31992 requests in 1.00m, 5.40MB read
Requests/sec:    533.01
Transfer/sec:     92.13KB
```
[Flamegraph](https://github.com/cheaterok/2018-highload-kv/blob/master/flamegraphs/flamegraph_put_2.svg)
```
wrk -c2 -t2 -d60s -s wrk_scripts/put.lua http://localhost:8080
Running 1m test @ http://localhost:8080
  2 threads and 2 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency     1.62ms   10.79ms 279.40ms   99.44%
    Req/Sec     1.09k   238.38     1.23k    87.21%
  Latency Distribution
     50%  813.00us
     75%    0.89ms
     90%    1.14ms
     99%    5.28ms
  129736 requests in 1.00m, 21.90MB read
Requests/sec:   2162.13
Transfer/sec:    373.73KB
```
[Flamegraph](https://github.com/cheaterok/2018-highload-kv/blob/master/flamegraphs/flamegraph_put_4.svg)
```
wrk -c4 -t4 -d60s -s wrk_scripts/put.lua http://localhost:8080
Running 1m test @ http://localhost:8080
  4 threads and 4 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency     2.52ms   14.97ms 360.81ms   99.32%
    Req/Sec   710.36    160.86     0.87k    84.84%
  Latency Distribution
     50%    1.23ms
     75%    1.55ms
     90%    2.22ms
     99%   10.02ms
  168924 requests in 1.00m, 28.51MB read
Requests/sec:   2814.25
Transfer/sec:    486.45KB
```
[Flamegraph](https://github.com/cheaterok/2018-highload-kv/blob/master/flamegraphs/flamegraph_put_r_1.svg)
```
wrk -c1 -t1 -d60s -s wrk_scripts/put_r.lua http://localhost:8080
Running 1m test @ http://localhost:8080
  1 threads and 1 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency     2.26ms    7.72ms 219.10ms   99.52%
    Req/Sec   544.02    101.05     1.10k    88.46%
  Latency Distribution
     50%    1.85ms
     75%    1.90ms
     90%    1.98ms
     99%    4.12ms
  32387 requests in 1.00m, 5.47MB read
Requests/sec:    539.48
Transfer/sec:     93.25KB
```
[Flamegraph](https://github.com/cheaterok/2018-highload-kv/blob/master/flamegraphs/flamegraph_put_r_2.svg)
```
wrk -c2 -t2 -d60s -s wrk_scripts/put_r.lua http://localhost:8080
Running 1m test @ http://localhost:8080
  2 threads and 2 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency     2.07ms   16.70ms 378.80ms   99.43%
    Req/Sec     1.10k   228.87     1.22k    87.94%
  Latency Distribution
     50%  809.00us
     75%    0.88ms
     90%    1.09ms
     99%    5.20ms
  130886 requests in 1.00m, 22.09MB read
Requests/sec:   2180.88
Transfer/sec:    376.97KB
```
[Flamegraph](https://github.com/cheaterok/2018-highload-kv/blob/master/flamegraphs/flamegraph_put_r_4.svg)
```
wrk -c4 -t4 -d60s -s wrk_scripts/put_r.lua http://localhost:8080
Running 1m test @ http://localhost:8080
  4 threads and 4 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency     2.49ms   15.29ms 361.52ms   99.39%
    Req/Sec   718.96    149.87     0.87k    85.68%
  Latency Distribution
     50%    1.23ms
     75%    1.54ms
     90%    2.15ms
     99%    9.18ms
  170968 requests in 1.00m, 28.86MB read
Requests/sec:   2848.41
Transfer/sec:    492.36KB
```

## GET

[Flamegraph](https://github.com/cheaterok/2018-highload-kv/blob/master/flamegraphs/flamegraph_get_1.svg)
```
wrk -c1 -t1 -d60s -s wrk_scripts/get.lua http://localhost:8080
Running 1m test @ http://localhost:8080
  1 threads and 1 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency     2.17ms    6.92ms 206.71ms   99.61%
    Req/Sec   546.33    117.95     1.14k    88.80%
  Latency Distribution
     50%    1.87ms
     75%    1.92ms
     90%    2.01ms
     99%    3.89ms
  32535 requests in 1.00m, 5.80MB read
  Non-2xx or 3xx responses: 32535
Requests/sec:    542.04
Transfer/sec:     98.99KB
```
[Flamegraph](https://github.com/cheaterok/2018-highload-kv/blob/master/flamegraphs/flamegraph_get_2.svg)
```
wrk -c2 -t2 -d60s -s wrk_scripts/get.lua http://localhost:8080
Running 1m test @ http://localhost:8080
  2 threads and 2 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency     1.83ms   13.97ms 332.20ms   99.40%
    Req/Sec     1.12k   209.70     1.23k    90.28%
  Latency Distribution
     50%  804.00us
     75%    0.87ms
     90%    1.00ms
     99%    4.93ms
  133569 requests in 1.00m, 23.82MB read
  Non-2xx or 3xx responses: 133569
Requests/sec:   2225.89
Transfer/sec:    406.49KB
```
[Flamegraph](https://github.com/cheaterok/2018-highload-kv/blob/master/flamegraphs/flamegraph_get_4.svg)
```
wrk -c4 -t4 -d60s -s wrk_scripts/get.lua http://localhost:8080
Running 1m test @ http://localhost:8080
  4 threads and 4 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency     2.45ms   16.04ms 374.27ms   99.42%
    Req/Sec   770.54    160.16     0.94k    86.43%
  Latency Distribution
     50%    1.14ms
     75%    1.42ms
     90%    2.01ms
     99%    8.46ms
  183215 requests in 1.00m, 32.67MB read
  Non-2xx or 3xx responses: 183215
Requests/sec:   3052.01
Transfer/sec:    557.36KB
```
[Flamegraph](https://github.com/cheaterok/2018-highload-kv/blob/master/flamegraphs/flamegraph_get_r_1.svg)
```
wrk -c1 -t1 -d60s -s wrk_scripts/get_r.lua http://localhost:8080
Running 1m test @ http://localhost:8080
  1 threads and 1 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency     2.31ms    8.24ms 230.79ms   99.49%
    Req/Sec   539.46    104.71     1.13k    89.46%
  Latency Distribution
     50%    1.87ms
     75%    1.92ms
     90%    2.00ms
     99%    4.14ms
  32122 requests in 1.00m, 5.73MB read
  Non-2xx or 3xx responses: 32122
Requests/sec:    535.13
Transfer/sec:     97.72KB
```
[Flamegraph](https://github.com/cheaterok/2018-highload-kv/blob/master/flamegraphs/flamegraph_get_r_2.svg)
```
wrk -c2 -t2 -d60s -s wrk_scripts/get_r.lua http://localhost:8080
Running 1m test @ http://localhost:8080
  2 threads and 2 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency     1.99ms   15.91ms 363.36ms   99.42%
    Req/Sec     1.12k   223.14     1.23k    88.02%
  Latency Distribution
     50%  802.00us
     75%    0.87ms
     90%    1.05ms
     99%    5.03ms
  132679 requests in 1.00m, 23.66MB read
  Non-2xx or 3xx responses: 132679
Requests/sec:   2211.06
Transfer/sec:    403.78KB
```
[Flamegraph](https://github.com/cheaterok/2018-highload-kv/blob/master/flamegraphs/flamegraph_get_r_4.svg)
```
wrk -c4 -t4 -d60s -s wrk_scripts/get_r.lua http://localhost:8080
Running 1m test @ http://localhost:8080
  4 threads and 4 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency     2.50ms   16.62ms 389.65ms   99.41%
    Req/Sec   769.00    160.16     0.91k    86.60%
  Latency Distribution
     50%    1.14ms
     75%    1.42ms
     90%    2.00ms
     99%    8.50ms
  182805 requests in 1.00m, 32.60MB read
  Non-2xx or 3xx responses: 182805
Requests/sec:   3045.02
Transfer/sec:    556.08KB
```

## GET-PUT

[Flamegraph](https://github.com/cheaterok/2018-highload-kv/blob/master/flamegraphs/flamegraph_get_put_1.svg)
```
wrk -c1 -t1 -d60s -s wrk_scripts/get_put.lua http://localhost:8080
Running 1m test @ http://localhost:8080
  1 threads and 1 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency     2.19ms    7.93ms 224.81ms   99.48%
    Req/Sec   569.96    132.92     1.16k    87.63%
  Latency Distribution
     50%    1.80ms
     75%    1.88ms
     90%    1.96ms
     99%    4.05ms
  33946 requests in 1.00m, 5.97MB read
  Non-2xx or 3xx responses: 1
Requests/sec:    565.46
Transfer/sec:    101.88KB
```
[Flamegraph](https://github.com/cheaterok/2018-highload-kv/blob/master/flamegraphs/flamegraph_get_put_2.svg)
```
wrk -c2 -t2 -d60s -s wrk_scripts/get_put.lua http://localhost:8080
Running 1m test @ http://localhost:8080
  2 threads and 2 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency     1.47ms    8.90ms 245.79ms   99.41%
    Req/Sec     1.13k   236.21     1.26k    87.46%
  Latency Distribution
     50%  792.00us
     75%    0.86ms
     90%    1.07ms
     99%    5.05ms
  134574 requests in 1.00m, 23.68MB read
  Non-2xx or 3xx responses: 1
Requests/sec:   2242.70
Transfer/sec:    404.08KB
```
[Flamegraph](https://github.com/cheaterok/2018-highload-kv/blob/master/flamegraphs/flamegraph_get_put_4.svg)
```
wrk -c4 -t4 -d60s -s wrk_scripts/get_put.lua http://localhost:8080
Running 1m test @ http://localhost:8080
  4 threads and 4 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency     2.11ms   10.91ms 286.88ms   99.34%
    Req/Sec   741.75    168.84     0.91k    84.57%
  Latency Distribution
     50%    1.18ms
     75%    1.49ms
     90%    2.14ms
     99%    9.06ms
  176649 requests in 1.00m, 31.08MB read
Requests/sec:   2942.93
Transfer/sec:    530.25KB
```
[Flamegraph](https://github.com/cheaterok/2018-highload-kv/blob/master/flamegraphs/flamegraph_get_put_r_1.svg)
```
wrk -c1 -t1 -d60s -s wrk_scripts/get_put_r.lua http://localhost:8080
Running 1m test @ http://localhost:8080
  1 threads and 1 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency     2.50ms   10.80ms 278.11ms   99.49%
    Req/Sec   534.08     94.87     1.09k    89.30%
  Latency Distribution
     50%    1.87ms
     75%    1.92ms
     90%    2.00ms
     99%    4.13ms
  31796 requests in 1.00m, 5.37MB read
Requests/sec:    529.62
Transfer/sec:     91.54KB
```
[Flamegraph](https://github.com/cheaterok/2018-highload-kv/blob/master/flamegraphs/flamegraph_get_put_r_2.svg)
```
wrk -c2 -t2 -d60s -s wrk_scripts/get_put_r.lua http://localhost:8080
Running 1m test @ http://localhost:8080
  2 threads and 2 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency     2.03ms   16.12ms 368.91ms   99.43%
    Req/Sec     1.09k   237.69     1.28k    86.18%
  Latency Distribution
     50%  810.00us
     75%    0.89ms
     90%    1.17ms
     99%    5.67ms
  129999 requests in 1.00m, 22.37MB read
  Non-2xx or 3xx responses: 2
Requests/sec:   2166.25
Transfer/sec:    381.77KB
```
[Flamegraph](https://github.com/cheaterok/2018-highload-kv/blob/master/flamegraphs/flamegraph_get_put_r_4.svg)
```
wrk -c4 -t4 -d60s -s wrk_scripts/get_put_r.lua http://localhost:8080
Running 1m test @ http://localhost:8080
  4 threads and 4 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency     2.67ms   17.52ms 409.88ms   99.38%
    Req/Sec   725.36    149.19     0.88k    86.63%
  Latency Distribution
     50%    1.22ms
     75%    1.52ms
     90%    2.14ms
     99%    9.87ms
  172357 requests in 1.00m, 29.09MB read
Requests/sec:   2871.15
Transfer/sec:    496.29KB
```
