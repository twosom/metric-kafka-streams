package com.icloud.pipeline;

import com.google.gson.JsonParser;
import org.apache.kafka.streams.kstream.Predicate;

public class MetricJsonUtils {

    public static Predicate<String, String> getPredicateByTotalCPUUsage(Double usage) {
        return (key, value) -> getTotalCPUPercent(value) > usage;
    }

    public static Predicate<String, String> getPredicateByResource(String target) {
        return (key, value) -> getMetricName(value).equals(target);
    }

    private static double getTotalCPUPercent(String value) {
        return JsonParser.parseString(value).getAsJsonObject()
                .get("system").getAsJsonObject()
                .get("cpu").getAsJsonObject()
                .get("total").getAsJsonObject()
                .get("norm").getAsJsonObject()
                .get("pct").getAsDouble();
    }

    private static String getMetricName(String value) {
        return JsonParser.parseString(value).getAsJsonObject()
                .get("metricset").getAsJsonObject()
                .get("name").getAsString();
    }

    public static String getHostTimestamp(String value) {
        var objectValue = JsonParser.parseString(value).getAsJsonObject();
        var result = objectValue.getAsJsonObject("host");
        result.add("timestamp", objectValue.get("@timestamp"));
        return result.toString();
    }
}
