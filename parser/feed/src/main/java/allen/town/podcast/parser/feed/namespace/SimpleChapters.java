package allen.town.podcast.parser.feed.namespace;

import android.text.TextUtils;
import android.util.Log;

import allen.town.podcast.model.feed.Chapter;
import allen.town.podcast.parser.feed.HandlerState;
import allen.town.podcast.parser.feed.element.SyndElement;
import allen.town.podcast.parser.feed.util.DateUtils;
import org.xml.sax.Attributes;

import java.util.ArrayList;

import allen.town.podcast.model.feed.FeedItem;

public class SimpleChapters extends Namespace {
    private static final String TAG = "NSSimpleChapters";

    public static final String NSTAG = "psc|sc";
    public static final String NSURI = "http://podlove.org/simple-chapters";

    private static final String CHAPTERS = "chapters";
    private static final String CHAPTER = "chapter";
    private static final String START = "start";
    private static final String TITLE = "title";
    private static final String HREF = "href";
    private static final String IMAGE = "image";

    @Override
    public SyndElement handleElementStart(String localName, HandlerState state, Attributes attributes) {
        FeedItem currentItem = state.getCurrentItem();
        if (currentItem != null) {
            if (localName.equals(CHAPTERS)) {
                currentItem.setChapters(new ArrayList<>());
            } else if (localName.equals(CHAPTER) && !TextUtils.isEmpty(attributes.getValue(START))) {
                // if the chapter's START is empty, we don't need to do anything
                try {
                    long start = DateUtils.parseTimeString(attributes.getValue(START));
                    String title = attributes.getValue(TITLE);
                    String link = attributes.getValue(HREF);
                    String imageUrl = attributes.getValue(IMAGE);
                    Chapter chapter = new Chapter(start, title, link, imageUrl);
                    currentItem.getChapters().add(chapter);
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Unable to read chapter", e);
                }
            }
        }
        return new SyndElement(localName, this);
    }

    @Override
    public void handleElementEnd(String localName, HandlerState state) {
    }

}
