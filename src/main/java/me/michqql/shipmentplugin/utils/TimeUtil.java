package me.michqql.shipmentplugin.utils;

import me.michqql.shipmentplugin.shipment.ShipmentManager;

import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZonedDateTime;

public class TimeUtil {

    public final static SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat("EEEE MMMM dd yyyy");

    public static long getStartOfToday() {
        return LocalDate.now().atStartOfDay(ShipmentManager.ZONE_ID).toInstant().toEpochMilli();
    }

    public static long getDayTimeStamp(DayOfWeek dow, int weeksDifference) {
        ZonedDateTime date = LocalDate.now().atStartOfDay(ShipmentManager.ZONE_ID);

        int dayOfWeekDifference = dow.getValue() - date.getDayOfWeek().getValue();
        if(dayOfWeekDifference < 0)
            dayOfWeekDifference += 7;

        long difference = dayOfWeekDifference + (weeksDifference * 7L);
        date = date.plusDays(difference);
        return date.toInstant().toEpochMilli();
    }
}
