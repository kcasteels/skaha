#!/bin/bash


#Find the name of an arc-tomcat pod:
pod=$(kubectl -n skaha-system get pods --no-headers -o custom-columns=":metadata.name" | grep arc-tomcat | head -n 1)


arcpath='/cephfs/cavern/home'
localpath='/home/casteels/stats/pods/plots'



#Generate user list:
ls -1 $localpath/ | sed 's/-stats//g' > users.txt

#Loop through users:
while read u; do

	echo $u

	cat > $localpath/$u-stats/$u-stats.html <<-EOFMarker
<!DOCTYPE html>
<html>
<head>
<title>CANFAR Science Portal Resource Usage for $u</title>
<meta http-equiv="refresh" content="600">
<style>
* {
  box-sizing: border-box;
}

.column {
  float: left;
  width: 33.33%;
  padding: 5px;
}

/* Clearfix (clear floats) */
.row::after {
  content: "";
  clear: both;
  display: table;
}

/* Responsive layout - makes the three columns stack on top of each other instead of next to each other */
@media screen and (max-width: 500px) {
  .column {
    width: 100%;
  }
}

</style>
</head>
<body>

<h2 style="background-color:cyan">Total Resource Usage for $u</h2>
<p>These plots represent all resource usage for user $u. Plots are updated once per hour.</p>


<div class="row">
  <div class="column">
    <img src="img/$u-cpu.png" alt="CPU Usage" style="width:100%">
  </div>
  <div class="column">
    <img src="img/$u-ram.png" alt="RAM Usage" style="width:100%">
  </div>
  <div class="column">
    <img src="img/$u-load.png" alt="CPU and RAM Load" style="width:100%">
  </div>
</div>

<h2 style="background-color:orange">Interactive Session Resource Usage for $u</h2>
<p>These plots show resource usage for all interactive sessions combined, including Desktop, Notebook, Carta and other sessions with a GUI.</p>

<div class="row">
  <div class="column">
    <img src="img/$u-interactive-cpu.png" alt="CPU Usage" style="width:100%">
  </div>
  <div class="column">
    <img src="img/$u-interactive-ram.png" alt="RAM Usage" style="width:100%">
  </div>
  <div class="column">
    <img src="img/$u-interactive-load.png" alt="CPU and RAM Load" style="width:100%">
  </div>
</div>

<h2 style="background-color:DarkKhaki">Headless Session Resource Usage for $u</h2>
<p>These plots represent resource usage for all headless jobs combined.</p>

<div class="row">
  <div class="column">
    <img src="img/$u-headless-cpu.png" alt="CPU Usage" style="width:100%">
  </div>
  <div class="column">
    <img src="img/$u-headless-ram.png" alt="RAM Usage" style="width:100%">
  </div>
  <div class="column">
    <img src="img/$u-headless-load.png" alt="CPU and RAM Load" style="width:100%">
  </div>
</div>

<h2 style="background-color:Tomato">Total CANFAR Science Portal Resource Usage</h2>
<p>These plots represent total resource usage for all users combined.</p>

<div class="row">
  <div class="column">
    <img src="img/combined-cpu.png" alt="CPU Usage" style="width:100%">
  </div>
  <div class="column">
    <img src="img/combined-ram.png" alt="RAM Usage" style="width:100%">
  </div>
  <div class="column">
    <img src="img/combined-load.png" alt="CPU and RAM Load" style="width:100%">
  </div>
</div>

</body>
</html>
EOFMarker
	
	kubectl -n skaha-system cp $localpath/$u-stats $pod:$arcpath/$u/.

done < users.txt
