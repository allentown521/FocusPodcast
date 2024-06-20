package allen.town.podcast.core.sync;

import allen.town.podcast.core.R;

public enum SynchronizationProviderViewData {
    GPODDER_NET(
            "GPODDER_NET",
            R.string.gpodnetauth_sum,
            R.drawable.gpodder_icon
    ),
    NEXTCLOUD_GPODDER(
            "NEXTCLOUD_GPODDER",
            R.string.gpodsync_synchronization_sum,
            R.drawable.nextcloud_logo
    );

    public static SynchronizationProviderViewData fromIdentifier(String provider) {
        for (SynchronizationProviderViewData synchronizationProvider : SynchronizationProviderViewData.values()) {
            if (synchronizationProvider.getIdentifier().equals(provider)) {
                return synchronizationProvider;
            }
        }
        return null;
    }

    private final String identifier;
    private final int iconResource;
    private final int summaryResource;

    SynchronizationProviderViewData(String identifier, int summaryResource, int iconResource) {
        this.identifier = identifier;
        this.iconResource = iconResource;
        this.summaryResource = summaryResource;
    }

    public String getIdentifier() {
        return identifier;
    }

    public int getIconResource() {
        return iconResource;
    }

    public int getSummaryResource() {
        return summaryResource;
    }
}
