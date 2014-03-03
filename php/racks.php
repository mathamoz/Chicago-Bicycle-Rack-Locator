<?php
	$lat = $_POST['lat'];
	$lng = $_POST['lng'];
	$dst = $_POST['dst'];
	
	if (!isset($lat))
		$lat = "41.878114";
	if (!isset($lng))
		$lng = "-87.629798";

	if (!isset($dst))
		$dst = 10;

	$link = mysql_connect('', '', '')
	    or die('Could not connect: ' . mysql_error());

	if ($dst > 10) {
		$dst = $dst / 40;
		$orderBy = "RAND()";
	} else {
		$orderBy = "distance";
	}
	
	mysql_select_db('bikeracks') or die('Could not select database');
	$res = mysql_query("SELECT RackID, Address, TotInstall as Installed, CommunityName, Latitude, Longitude, ( 3959 * acos( cos( radians($lat) ) * cos( radians( Latitude ) ) * cos( radians( Longitude ) - radians($lng) ) + sin( radians($lat) ) * sin( radians( Latitude ) ) ) ) AS distance 
	FROM racks HAVING distance < $dst ORDER BY $orderBy LIMIT 100;") or die("Query failed! " . mysql_error());
	
	$results = array();
	
	while ($row = mysql_fetch_array($res)) {
		$val = array("Address" => $row[1], "Installed" => $row[2], "CommunityName" => $row[3], "Latitude" => $row[4], "Longitude" => $row[5], "Distance" => round($row[6], 2));
		array_push($results, $val);
	}
	
	echo json_encode($results);
	
	mysql_close($link);
?>