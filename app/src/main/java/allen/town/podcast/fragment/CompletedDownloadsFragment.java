package allen.town.podcast.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.faltenreich.skeletonlayout.Skeleton;
import com.faltenreich.skeletonlayout.SkeletonLayoutUtils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

import allen.town.focus_common.util.DoubleClickBackToContentTopListener;
import allen.town.podcast.R;
import allen.town.podcast.activity.MainActivity;
import allen.town.podcast.adapter.EpisodeItemListAdapter;
import allen.town.podcast.adapter.MultiSelectAdapter;
import allen.town.podcast.core.event.DownloadEvent;
import allen.town.podcast.core.event.DownloadLogEvent;
import allen.town.podcast.core.service.download.DownloadService;
import allen.town.podcast.core.storage.DBReader;
import allen.town.podcast.core.util.FeedItemUtil;
import allen.town.podcast.core.util.download.AutoUpdateManager;
import allen.town.podcast.core.util.menuhandler.MenuItemUtils;
import allen.town.podcast.event.FeedItemEvent;
import allen.town.podcast.event.PlayerStatusEvent;
import allen.town.podcast.event.UnreadItemsUpdateEvent;
import allen.town.podcast.event.playback.PlaybackPositionEvent;
import allen.town.podcast.fragment.actions.EpisodeMultiSelectActionHandler;
import allen.town.podcast.menuprocess.FeedItemMenuProcess;
import allen.town.podcast.model.feed.FeedItem;
import allen.town.podcast.ui.common.PagedToolbarFragment;
import allen.town.podcast.util.SkeletonRecyclerDelay;
import allen.town.podcast.view.EmptyViewHandler;
import allen.town.podcast.viewholder.EpisodeItemViewHolder;
import code.name.monkey.appthemehelper.util.scroll.ThemedFastScroller;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * Displays all completed downloads and provides a button to delete them.
 */
