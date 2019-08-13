package jigit.common;

import org.jetbrains.annotations.NotNull;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;


public enum CommitDateHelper {
    Instance;

    @NotNull
    private final ThreadLocal<SimpleDateFormat> formatterUTC = new ThreadLocal<SimpleDateFormat>() {
        @NotNull
        @Override
        protected SimpleDateFormat initialValue() {
            final SimpleDateFormat simpleDateFormat = new SimpleDateFormat();
            simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            return simpleDateFormat;
        }
    };
    @NotNull
    private final ThreadLocal<SimpleDateFormat> formatterLocal = new ThreadLocal<SimpleDateFormat>() {
        @NotNull
        @Override
        protected SimpleDateFormat initialValue() {
            return new SimpleDateFormat();
        }
    };

    @NotNull
    public Date toUTC(@NotNull Date date) throws ParseException {
        final String formatted = formatterUTC.get().format(date);
        return formatterLocal.get().parse(formatted);
    }

    @NotNull
    public Date toLocal(@NotNull Date date) throws ParseException {
        final String formatted = formatterLocal.get().format(date);
        return formatterUTC.get().parse(formatted);
    }
}
