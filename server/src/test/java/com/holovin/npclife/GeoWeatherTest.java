package com.holovin.npclife;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class GeoWeatherTest {
    @Test void geographyUsesSameAuthoritativeWeather(){
        var s=new WorldState();
        for(int d=1;d<=365;d+=7){
            s.day=d;s.minute=730;
            var original=WeatherLife.at(s.day,s.minute);
            var geo=GeoProjection.weather(s);
            assertEquals(original.season().name(),geo.season());
            assertEquals(original.sky().name(),geo.sky());
            assertEquals(original.temperatureC(),geo.temperatureC());
            assertEquals(original.raining(),geo.raining());
            assertEquals(original.snowing(),geo.snowing());
        }
    }
}
