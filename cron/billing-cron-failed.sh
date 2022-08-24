#!/bin/bash

now=$(date)
log="/var/log/cron.log"

# Test if log file exists, otherwise create it.
test -f $log || touch $log

{
  echo "----------------------------------------------------"
  echo "Current date is : $now"
  echo "Will start charging process....."
  echo "----------------------------------------------------"
} >> $log

result=$(curl -s http://antaeus:7000/rest/v1/charing/failed)

if [[ $result == "[]" ]]; then
  echo "No more invoices left for payment" >> $log
else
  echo "$result" >> $log
fi
