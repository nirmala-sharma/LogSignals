package com.nirmala.logsense;

import com.nirmala.logsense.model.LogModel;
import com.nirmala.logsense.parser.LogParser;
import com.sun.tools.javac.Main;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class MainApp {

public static void main(String[] args){
    Map<Instant, Integer> errorsPerMinute = new HashMap<>();

    try (InputStream is = Main.class
            .getClassLoader()
            .getResourceAsStream("logs/app.log")) {

        if (is == null) {
            throw new RuntimeException("Log file not found in resources");
        }
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(is, StandardCharsets.UTF_8));

        String line;
        while ((line = reader.readLine()) != null) {
               try{
                   // log parsing
                LogModel log = LogParser.parse(line);

                   // log aggregation
                 Aggregator aggregator = new Aggregator();
                 aggregator.add(log);

               } catch (Exception e) {
                System.err.println("Invalid log line: " + line);
            }
        }
    }
    catch (Exception e) {
        System.err.println(e.getMessage()) ;
    }

  }
}
