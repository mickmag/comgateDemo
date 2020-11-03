# Comgate integration in Java

This is simple application to test connection to Comgate pay wall.

## Configuration

Rename .env.example to .env and fill your 'user' (merchant) and 'secret' to your Comgate account.

## Run the application

Compile the application
```bash
mvn install
```

Run app 
```bash
java -jar target/notariComgateDemo-1.0-SNAPSHOT.jar
```

The application should now be running at [http://localhost:4567/](http://localhost:4567/)

## Run with ngrok

To get public URL use ngrok. Use the ngrok in your Comgate account to configure retrieve for /status and /result pages.

```bash
ngrok http 4567
```