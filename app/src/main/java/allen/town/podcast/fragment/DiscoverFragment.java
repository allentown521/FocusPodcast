package allen.town.podcast.fragment;

import static android.content.Context.MODE_PRIVATE;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.activity.result.contract.ActivityResultContracts.GetContent;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.widget.Toolbar;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.faltenreich.skeletonlayout.Skeleton;
import com.faltenreich.skeletonlayout.SkeletonLayoutUtils;
import com.wyjson.router.GoRouter;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import allen.town.core.service.PayService;
import allen.town.focus_common.util.DoubleClickBackToContentTopListener;
import allen.town.focus_common.util.Timber;
import allen.town.focus_common.util.TopSnackbarUtil;
import allen.town.podcast.MyApp;
import allen.town.podcast.R;
import allen.town.podcast.activity.ImportOPMLActivity;
import allen.town.podcast.activity.MainActivity;
import allen.town.podcast.adapter.ItunesAdapter;
import allen.town.podcast.core.storage.DBReader;
import allen.town.podcast.core.storage.DBTasks;
import allen.town.podcast.databinding.DiscoverLayoutBinding;
import allen.town.podcast.discovery.CombinedSearcher;
import allen.town.podcast.discovery.ItunesCategoryTopLoader;
import allen.town.podcast.discovery.PodcastSearchResult;
import allen.town.podcast.event.DiscoveryDefaultUpdateEvent;
import allen.town.podcast.event.FeedListUpdateEvent;
import allen.town.podcast.event.ItunesCategoryChangeEvent;
import allen.town.podcast.event.ShowSwitchCountryEvent;
import allen.town.focus_common.common.prefs.supportv7.dialogs.PreferenceListDialog;
import allen.town.podcast.model.feed.Feed;
import allen.town.podcast.model.feed.SortOrder;
import allen.town.podcast.util.SkeletonRecyclerDelay;
import allen.town.podcast.view.TagsSectionView;
import code.name.monkey.appthemehelper.util.scroll.ThemedFastScroller;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * Provides actions for adding new podcast subscriptions.
 */
public class DiscoverFragment extends Fragment implements Toolbar.OnMenuItemClickListener , DoubleClickBackToContentTopListener.IBackToContentTopView{

    public static final String TAG = "DiscoverFragment";
    private static final String KEY_UP_ARROW = "up_arrow";

    private DiscoverLayoutBinding viewBinding;
    private MainActivity activity;
    private boolean displayUpArrow;

    private SharedPreferences prefs;

    /**
     * Adapter responsible with the search results.
     */
    private ItunesAdapter adapter;
    private RecyclerView gridView;
    private TextView txtvError;
    private Button butRetry;
    private TextView txtvEmpty;

    /**
     * List of podcasts retreived from the search.
     */
    private List<PodcastSearchResult> topList;
    private Disposable disposable;
    private String countryCode = "US";
    private int categoryCode;
    private Skeleton skeleton;
    private SkeletonRecyclerDelay skeletonRecyclerDelay;

    /**
     * Replace adapter data with provided search results from SearchTask.
     *
     * @param result List of Podcast objects containing search results
     */
    private void updateData(List<PodcastSearchResult> result) {
        if (result != null && result.size() > 0) {
            gridView.setVisibility(View.VISIBLE);
            txtvEmpty.setVisibility(View.GONE);
        } else {
            gridView.setVisibility(View.GONE);
            txtvEmpty.setVisibility(View.VISIBLE);
        }
        adapter.addAll(result);
    }

    private final ActivityResultLauncher<String> chooseOpmlImportPathLauncher =
            registerForActivityResult(new GetContent(), this::chooseOpmlImportPathResult);
    private final ActivityResultLauncher<Uri> addLocalFolderLauncher =
            registerForActivityResult(new AddLocalFolder(), this::addLocalFolderResult);


    @Override
    @Nullable
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        viewBinding = DiscoverLayoutBinding.inflate(getLayoutInflater());
        activity = (MainActivity) getActivity();

        Toolbar toolbar = viewBinding.appBarLayout.getToolbar();
        toolbar.setOnMenuItemClickListener(this);
        toolbar.setTitle(R.string.discover);
        displayUpArrow = getParentFragmentManager().getBackStackEntryCount() != 0;
        if (savedInstanceState != null) {
            displayUpArrow = savedInstanceState.getBoolean(KEY_UP_ARROW);
        }
        ((MainActivity) getActivity()).setupToolbarToggle(toolbar, displayUpArrow);
        toolbar.inflateMenu(R.menu.add_subscription);

