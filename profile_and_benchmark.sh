#!/bin/bash -ex


SCRIPT_NAMES=(put put_r get get_r get_put get_put_r)
THREAD_NUMS=(1 2 4)


start_cluster() {
  ./gradlew run &> /dev/null &
  sleep 5
}

get_cluster_pid() {
  jps | grep Cluster | awk '{print $1;}'
}

stop_cluster() {
  kill $(get_cluster_pid)
  sleep 5
}


for s in ${SCRIPT_NAMES[*]}
do
  for t in ${THREAD_NUMS[*]}
  do
    start_cluster

    async-profiler/profiler.sh -d 60 -f flamegraph_${s}_${t}.svg $(get_cluster_pid) &
    
    echo "wrk -c$t -t$t -d60s -s wrk_scripts/$s.lua http://localhost:8080" >> LOADTEST.md
    wrk --latency -c$t -t$t -d60s -s wrk_scripts/$s.lua http://localhost:8080 >> LOADTEST.md
    
    stop_cluster
  done
done