public class CompletedDownloadsFragment extends Fragment implements DoubleClickBackToContentTopListener.IBackToContentTopView,
        EpisodeItemListAdapter.OnSelectModeListener {

    private static final String TAG = CompletedDownloadsFragment.class.getSimpleName();

    private List<FeedItem> items = new ArrayList<>();
    private CompletedDownloadsListAdapter adapter;
    private RecyclerView recyclerView;
    private Disposable disposable;
    private EmptyViewHandler emptyView;

    private boolean isUpdatingFeeds = false;
    private Skeleton skeleton;
    private SkeletonRecyclerDelay skeletonRecyclerDelay;


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.simple_list_fragment, container, false);

        recyclerView = root.findViewById(R.id.recyclerView);
        recyclerView.setRecycledViewPool(((MainActivity) getActivity()).getRecycledViewPool());
        adapter = new CompletedDownloadsListAdapter((MainActivity) getActivity());
        adapter.setOnSelectModeListener(this);
        recyclerView.setAdapter(adapter);
        ThemedFastScroller.create(recyclerView);

        adapter.setOnMenuItemClickListener(new MultiSelectAdapter.OnMenuItemClickListener() {
            @Override
            public void onMenuItemClick(MenuItem item) {
                new EpisodeMultiSelectActionHandler(((MainActivity) getActivity()), adapter.getSelectedItems())
                        .handleAction(item.getItemId());
                adapter.endSelectMode();
            }
        });


        addEmptyView();
        skeleton = SkeletonLayoutUtils.applySkeleton(recyclerView, R.layout.item_small_recyclerview_skeleton,15);
        skeletonRecyclerDelay = new SkeletonRecyclerDelay(skeleton,recyclerView);
        skeletonRecyclerDelay.showSkeleton();
        EventBus.getDefault().register(this);
        return root;
    }

    @Override
    public void onDestroyView() {
        EventBus.getDefault().unregister(this);
        adapter.endSelectMode();
        super.onDestroyView();
    }

    @Override
    public void onStart() {
        super.onStart();
        loadItems();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (disposable != null) {
            disposable.dispose();
        }
    }

    @Override
    public void onPrepareOptionsMenu(@NonNull Menu menu) {
        menu.findItem(R.id.clear_logs_item).setVisible(false);
        isUpdatingFeeds = MenuItemUtils.updateRefreshMenuItem(menu, R.id.refresh_item, updateRefreshMenuItemChecker);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.refresh_item) {
            AutoUpdateManager.runImmediate(requireContext());
            return true;
        }
        return false;
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onEventMainThread(DownloadEvent event) {
        if (event.hasChangedFeedUpdateStatus(isUpdatingFeeds)) {
            ((PagedToolbarFragment) getParentFragment()).invalidateOptionsMenuIfActive(this);
        }
    }

    private final MenuItemUtils.UpdateRefreshMenuItemChecker updateRefreshMenuItemChecker =
            () -> DownloadService.isRunning && DownloadService.isDownloadingFeeds();

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        FeedItem selectedItem = adapter.getLongPressedItem();
        if (selectedItem == null) {
            Log.i(TAG, "Selected item at current position was null, ignoring selection");
            return super.onContextItemSelected(item);
        }
        if (adapter.onContextItemSelected(item)) {
            return true;
        }

        return FeedItemMenuProcess.onMenuItemClicked(this, item.getItemId(), selectedItem);
    }

    private void addEmptyView() {
        emptyView = new EmptyViewHandler(getActivity());
        emptyView.setIcon(R.drawable.ic_download);
        emptyView.setTitle(R.string.no_comp_downloads_head_label);
        emptyView.attachToRecyclerView(recyclerView);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(FeedItemEvent event) {
        if (items == null) {
            return;
        } else if (adapter == null) {
            loadItems();
            return;
        }
        for (int i = 0, size = event.items.size(); i < size; i++) {
            FeedItem item = event.items.get(i);
            int pos = FeedItemUtil.indexOfItemWithId(items, item.getId());
            if (pos >= 0) {
                items.remove(pos);
                if (item.getMedia().isDownloaded()) {
                    items.add(pos, item);
                    adapter.notifyItemChangedCompat(pos);
                } else {
                    adapter.notifyItemRemoved(pos);
                }
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(PlaybackPositionEvent event) {
        if (adapter != null) {
            for (int i = 0; i < adapter.getItemCount(); i++) {
                EpisodeItemViewHolder holder = (EpisodeItemViewHolder) recyclerView.findViewHolderForAdapterPosition(i);
                if (holder != null && holder.isCurrentlyPlayingItem()) {
                    holder.notifyPlaybackPositionUpdated(event);
                    break;
                }
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPlayerStatusChanged(PlayerStatusEvent event) {
        loadItems();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onDownloadLogChanged(DownloadLogEvent event) {
        loadItems();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onUnreadItemsChanged(UnreadItemsUpdateEvent event) {
        loadItems();
    }

    private void loadItems() {
        if (disposable != null) {
            disposable.dispose();
        }
        emptyView.hide();
        disposable = Observable.fromCallable(DBReader::getDownloadedItems)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    items = result;
                    adapter.updateItems(result);
                    ((PagedToolbarFragment) getParentFragment()).invalidateOptionsMenuIfActive(this);
//                    progressBar.setVisibility(View.GONE);
                    if(skeleton.isSkeleton()) {
                        skeletonRecyclerDelay.showOriginal();
                    }
                }, error -> Log.e(TAG, Log.getStackTraceString(error)));
    }

    @Override
    public void onStartSelectMode() {
        ((PagedToolbarFragment) getParentFragment()).getToolbar().setVisibility(View.GONE);
    }

    @Override
    public void onEndSelectMode() {
        ((PagedToolbarFragment) getParentFragment()).getToolbar().setVisibility(View.VISIBLE);
    }

    @Override
    public void backToContentTop() {
        recyclerView.scrollToPosition(5);
        recyclerView.post(() -> recyclerView.smoothScrollToPosition(0));
    }

    private class CompletedDownloadsListAdapter extends EpisodeItemListAdapter {

        public CompletedDownloadsListAdapter(MainActivity mainActivity) {
            super(mainActivity,R.menu.downloads_action_menus);
        }

        @Override
        public void afterBindViewHolder(EpisodeItemViewHolder holder, int pos) {
            if (!inActionMode()) {
                holder.downloadedButton.setVisibility(View.GONE);
//                DeleteActionButton actionButton = new DeleteActionButton(getItem(pos));
//                actionButton.configure(holder.secondaryActionButton, holder.secondaryActionIcon, getActivity());
            }
        }

        @Override
        public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
            super.onCreateContextMenu(menu, v, menuInfo);
            if (!inActionMode()) {
                menu.findItem(R.id.multi_select).setVisible(true);
            }
            MenuItemUtils.setOnClickListeners(menu, CompletedDownloadsFragment.this::onContextItemSelected);
        }
    }
}
