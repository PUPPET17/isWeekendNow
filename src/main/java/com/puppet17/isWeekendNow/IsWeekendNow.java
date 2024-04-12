package com.puppet17.isWeekendNow;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;

/**
 * The type Is weekend now.
 *
 * @author PUPPET17
 */
public class IsWeekendNow {

    /**
     * The entry point of application.
     *
     * @param args the input arguments
     */
    public static void main(String[] args) {
        // 工作周开始时间（周一05:00）
        LocalTime startOfWorkWeek = LocalTime.of(5, 0);
        // 工作日结束时间（周五19:00）
        LocalTime endOfWorkWeek = LocalTime.of(19, 0);

        LocalTime workStart = LocalTime.of(8, 0);
        LocalTime workEnd = LocalTime.of(19, 0);

        LocalDateTime nextMonday = LocalDateTime.now()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .withHour(startOfWorkWeek.getHour())
                .withMinute(startOfWorkWeek.getMinute())
                .withSecond(0)
                .withNano(0);
        LocalDateTime nextFriday = LocalDateTime.now()
                .with(TemporalAdjusters.nextOrSame(DayOfWeek.FRIDAY))
                .withHour(endOfWorkWeek.getHour())
                .withMinute(endOfWorkWeek.getMinute())
                .withSecond(0)
                .withNano(0);

        LocalDateTime now = LocalDateTime.now();

        // 初始化消息
        String message;

        // 在工作时间内，且非周末时，计算今天剩余的工作时间
        if (now.toLocalTime().isBefore(workEnd) && now.toLocalTime().isAfter(workStart) && now.getDayOfWeek().getValue() <= DayOfWeek.FRIDAY.getValue()) {
            Duration durationLeftToday = Duration.between(now.toLocalTime(), workEnd);
            long hoursLeftToday = durationLeftToday.toHours();
            long minutesLeftToday = durationLeftToday.minusHours(hoursLeftToday).toMinutes();
            message = String.format("今天工作时长还剩: %dh%dm", hoursLeftToday, minutesLeftToday);
            System.out.println(message);

            // 计算剩余工作时长的百分比
            long totalWorkMinutesToday = Duration.between(workStart, workEnd).toMinutes();
            long minutesLeftTodayPercent = durationLeftToday.toMinutes();
            double percentOfDayCompleted = 100 - ((double) minutesLeftTodayPercent / totalWorkMinutesToday * 100);
            message += String.format(", 已进行: %.2f%%", percentOfDayCompleted);
        } else {
            message = "不是工作时间哦~~";
        }

        // 如果下班后，计算下周的工作时间
        if (now.isAfter(nextFriday)) {
            nextMonday = now.with(TemporalAdjusters.next(DayOfWeek.MONDAY))
                    .withHour(startOfWorkWeek.getHour())
                    .withMinute(startOfWorkWeek.getMinute())
                    .withSecond(0)
                    .withNano(0);
            nextFriday = nextMonday.with(TemporalAdjusters.next(DayOfWeek.FRIDAY))
                    .withHour(endOfWorkWeek.getHour())
                    .withMinute(endOfWorkWeek.getMinute())
                    .withSecond(0)
                    .withNano(0);
        }

        // 计算本周剩余工作时间
        long totalWorkMinutesThisWeek = Duration.between(nextMonday, nextFriday).toMinutes();
        long minutesWorkedSoFarThisWeek = now.isAfter(nextMonday) ? Duration.between(nextMonday, now).toMinutes() : 0;
        double percentOfWorkWeekCompleted = totalWorkMinutesThisWeek > 0 ? (double) minutesWorkedSoFarThisWeek / totalWorkMinutesThisWeek * 100 : 0;
        message += String.format(", 本周已进行: %.3f%%", percentOfWorkWeekCompleted);

        // 计算到周末的剩余时间
        if (!(now.getDayOfWeek() == DayOfWeek.SATURDAY || now.getDayOfWeek() == DayOfWeek.SUNDAY)) {
            Duration untilWeekend = Duration.between(now, nextFriday);
            long hoursUntilWeekend = untilWeekend.toHours();
            long minutesUntilWeekend = untilWeekend.minusHours(hoursUntilWeekend).toMinutes();
            message += String.format(", 本周时长还剩: %dh%dm", hoursUntilWeekend, minutesUntilWeekend);
        }

        // 推送至Bark
        sendToBark(message);
        System.out.println(message);
    }

    private static void sendToBark(String message) {
        try {
            String baseURL = "http://117.72.16.190/PvVHpAR8Zm7yfoQxisDeYC/";
            String encodedMessage = URLEncoder.encode(message, "UTF-8");
            String sound = "paymentsuccess";
            String group = "work";
            String finalUrl = String.format("%s%s?sound=%s&group=%s", baseURL, encodedMessage, sound, group);

            URL url = new URL(finalUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                System.out.println("通知发送成功: HTTP " + responseCode);
            } else {
                System.out.println("通知发送失败: HTTP " + responseCode);
            }
            connection.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("通知发送异常");
        }
    }
}