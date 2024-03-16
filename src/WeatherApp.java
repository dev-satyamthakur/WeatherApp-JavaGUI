import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

//retrieve weather data from api
public class WeatherApp {
    public static JSONObject getWeatherData(String locationName) {
        JSONArray locationData = getLocationData(locationName);

        // getting lat and long of from the json
        JSONObject location = (JSONObject) locationData.get(0);
        double latitude = (double) location.get("latitude");
        double longitude = (double) location.get("longitude");

        // Building an api request with latitude and longitude
        String urlString = "https://api.open-meteo.com/v1/forecast?latitude=" + latitude +
                "&longitude=" + longitude +
                "&hourly=temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m";

        try {
            HttpURLConnection connection = fetchApiResponse(urlString);

            if (connection.getResponseCode() != 200) {
                System.out.println("Error: Could not get response from API on weather data");
                return null;
            }

            StringBuilder resultJson = new StringBuilder();
            Scanner scanner = new Scanner(connection.getInputStream());

            while (scanner.hasNext()) {
                // read json into the string builder
                resultJson.append(scanner.nextLine());
            }

            scanner.close();
            connection.disconnect();

            JSONParser parser = new JSONParser();
            JSONObject resultsJson = (JSONObject) parser.parse(String.valueOf(resultJson));

            // retrieve hourly data
            JSONObject hourly = (JSONObject) resultsJson.get("hourly");

            JSONArray time = (JSONArray) hourly.get("time");
            int index = findIndexOfCurrentTime(time);

            JSONArray temperatureData = (JSONArray) hourly.get("temperature_2m");
            double temperature = (double) temperatureData.get(index);

            // get weather code
            JSONArray weathercode = (JSONArray) hourly.get("weather_code");
            String weatherCondition = convertWeatherCode((long) weathercode.get(index));

            // get humidity
            JSONArray relativeHumidity = (JSONArray) hourly.get("relative_humidity_2m");
            long humidity = (long) relativeHumidity.get(index);

            // get windspeed
            JSONArray windspeedData = (JSONArray) hourly.get("wind_speed_10m");
            double windspeed = (double) windspeedData.get(index);

            // build the weather json data that will be accessible to frontend
            JSONObject weatherData = new JSONObject();
            weatherData.put("temperature", temperature);
            weatherData.put("weather_condition", weatherCondition);
            weatherData.put("humidity", humidity);
            weatherData.put("windspeed", windspeed);

            return weatherData;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    // retrieves geographic coordinates for given location name
    public static JSONArray getLocationData(String locationName) {
        locationName = locationName.replaceAll(" ", "+");

        // Building API endpoint
        String urlString = "https://geocoding-api.open-meteo.com/v1/search?name=" +
                locationName + "&count=10&language=en&format=json";

        try {
            HttpURLConnection connection = fetchApiResponse(urlString);

            if (connection.getResponseCode() != 200) {
                System.out.println("Error: Could not connect to API.");
            } else {
                StringBuilder resultJson = new StringBuilder();
                Scanner scanner = new Scanner(connection.getInputStream());
                while (scanner.hasNext()) {
                    resultJson.append(scanner.nextLine());
                }

                scanner.close();

                connection.disconnect();

                JSONParser parser = new JSONParser();
                JSONObject resultsJsonObj = (JSONObject) parser.parse(String.valueOf(resultJson));

                JSONArray locationData = (JSONArray) resultsJsonObj.get("results");
                return locationData;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private static HttpURLConnection fetchApiResponse(String urlString) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("GET");

            connection.connect();
            return connection;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private static int findIndexOfCurrentTime(JSONArray timeList) {
        String currentTime = getCurrentTime();

        for (int i = 0; i < timeList.size(); i++) {
            String time = (String) timeList.get(i);
            if (time.equalsIgnoreCase(currentTime)) {
                return i;
            }
        }

        return 0;
    }

    public static String getCurrentTime() {
        LocalDateTime localDateTime = LocalDateTime.now(ZoneId.of("Asia/Kolkata"));

        // Convert the time to GMT
        LocalDateTime gmtDateTime = localDateTime.atZone(ZoneId.of("Asia/Kolkata"))
                .withZoneSameInstant(ZoneOffset.UTC)
                .toLocalDateTime();

        // Format the GMT time
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH':00'");
        String formattedDateTime = gmtDateTime.format(formatter);

        return formattedDateTime;
//
//        LocalDateTime localDateTime = LocalDateTime.now();
//
//        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH':00'");
//
//        String formattedDateTime = localDateTime.format(formatter);
//
//        return formattedDateTime;
    }

    private static String convertWeatherCode(long weatherCode) {
        String weatherCondition = "";
        if (weatherCode == 0) {
            weatherCondition = "Clear";
        } else if (weatherCode >= 1 && weatherCode <= 3) {
            weatherCondition = "Cloudy";
        } else if (weatherCode == 45 || weatherCode == 48) {
            weatherCondition =  "Cloudy";
        } else if ((weatherCode >= 51 && weatherCode <= 55) || (weatherCode >= 61 && weatherCode <= 67)) {
            weatherCondition = "Rain";
        } else if (weatherCode >= 71 && weatherCode <= 77) {
            weatherCondition = "Snow";
        } else if ((weatherCode >= 80 && weatherCode <= 82) || (weatherCode == 85 || weatherCode == 86)) {
            weatherCondition = "Rain";
        } else if (weatherCode == 95) {
            weatherCondition = "Rain";
        } else if (weatherCode == 96 || weatherCode == 99) {
            weatherCondition = "Snow";
        } else {
            weatherCondition = "Unknown weather condition";
        }
        return  weatherCondition;
    }

}
