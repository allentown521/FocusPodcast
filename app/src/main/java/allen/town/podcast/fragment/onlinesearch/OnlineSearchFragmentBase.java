package allen.town.podcast.fragment.onlinesearch;

import static android.widget.AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.faltenreich.skeletonlayout.Skeleton;
import com.faltenreich.skeletonlayout.SkeletonLayoutUtils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

import allen.town.focus_common.util.Timber;
import allen.town.podcast.R;
import allen.town.podcast.adapter.ItunesAdapter;
import allen.town.podcast.core.storage.DBReader;
import allen.town.podcast.discovery.PodcastSearchResult;
import allen.town.podcast.discovery.PodcastSearcher;
import allen.town.podcast.event.FeedListUpdateEvent;
import allen.town.podcast.event.SearchOnlineEvent;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public abstract class OnlineSearchFragmentBase extends Fragment {
    private static final String TAG = "OnlineSearchBase";
    /**
     * Adapter responsible with the search results
     */
    protected ItunesAdapter adapter;
    protected PodcastSearcher searchProvider;
    protected RecyclerView gridView;
    protected ProgressBar progressBar;
    protected TextView txtvError;
    protected Button butRetry;
    protected TextView txtvEmpty;

    /**
     * List of podcasts retreived from the search
     */
    private List<PodcastSearchResult> searchResults;
    protected Disposable disposable;
    private Skeleton skeleton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.fragment_online_search, container, false);
        gridView = root.findViewById(R.id.gridView);
        gridView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ItunesAdapter(getActivity(), new ArrayList<>(), true,isTypeEpisodes());
        gridView.setAdapter(adapter);
        skeleton = SkeletonLayoutUtils.applySkeleton(gridView, R.layout.item_small_recyclerview_skeleton,15);
        skeleton.showSkeleton();

        //Show information about the podcast when the list item is clicked
        progressBar = root.findViewById(R.id.progressBar);
        txtvError = root.findViewById(R.id.txtvError);
        butRetry = root.findViewById(R.id.butRetry);
        txtvEmpty = root.findViewById(android.R.id.empty);

        gridView.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                if (newState == SCROLL_STATE_TOUCH_SCROLL) {
                    InputMethodManager imm = (InputMethodManager)
                            getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(gridView.getWindowToken(), 0);
                }
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
            }
        });

        getSubedFeedsList();
        EventBus.getDefault().register(this);
        return root;
    }

    private void getSubedFeedsList(){
        getFeedsListDisposable = Observable.fromCallable(DBReader::getFeedList)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        feeds -> {
                            adapter.setSubscribedFeeds(feeds);
                            //下载完成了，数据库中已有该feed了
                        }, error -> Log.e(TAG, Log.getStackTraceString(error))
                );
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Timber.e("disposable " + disposable);
        if (disposable != null) {
            disposable.dispose();
        }

        if(getFeedsListDisposable != null){
            getFeedsListDisposable.dispose();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void searchOnlineEvent(SearchOnlineEvent event) {
        search(event.getQuery());
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onFeedListChanged(FeedListUpdateEvent event) {
        //订阅状态发生改变
        getSubedFeedsList();
    }

    public void search(String query) {
        if (disposable != null) {
            disposable.dispose();
        }

        showOnlyProgressBar();
        disposable = searchProvider.search(query).subscribe(result -> {
            searchResults = result;
            skeleton.showOriginal();
//            progressBar.setVisibility(View.GONE);
            adapter.addAll(searchResults);
//            gridView.setVisibility(!searchResults.isEmpty() ? View.VISIBLE : View.GONE);
            txtvEmpty.setVisibility(searchResults.isEmpty() ? View.VISIBLE : View.GONE);
            //这里直接用fragment的context，第一次搜索正常返回再搜索就闪退了，懒得找原因了
            txtvEmpty.setText(txtvEmpty.getContext().getString(R.string.no_results_for_query, query));
        }, error -> {
            Log.e(TAG, Log.getStackTraceString(error));
            skeleton.showOriginal();
//            progressBar.setVisibility(View.GONE);
            txtvError.setText(error.toString());
            txtvError.setVisibility(View.VISIBLE);
            butRetry.setOnClickListener(v -> search(query));
            butRetry.setVisibility(View.VISIBLE);
        });
    }

    private Disposable getFeedsListDisposable;

    private void showOnlyProgressBar() {
//        gridView.setVisibility(View.GONE);
        txtvError.setVisibility(View.GONE);
        butRetry.setVisibility(View.GONE);
        txtvEmpty.setVisibility(View.GONE);
//        progressBar.setVisibility(View.VISIBLE);
        skeleton.showSkeleton();
    }

    abstract boolean isTypeEpisodes();
}
