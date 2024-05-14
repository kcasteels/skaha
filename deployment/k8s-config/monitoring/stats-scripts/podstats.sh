#!/bin/bash

#Base path of working directory:
path="/home/casteels/stats/pods"

#Find the name of an arc-tomcat pod:
pod=$(kubectl -n skaha-system get pods --no-headers -o custom-columns=":metadata.name" | grep arc-tomcat | head -n 1)

#Generate a list of all users:
kubectl exec -it --namespace=skaha-system $pod -- bash -c 'ls /cephfs/cavern/home/ | tr " " "\n"' > $path/all-users.txt

#Get pod resource requests:
kubectl -n skaha-workload get pods --no-headers=true -o custom-columns='NAME:.metadata.name, CPU_REQUEST:.spec.containers[*].resources.requests.cpu, MEMORY_REQUEST:.spec.containers[*].resources.requests.memory' > $path/getpods.out

#Get pod resource usage:
kubectl -n skaha-workload top pods --no-headers=true > $path/toppods.out

#Get UNIX system time:
jd=$(date +%s)

#Merge the get and top files together, matching on pod name:
join $path/getpods.out $path/toppods.out > $path/loadpods.out

#Loop through all pods in list:
while read line; do

	#Parse the line into a variable array:
	read -a array <<< "$line"

	#Pod name:
	pod=${array[0]}

	#Get correct CADC username:
	user=$(echo $pod | cut -d "-" -f 3)
	#user=$(grep -i "$user" $path/all-users.txt | sed 's/[^[:alnum:]]//g')
	user=$(grep -i "$user" $path/all-users.txt)

	#Convert request cores to millicores:
	r_c=${array[1]}
	if [[ "$r_c" == *"m" ]]; then
		r_c=$(tr -dc '0-9' <<< $r_c)
	elif [[ "$r_c" == *"none"* ]]; then
		r_c=0
	else
		r_c=$((r_c*1000))
	fi

	#Convert GB to MB
	r_m=$(echo "${array[2]}" | sed 's/[^0-9]*//g')
	r_m=$((r_m*1024))
	
	#Used cores:
	u_c=$(echo "${array[3]}" | sed 's/[^0-9]*//g')
	
	#Used RAM:
	u_m=$(echo "${array[4]}" | sed 's/[^0-9]*//g')

	#Output to stats file:
	echo "$jd,$user,$pod,$r_c,$r_m,$u_c,$u_m" >> $path/podstats.csv

	
done < $path/loadpods.out

python3 /home/casteels/stats/pods/plot-pods.py

sh /home/casteels/stats/pods/copy-plots.sh

