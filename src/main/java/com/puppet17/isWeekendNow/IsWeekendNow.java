package com.puppet17.isWeekendNow;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The type Is weekend now.
 *
 * @author PUPPET17
 */
public class IsWeekendNow {

    private static final String BASE_URL = "your_bark_server_url";
    private static final Logger LOGGER = Logger.getLogger(IsWeekendNow.class.getName());
    private static final int LUNCH_BREAK_MINUTES = 90;

    // 工作周开始时间（05:00）
    public static final LocalTime START_OF_WORK_WEEK = LocalTime.of(5, 0);

    public static final  LocalTime WORK_START_TIME = LocalTime.of(8, 0);

    // 工作日结束时间（19:00）
    public static final LocalTime END_OF_WORK_WEEK = LocalTime.of(19, 0);

    public static final LocalTime WORK_END_TIME = LocalTime.of(19, 0);

    public static final String ICON_URL = "https://s3.bmp.ovh/imgs/2024/11/07/01bdf18daa5b5d9c.jpg";

    /**
     * The entry point of application.
     *
     * @param args the input arguments
     */
    public static void main(String[] args) {

        LocalDateTime nextMonday = LocalDateTime.now()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .withHour(START_OF_WORK_WEEK.getHour())
                .withMinute(START_OF_WORK_WEEK.getMinute())
                .withSecond(0)
                .withNano(0);
        LocalDateTime nextFriday = LocalDateTime.now()
                .with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY))
                .withHour(END_OF_WORK_WEEK.getHour())
                .withMinute(END_OF_WORK_WEEK.getMinute())
                .withSecond(0)
                .withNano(0);

        LocalDateTime now = LocalDateTime.now();

        // 初始化消息
        StringBuilder messageBuilder = new StringBuilder();

        // 在工作时间内，且非周末时，计算今天剩余的工作时间
        if (now.toLocalTime().isBefore(WORK_END_TIME) && now.toLocalTime().isAfter(WORK_START_TIME) && now.getDayOfWeek().getValue() <= DayOfWeek.FRIDAY.getValue()) {
            Duration durationLeftToday = Duration.between(now.toLocalTime(), WORK_END_TIME).minusMinutes(LUNCH_BREAK_MINUTES);
            long hoursLeftToday = durationLeftToday.toHours();
            long minutesLeftToday = durationLeftToday.minusHours(hoursLeftToday).toMinutes();

            // 今日工作进度
            messageBuilder.append("📅 本日工作进度:\n")
                    .append(String.format("  ⏳ 剩余时长: %d小时%d分钟\n", hoursLeftToday, minutesLeftToday));

            long totalWorkMinutesToday = Duration.between(WORK_START_TIME, WORK_END_TIME).toMinutes();
            double percentOfDayCompleted = 100 - ((double) durationLeftToday.toMinutes() / totalWorkMinutesToday * 100);
            messageBuilder.append(String.format("  🔄 完成进度: %.2f%%\n", percentOfDayCompleted));
        } else {
            messageBuilder.append("🏖️ 当前不是工作时间哦~~\n");
        }

        // 计算本周工作时间进度
        messageBuilder.append("\n📅 本周工作进度:\n");

        long totalWorkMinutesThisWeek = Duration.between(nextMonday, nextFriday).toMinutes();
        long minutesWorkedSoFarThisWeek = now.isAfter(nextMonday) ? Duration.between(nextMonday, now).toMinutes() : 0;
        double percentOfWorkWeekCompleted = totalWorkMinutesThisWeek > 0 ? (double) minutesWorkedSoFarThisWeek / totalWorkMinutesThisWeek * 100 : 0;
        // 计算到周末的剩余时间
        if (!(now.getDayOfWeek() == DayOfWeek.SATURDAY || now.getDayOfWeek() == DayOfWeek.SUNDAY)) {
            Duration untilWeekend = Duration.between(now, nextFriday);
            long hoursUntilWeekend = untilWeekend.toHours();
            long minutesUntilWeekend = untilWeekend.minusHours(hoursUntilWeekend).toMinutes();

            messageBuilder.append(String.format("  ⏳ 剩余时长: %d小时%d分钟\n", hoursUntilWeekend, minutesUntilWeekend));
        }
        messageBuilder.append(String.format("  🔄 完成进度: %.2f%%\n", percentOfWorkWeekCompleted));

        // 推送至Bark
        sendToBark(messageBuilder.toString());
        LOGGER.info("推送消息: " + messageBuilder);
    }

    private static void sendToBark(String message) {
        try {
            String encodedMessage = URLEncoder.encode(message, "UTF-8");
            String sound = "paymentsuccess";
            String group = "work";
            String finalUrl = String.format("%s%s?sound=%s&group=%s&icon=%s",
                    BASE_URL, encodedMessage, sound, group, URLEncoder.encode(ICON_URL, "UTF-8"));

            URL url = new URL(finalUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                LOGGER.info("通知发送成功: HTTP " + responseCode);
            } else {
                LOGGER.warning("通知发送失败: HTTP " + responseCode);
            }
            connection.disconnect();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "发送通知时发生错误", e);
        }
    }
}