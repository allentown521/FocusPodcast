package allen.town.podcast.parser.feed.namespace;

import allen.town.podcast.parser.feed.HandlerState;
import allen.town.podcast.parser.feed.element.SyndElement;
import org.xml.sax.Attributes;

public class Content extends Namespace {
    public static final String NSTAG = "content";
    public static final String NSURI = "http://purl.org/rss/1.0/modules/content/";

    private static final String ENCODED = "encoded";

    @Override
    public SyndElement handleElementStart(String localName, HandlerState state, Attributes attributes) {
        return new SyndElement(localName, this);
    }

    @Override
    public void handleElementEnd(String localName, HandlerState state) {
        if (ENCODED.equals(localName) && state.getCurrentItem() != null && state.getContentBuf() != null) {
            state.getCurrentItem().setDescriptionIfLonger(state.getContentBuf().toString());
        }
    }
}
