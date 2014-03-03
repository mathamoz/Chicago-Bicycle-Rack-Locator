<?php
        $lat = $_GET['lat'];
        $lng = $_GET['lng'];
        $search = $_GET['search'];

        if (!isset($lat))
                $lat = "41.878114";
        if (!isset($lng))
                $lng = "-87.629798";

        if (!isset($search))
                $search = "loop";

        $ch = curl_init("http://where.yahooapis.com/geocode?q=" . urlencode($search) . "+Chicago+IL&appid=ShJkPr72");
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, 1);
        $response = curl_exec($ch);
        curl_close($ch);

        $xml = new SimpleXMLElement($response);

        $latitude = $xml->Result->latitude;
        $longitude = $xml->Result->longitude;

        if ($latitude)
                $lat = $latitude;
        if ($longitude)
                $lng = $longitude;

        $link = mysql_connect('', '', '')
            or die('Could not connect: ' . mysql_error());

        mysql_select_db('bikeracks') or die('Could not select database');
        $res = mysql_query("SELECT RackID, Address, TotInstall as Installed, CommunityName, Latitude, Longitude, ( 3959 * acos( cos( radians($lat) ) * cos( radians( Latitude ) ) * cos( radians( Longitude ) - radians($lng) ) + sin( radians($lat) ) * sin( radians( Latitude ) ) ) ) AS distance 
        FROM racks WHERE Address OR CommunityName LIKE '%$search%' OR Address LIKE '%$search%' ORDER BY distance LIMIT 40;") or die("Query failed! " . mysql_error());

        $results = array();

        while ($row = mysql_fetch_array($res)) {
                $val = array("Address" => $row[1], "Installed" => $row[2], "CommunityName" => $row[3], "Latitude" => $row[4], "Longitude" => $row[5], "Distance" => round($row[6], 2));
                array_push($results, $val);
        }

        echo json_encode($results);

        mysql_close($link);
?>