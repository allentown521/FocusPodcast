package allen.town.podcast.parser.feed.util;

import android.text.TextUtils;
import android.util.Log;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.TimeZones;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Pattern;
/**
 * Parses several date formats.
 */
public class ItunesEpisodesDateUtils {

    private ItunesEpisodesDateUtils() {

    }

    private static final String TAG = "ItunesEpisodesDateUtils";

    public static long getTime(String str) {
        return getTime(str, System.currentTimeMillis());
    }


    /* renamed from: q */
    public static final Pattern f19227q = Pattern.compile("\\.");

    /* renamed from: r */
    public static final Pattern f19228r = Pattern.compile("(-){2,}+");

    /* renamed from: s */
    public static final Pattern f19229s = Pattern.compile("( ){2,}+");

    /* renamed from: t */
    public static final Pattern f19230t = Pattern.compile("\\bSept\\b");

    public static long getTime(String str, long j) {
        String trim = null;
        if (str == null) {
            return j;
        }
        long j2 = -1;
        try {
            trim = str.trim();
        } catch (Throwable th) {
            Log.e(TAG,th.getMessage());
        }
        if (TextUtils.isEmpty(trim)) {
            return j;
        }
        str = f19230t.matcher(f19228r.matcher(f19229s.matcher(f19227q.matcher(trim.replace('/', '-')).replaceAll("")).replaceAll(StringUtils.SPACE)).replaceAll("-")).replaceAll("Sep");
        int indexOf = str.indexOf(44);
        if (indexOf > 0) {
            str = str.substring(indexOf + 1).trim();
        }
        if (!TextUtils.isEmpty(str)) {
            m10544u();
            for (ThreadLocal<SimpleDateFormat> threadLocal : f19218h) {
                if (threadLocal != null) {
                    try {
                        Date parse = threadLocal.get().parse(str);
                        if (parse != null) {
                            int year = parse.getYear();
                            if (year < -1800) {
                                parse.setYear(year + 1900);
                            }
                            j2 = parse.getTime();
                            break;
                        }
                        continue;
                    } catch (NumberFormatException | ParseException unused) {
                        continue;
                    }
                }
            }
        }
        if (m10904G1(j2)) {
            return j2;
        }
        Log.e(TAG,"Failed to convert String to Date: " + str);
        return j;
    }

    public static final long SIS_PING_INTERVAL = 2592000000L;

    public static boolean m10904G1(long j) {
        return j > SIS_PING_INTERVAL || j < -2592000000L;
    }

    public static List<ThreadLocal<SimpleDateFormat>> f19218h = null;
    public static final Object f19219i = new Object();
    public static final String[] f19220j = {"yyyy-MM-dd'T'HH:mm:ss", "dd MMM yy HH:mm:ss Z", "dd MMM yy HH:mm:ss z", "dd MMM yy HH:mm Z", "dd MMM yyyy HH:mm:ss Z", "dd MMM yyyy HH:mm:ss z", "dd MMM yyyy HH:mm:ss", "dd MMMM yyyy HH:mm:ss Z", "dd MMMM yyyy HH:mm:ss", "dd MMM yy HH:mm:ss", "MMM d HH:mm:ss yyyy", "dd MMM yyyy HH:mm Z", "dd MMM yyyy HH:mm zzzz", "dd MMM yyyy HH:mm", "dd MMMM yyyy HH:mm Z", "dd MMMM yyyy HH:mm", "dd MMM yy HH:mm", "MMM d HH:mm yyyy", "yyyy-MM-dd'T'HH:mm:ss.SSS Z", "yyyy-MM-dd'T'HH:mm:ss.SSS", "yyyy-MM-dd'T'HH:mm:ssZ", "yyyy-MM-dd'T'HH:mm:ss'Z'", "yyyy-MM-dd'T'HH:mm:ss.SSSzzzz", "yyyy-MM-dd'T'HH:mm:sszzzz", "yyyy-MM-dd'T'HH:mm:ss z", "yyyy-MM-dd'T'HH:mm:ssz", "yyyy-MM-dd'T'HHmmss.SSSz", "yyyy-MM-dd'T'HH:mm:ss.sZ", "yyyy-MM-dd'T'HH:mmZ", "yyyy-MM-ddZ", "yyyy-MM-dd", "dd MMM yyyy", "yyyyMMdd'T'HHmmssSSSZ"};
    public static void m10544u() {
        if (f19218h == null) {
            synchronized (f19219i) {
                if (f19218h == null) {
                    String[] strArr = f19220j;
                    ArrayList arrayList = new ArrayList(strArr.length * 2);
                    for (String str : strArr) {
                        arrayList.add(new C5612c(str));
                    }
                    for (String str2 : f19220j) {
                        arrayList.add(new C5613d(str2));
                    }
                    f19218h = arrayList;
                }
            }
        }
    }

    public static boolean m10563b(String str) {
        return TextUtils.equals(str, "yyyy-MM-dd") || TextUtils.equals(str, "dd MMM yyyy");
    }

    public static final TimeZone f19215e = TimeZone.getTimeZone(TimeZones.GMT_ID);
    public static class C5612c extends ThreadLocal<SimpleDateFormat> {

        /* renamed from: a */
        public final /* synthetic */ String f19233a;

        public C5612c(String str) {
            this.f19233a = str;
        }

        /* renamed from: a */
        public SimpleDateFormat initialValue() {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(this.f19233a, Locale.US);
            if (m10563b(this.f19233a)) {
                simpleDateFormat.setTimeZone(TimeZone.getDefault());
            } else {
                simpleDateFormat.setTimeZone(f19215e);
            }
            simpleDateFormat.setLenient(false);
            return simpleDateFormat;
        }
    }

    public static class C5613d extends ThreadLocal<SimpleDateFormat> {

        /* renamed from: a */
        public final /* synthetic */ String f19234a;

        public C5613d(String str) {
            this.f19234a = str;
        }

        /* renamed from: a */
        public SimpleDateFormat initialValue() {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat(this.f19234a, Locale.getDefault());
            if (m10563b(this.f19234a)) {
                simpleDateFormat.setTimeZone(TimeZone.getDefault());
            } else {
                simpleDateFormat.setTimeZone(f19215e);
            }
            simpleDateFormat.setLenient(false);
            return simpleDateFormat;
        }
    }
}
