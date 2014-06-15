#!/bin/bash 
cd /home/mike/dev/apache-nutch-1.7/ 
 echo "start save the urls"
bin/nutch readseg -dump crawl/segments/20131021150210/ outputdir2 -nocontent -nofetch -nogenerate -noparse -noparsetextless outputdir2/dump > saveNutchUrls_log.txt
 echo "finish save the urls"