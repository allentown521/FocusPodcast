package allen.town.podcast.model.feed;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import static allen.town.podcast.model.feed.SortOrder.Scope.INTER_FEED;
import static allen.town.podcast.model.feed.SortOrder.Scope.INTRA_FEED;

/**
 * Provides sort orders to sort a list of episodes.
 */
public enum SortOrder {
    DATE_NEW_OLD(1, INTRA_FEED,1,0),
    DATE_OLD_NEW(2, INTRA_FEED,1,0),
    EPISODE_TITLE_Z_A(3, INTRA_FEED,3,1),
    EPISODE_TITLE_A_Z(4, INTRA_FEED,3,1),
    DURATION_LONG_SHORT(5, INTRA_FEED,5,2),
    DURATION_SHORT_LONG(6, INTRA_FEED,5,2),
    FEED_TITLE_Z_A(101, INTER_FEED,101,3),
    FEED_TITLE_A_Z(102, INTER_FEED,101,3),
    RANDOM(103, INTER_FEED,0,4),
    //这个是假的，不然算法有问题，random也保持2个
    RANDOM_FAKE(104, INTER_FEED,0,4),
    SMART_SHUFFLE_NEW_OLD(105, INTER_FEED,105,5),
    SMART_SHUFFLE_OLD_NEW(106, INTER_FEED,105,5);

    public static int ASC_INDEX = 1;
    public static int DESC_INDEX = 0;

    public enum Scope {
        INTRA_FEED, INTER_FEED
    }

    public final int code;
    public final int ascBaseCode;
    public final int groupIndex;

    @NonNull
    public final Scope scope;

    SortOrder(int code, @NonNull Scope scope, int ascBaseCode, int groupIndex) {
        this.code = code;
        this.scope = scope;
        this.ascBaseCode = ascBaseCode;
        this.groupIndex = groupIndex;
    }

    /**
     * Converts the string representation to its enum value. If the string value is unknown,
     * the given default value is returned.
     */
    public static SortOrder parseWithDefault(String value, SortOrder defaultValue) {
        try {
            return valueOf(value);
        } catch (IllegalArgumentException e) {
            return defaultValue;
        }
    }

    @Nullable
    public static SortOrder fromCodeString(@Nullable String codeStr) {
        if (TextUtils.isEmpty(codeStr)) {
            return null;
        }
        int code = Integer.parseInt(codeStr);
        for (SortOrder sortOrder : values()) {
            if (sortOrder.code == code) {
                return sortOrder;
            }
        }
        throw new IllegalArgumentException("Unsupported code: " + code);
    }

    @Nullable
    public static String toCodeString(@Nullable SortOrder sortOrder) {
        return sortOrder != null ? Integer.toString(sortOrder.code) : null;
    }

    public static SortOrder[] valuesOf(String[] stringValues) {
        SortOrder[] values = new SortOrder[stringValues.length];
        for (int i = 0; i < stringValues.length; i++) {
            values[i] = SortOrder.valueOf(stringValues[i]);
        }
        return values;
    }
}
