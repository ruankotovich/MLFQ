#!/bin/sh

for queue in $(seq "$1" "$2")
do
  for minimal in $(seq "$3" "$4")
  do
    for increment in 1 2 3 4 5
    do
      for refreshTime in 10 30 40 80 100 200 300 500 700 1000 2000 3000
      #while [ $refreshTime -le 100 ]
      do
        java -jar MLFQ.jar "$queue" "$minimal" "$increment" "$refreshTime" < "$5"
      done
    done
  done
done
