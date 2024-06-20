package allen.town.podcast.core.util.comparator;

import java.util.Comparator;

import allen.town.podcast.model.feed.Chapter;

public class ChapterStartTimeComparator implements Comparator<Chapter> {

	@Override
	public int compare(Chapter lhs, Chapter rhs) {
		return Long.compare(lhs.getStart(), rhs.getStart());
	}

}
