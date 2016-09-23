<?php
require_once __DIR__.'/google-api-php-client/vendor/autoload.php';
session_start();

$servername = "localhost";
$username = "root";
$password = "root";
$database = "analyticsTest";

$conn = new mysqli($servername, $username, $password, $database);

if ($conn->connect_error) {
	die("Connection failed: " . $conn->connect_error);
}

$client = new Google_Client();
$client->setAuthConfig(__DIR__ . '/client_secrets.json');
$client->addScope(Google_Service_Analytics::ANALYTICS_READONLY);
$client->setAccessType('offline');

$client->authenticate($_GET['code']);
$accessToken = $client->getAccessToken();
$analytics = new Google_Service_Analytics($client);
$profile = getFirstProfileId($analytics);

function getFirstProfileId($analytics) {
  // Get the user's first view (profile) ID.

  // Get the list of accounts for the authorized user.
  $accounts = $analytics->management_accounts->listManagementAccounts();

  if (count($accounts->getItems()) > 0) {
    $items = $accounts->getItems();
    $firstAccountId = $items[0]->getId();

    // Get the list of properties for the authorized user.
    $properties = $analytics->management_webproperties
        ->listManagementWebproperties($firstAccountId);

    if (count($properties->getItems()) > 0) {
      $items = $properties->getItems();
      $firstPropertyId = $items[0]->getId();

      // Get the list of views (profiles) for the authorized user.
      $profiles = $analytics->management_profiles
          ->listManagementProfiles($firstAccountId, $firstPropertyId);

      if (count($profiles->getItems()) > 0) {
        $items = $profiles->getItems();

        // Return the first view (profile) ID.
        return $items[0]->getId();

      } else {
        throw new Exception('No views (profiles) found for this user.');
      }
    } else {
      throw new Exception('No properties found for this user.');
    }
  } else {
    throw new Exception('No accounts found for this user.');
  }
}




$sql = "INSERT INTO analytics_info (view_id, access_token, refresh_token, created, code) VALUES ('".$profile."', '".$accessToken["access_token"]."', '".$accessToken["refresh_token"]."', '".$accessToken["created"]."', '".$_GET['code']."')";

if ($conn->query($sql) === TRUE) {
	echo "Inserted token into database!";
} else {
	echo "<br>";
	echo "Error: " . $sql . "<br>" . $conn->error;
}

?>
