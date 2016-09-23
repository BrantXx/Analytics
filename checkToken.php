<?php
require_once __DIR__.'/google-api-php-client/vendor/autoload.php';

// This is going to require a for each loop to do "for each row in the table : get the access token, refresh token, and created, check if the access token is expired, if so refresh, if not continue on"

session_start();

$servername = "localhost";
$username = "root";
$password = "root";
$database = "analyticsTest";
$conn = new mysqli($servername, $username, $password, $database);

if ($conn->connect_error) {
	die("Connection failed: " . $conn->connect_error);
}

$sql = "SELECT refresh_token,access_token,created FROM analytics_info";
$result = $conn->query($sql);

if ($result->num_rows > 0) {
    while($row = $result->fetch_assoc()) {
		$refreshToken = $row["refresh_token"];
		$oldToken = $row["access_token"];
		$created = $row["created"];
    }
}

$client = new Google_Client();
$client->setAuthConfig(__DIR__ . '/client_secrets.json');
$client->addScope(Google_Service_Analytics::ANALYTICS_READONLY);
$client->setAccessType('offline');

$oldTokenforGoogle = json_encode(array("access_token" => $oldToken, "token_type" => "Bearer", "expires_in" => 3600, "created" => $created));

$setOldToken = $client->setAccessToken($oldTokenforGoogle);

if($client->isAccessTokenExpired()){
	print "Token is Expired! Refreshing! <br>";

	$client->refreshToken($refreshToken);

	$newtoken = $client->getAccessToken();

	$sql = "UPDATE analytics_info SET access_token = '".$newtoken["access_token"]."', created = '".$newtoken["created"]."' WHERE access_token = '".$oldToken."';";

	if ($conn->query($sql) === TRUE) {
		echo "old token : " . $oldToken . "<br>";
		echo "new token : " . $newtoken["access_token"] . "<br>";
		echo "old create : " . $created . "<br>";
		echo "new create : " . $newtoken["created"];
	} else {
		echo "<br>";
		echo "Error: " . $sql . "<br>" . $conn->error;
	}
}else{
	print "Token is not expired! No refresh needed";
}

?>

