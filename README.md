# Analytics

## Requirements
PHP 5.6

MySQL 5.6

## Installation
Run this

``` cd ~/ && git clone https://github.com/BrantXx/Analytics.git && cd Analytics && wget https://github.com/google/google-api-php-client/releases/download/v2.0.3/google-api-php-client-2.0.3.zip && unzip google-api-php-client-2.0.3.zip && mv ~/Analytics/google-api-php-client-2.0.3 ~/Analytics/google-api-php-client && rm -rf google-api-php-client-2.0.3.zip && echo "Finished!!" ```

Run this query in MySQL
```
CREATE SCHEMA `analyticsTest`;

CREATE TABLE `analyticsTest`.`analytics_info` (
  `view_id` INT NOT NULL,
  `access_token` TINYTEXT NULL,
  `refresh_token` TINYTEXT NULL,
  `created` INT NULL,
  `code` TINYTEXT NULL,
  PRIMARY KEY (`view_id`));
```

Get the client_secrets.json from my H-starter clone : /brant/Analytics/client_secrets.json and put it in the Analytics repo

Run this ``` cd ~/Analytics/ && php -S localhost:8000 ```

Go to localhost:8000 in a browser and sign in.

in a terminal run ```php checkToken.php``` to check if token is active/expired

in a terminal run ```php queryGA.php``` to query google analytics

## To do
- Store Query Results into a Database
- Clean up code