        View root = viewBinding.getRoot();
        gridView = root.findViewById(R.id.gridView);
        gridView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ItunesAdapter(getActivity(), new ArrayList<>(), true, false);
        gridView.setAdapter(adapter);
        toolbar.setOnClickListener(new DoubleClickBackToContentTopListener(this));
        skeleton = SkeletonLayoutUtils.applySkeleton(gridView, R.layout.item_small_recyclerview_skeleton, 15);
        skeletonRecyclerDelay = new SkeletonRecyclerDelay(skeleton, gridView);
        skeletonRecyclerDelay.showSkeleton();

        txtvError = root.findViewById(R.id.txtvError);
        butRetry = root.findViewById(R.id.butRetry);
        txtvEmpty = root.findViewById(android.R.id.empty);

        TagsSectionView tagsSectionView = root.findViewById(R.id.tags_view);
        tagsSectionView.setCurrentCode(categoryCode);

//        NestedScrollView nestedScrollView = root.findViewById(R.id.nested_scroll);
        ThemedFastScroller.create(gridView);
        //这里设置false，滑动有问题
//        nestedScrollView.setNestedScrollingEnabled(false);

        getSubedFeedsList();

        loadToplist(countryCode);
        return viewBinding.getRoot();
    }

    private Disposable getFeedsListDisposable;

    private void getSubedFeedsList() {
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
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putBoolean(KEY_UP_ARROW, displayUpArrow);
        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == R.id.addLocalFolder) {
            if (!MyApp.getInstance().checkSupporter(getContext(), true)) {
                return false;
            }
            try {
                addLocalFolderLauncher.launch(null);
            } catch (ActivityNotFoundException e) {
                TopSnackbarUtil.showSnack(getActivity(), R.string.unable_to_start_system_file_manager, Toast.LENGTH_LONG);
            }
            return true;
        } else if (itemId == R.id.opmlImport) {
            try {
                chooseOpmlImportPathLauncher.launch("*/*");
            } catch (ActivityNotFoundException e) {
                TopSnackbarUtil.showSnack(getActivity(), R.string.unable_to_start_system_file_manager, Toast.LENGTH_LONG);
            }
            return true;
        } else if (itemId == R.id.switch_country) {
            EventBus.getDefault().post(new ShowSwitchCountryEvent());
            return true;
        } else if (itemId == R.id.search) {
            performSearch();
            return true;
        }
        return false;
    }


    private void performSearch() {
        String query = "";

        activity.loadChildFragment(OnlineSearchFragment.newInstance(CombinedSearcher.class, query));
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //如果保留这个，那么切换暗黑模式后从opml添加文件失败，但是注释掉不知道是否有其他问题，看git更新记录提及较少
//        setRetainInstance(true);
        prefs = getActivity().getSharedPreferences(ItunesCategoryTopLoader.PREFS, MODE_PRIVATE);
        countryCode = prefs.getString(ItunesCategoryTopLoader.PREF_KEY_COUNTRY_CODE, Locale.getDefault().getCountry());
        categoryCode = prefs.getInt(ItunesCategoryTopLoader.PREF_KEY_CATEGORY_CODE, 0);
    }

    private void chooseOpmlImportPathResult(final Uri uri) {
        if (uri == null) {
            return;
        }
        final Intent intent = new Intent(getContext(), ImportOPMLActivity.class);
        intent.setData(uri);
        startActivity(intent);
    }

    private void addLocalFolderResult(final Uri uri) {
        if (uri == null) {
            return;
        }
        Observable.fromCallable(() -> addLocalFolder(uri))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        feed -> {
                            Fragment fragment = FeedItemlistFragment.newInstance(feed.getId());
                            ((MainActivity) getActivity()).loadChildFragment(fragment);
                        }, error -> {
                            Log.e(TAG, Log.getStackTraceString(error));
                            TopSnackbarUtil.showSnack(getActivity(), error.getLocalizedMessage(), Toast.LENGTH_LONG);
                        });
    }

    private void loadToplist(String country) {
        if (disposable != null) {
            disposable.dispose();
        }

        skeleton.showSkeleton();
        //先设置为可见，这时候只能看到skeleton，不会看到真实内容
        gridView.setVisibility(View.VISIBLE);
        txtvError.setVisibility(View.GONE);
        butRetry.setVisibility(View.GONE);
        txtvEmpty.setVisibility(View.GONE);

        ItunesCategoryTopLoader loader = new ItunesCategoryTopLoader(getContext());
        disposable = loader.loadToplist(country, 100, categoryCode).subscribe(
                podcasts -> {
                    skeletonRecyclerDelay.showOriginal();
                    topList = podcasts;
                    updateData(topList);
                }, error -> {
                    Log.e(TAG, Log.getStackTraceString(error));
                    skeletonRecyclerDelay.showOriginal();
                    if (error instanceof SocketTimeoutException
                            || error instanceof UnknownHostException) {
                        txtvError.setText(R.string.download_error_connection_error);
                    } else {
                        txtvError.setText(error.getMessage());
                    }
                    gridView.setVisibility(View.GONE);
                    txtvError.setVisibility(View.VISIBLE);
                    butRetry.setOnClickListener(v -> loadToplist(country));
                    butRetry.setVisibility(View.VISIBLE);
                });
    }

    public void showSwitchCountryDialog() {

        List<String> countryCodeArray = new ArrayList<String>(Arrays.asList(Locale.getISOCountries()));
        HashMap<String, String> countryCodeNames = new HashMap<String, String>();
        for (String code : countryCodeArray) {
            Locale locale = new Locale("", code);
            String countryName = locale.getDisplayCountry();
            if (countryName != null) {
                countryCodeNames.put(code, countryName);
            }
        }

        List<String> countryNamesSort = new ArrayList<String>(countryCodeNames.values());
        Collections.sort(countryNamesSort);

        final String[] countryList = (String[]) countryNamesSort.toArray(new String[]{});

        PreferenceListDialog preferenceListDialog = new PreferenceListDialog(getContext(),
                getString(R.string.pref_language_name));

        preferenceListDialog.setSelection(countryNamesSort.indexOf(countryCodeNames.get(countryCode)));

        preferenceListDialog.openDialog(countryList);
        preferenceListDialog.setOnPreferenceChangedListener(which -> {
            String countryName = countryList[which];

            for (Object o : countryCodeNames.keySet()) {
                if (countryCodeNames.get(o).equals(countryName)) {
                    countryCode = o.toString();
                    break;
                }
            }

            prefs.edit()
                    .putString(ItunesCategoryTopLoader.PREF_KEY_COUNTRY_CODE, countryCode)
                    .apply();

            EventBus.getDefault().post(new DiscoveryDefaultUpdateEvent());
            loadToplist(countryCode);

        });
    }

    private Feed addLocalFolder(Uri uri) {
        getActivity().getContentResolver()
                .takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        DocumentFile documentFile = DocumentFile.fromTreeUri(getContext(), uri);
        if (documentFile == null) {
            throw new IllegalArgumentException("Unable to retrieve document tree");
        }
        String title = documentFile.getName();
        if (title == null) {
            title = getString(R.string.local_folder);
        }
        Feed dirFeed = new Feed(Feed.PREFIX_LOCAL_FOLDER + uri.toString(), null, title);
        dirFeed.setItems(Collections.emptyList());
        dirFeed.setSortOrder(SortOrder.EPISODE_TITLE_A_Z);
        dirFeed.setSubscribed(true);

        if (!GoRouter.getInstance().getService(PayService.class).isPurchase(getContext(), false) && DBReader.getSubscribedFeedsCount() >= Feed.MAX_SUBSCRIBED_FEEDS_FOR_FREE) {
            Timber.w("The maximum number of subscriptions for the free version has been reached for local folder");
            throw new IllegalArgumentException(getString(R.string.limit_subs_notify));
        }

        Feed fromDatabase = DBTasks.updateFeed(getContext(), dirFeed, false);
        DBTasks.forceRefreshFeed(getContext(), fromDatabase, true);
        return fromDatabase;
    }

    @Override
    public void backToContentTop() {
        gridView.scrollToPosition(5);
        gridView.post(() -> gridView.smoothScrollToPosition(0));
    }


    private static class AddLocalFolder extends ActivityResultContracts.OpenDocumentTree {
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @NonNull
        @Override
        public Intent createIntent(@NonNull final Context context, @Nullable final Uri input) {
            return super.createIntent(context, input)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        if (disposable != null) {
            disposable.dispose();
        }

        if (getFeedsListDisposable != null) {
            getFeedsListDisposable.dispose();
        }
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void showSwitchCountry(ShowSwitchCountryEvent event) {
        showSwitchCountryDialog();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void changeItunesCategory(ItunesCategoryChangeEvent itunesCategoryChangeEvent) {
        categoryCode = itunesCategoryChangeEvent.getCode();
        prefs.edit().putInt(ItunesCategoryTopLoader.PREF_KEY_CATEGORY_CODE, categoryCode).apply();
        loadToplist(countryCode);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onFeedListChanged(FeedListUpdateEvent event) {
        //订阅状态发生改变
        getSubedFeedsList();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        EventBus.getDefault().register(this);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        EventBus.getDefault().unregister(this);
    }
}
