<?php
require_once __DIR__.'/google-api-php-client/vendor/autoload.php';
session_start();

$client = new Google_Client();
$client->setAuthConfig(__DIR__ . '/client_secrets.json');
$client->setRedirectUri('http://' . $_SERVER['HTTP_HOST'] . '/putInDatabase.php');
$client->addScope(Google_Service_Analytics::ANALYTICS_READONLY);
$client->setAccessType('offline');

if (! isset($_GET['code'])) {
    $auth_url = $client->createAuthUrl();
    header('Location: ' . filter_var($auth_url, FILTER_SANITIZE_URL));
} else {
    $client->authenticate($_GET['code']);
    $_SESSION['access_token'] = $client->getAccessToken();
    $redirect_uri = 'http://' . $_SERVER['HTTP_HOST'] . '/putInDatabase.php';
    header('Location: ' . filter_var($redirect_uri, FILTER_SANITIZE_URL));
}

