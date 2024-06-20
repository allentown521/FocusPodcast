/**
 * This file is part of .
 * <p>
 * is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * If you own a pjsip commercial license you can also redistribute it
 * and/or modify it under the terms of the GNU Lesser General Public License
 * as an android library.
 * <p>
 * is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with .  If not, see <http://www.gnu.org/licenses/>.
 */

package allen.town.podcast.util;

import android.os.Environment;

import java.io.File;

import allen.town.podcast.MyApp;


public final class CustomDistribution {

    public static final String SDCARD_ROOT = MyApp.getInstance().getExternalFilesDir(null).getPath();
    public static final String SDCARD_CACHE_ROOT = MyApp.getInstance().getExternalCacheDir().getPath();

    public final static File UPDATE_DOWNLOAD_DIR = MyApp.getInstance().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
    public final static String PRIVATE_FILE_PATH = MyApp.getInstance().getFilesDir().getPath();
    final static String LOGS = "logs";
    public static final String NET_DISK_ROOT = SDCARD_ROOT + File.separator + "CloudDrive";
    public static final String NET_DISK_PICTURE = NET_DISK_ROOT + File.separator + "picture";
    public static final String NET_DISK_VIDEO = NET_DISK_ROOT + File.separator + "video";
    public static final String NET_DISK_AUDIO = NET_DISK_ROOT + File.separator + "audio";
    public static final String NET_DISK_DOCUMENT = NET_DISK_ROOT + File.separator + "document";
    private static final String APP_CACHE_DIRNAME = "/webcache";
    public static final String SDCARD_TEMP_ROOT = SDCARD_ROOT + File.separator + "tmp";

    public static final String FONT_DIR = SDCARD_ROOT + File.separator + "font";
    public static final String LOCAL_BACKUP_DIR = SDCARD_ROOT + File.separator + "backup";

    public static final String RECORDER = "Recorder";
    public static final String VIDEOS = "videos";
    public static final String MIGRATE_ZIP = "migrate.zip";
    public static final String MIGRATE_JSON = "migrate.json";


    //begin:xlog 放在data目录是防止用户清理了日志
    final static String XLOG_CACHE_FOLDER = PRIVATE_FILE_PATH + File.separator + "xlogCache";
    public final static String XLOG_FOLDER = PRIVATE_FILE_PATH
            + File.separator + "xlogs";
    final static String XLOG_NAMEPREFIX = "main";
    final static String XLOG_CORE_NAMEPREFIX = "core";
    //end:xlog


    public final static String ZIP_FOLDER = SDCARD_ROOT + File.separator + "zip";
    public final static String LOGS_FOLDER = SDCARD_ROOT
            + File.separator + LOGS;

    //增加一个新的文件夹的话在{@link FOLDER_LIST}中声明，程序自动创建
    public final static String VIDEO_FOLDER = SDCARD_ROOT
            + File.separator + "videos";
    public final static String RECORD_FOLDER = SDCARD_ROOT
            + File.separator + RECORDER;
    //这里改成data目录后7.0版本以下无法拍照,不能随意修改,此外从sdcard只能拷贝到data,rename会失败
    public final static String IMAGE = "Image";
    public final static String IMAGE_FOLDER = SDCARD_ROOT + File.separator + IMAGE;
    private final static String ROTATE_IMAGE_FOLDER = SDCARD_ROOT + File.separator + "rotateImage";
    // end:Added by Hanker 2013-06-17
    public final static String ICONS_FOLDER = SDCARD_ROOT
            + File.separator + "icons";
    public final static String RSS_IMAGE_FOLDER = SDCARD_ROOT
            + File.separator + "image2";
    public final static String EXPRESSION_COVER_FOLDER = SDCARD_ROOT
            + File.separator + "expression_cover";
    public final static String SYSAPPS_IMAGE_FOLDER = SDCARD_ROOT
            + File.separator + "sysApps";
    public final static String CIRCLE_FOLDER = SDCARD_ROOT
            + File.separator + "Circle";
    //这里必须保存到sdcard中
    public final static String PICTURE_FOLDER = SDCARD_ROOT
            + File.separator + "picture";
    public final static String AD_FOLDER = SDCARD_ROOT
            + File.separator + "ad";
    private final static String COLLECT_VEDIO_FOLDER = SDCARD_ROOT
            + File.separator + "collectVedio";
    private final static String COLLECT_IMG_FOLDER = SDCARD_ROOT
            + File.separator + "collectImg";
    public final static String AD_PAGE_PATH = AD_FOLDER + File.separator + "homepage.jpg";
    public final static String AD_NEAR_PAGE_PATH = AD_FOLDER + File.separator + "nearAd.jpg";
    public final static String TMP_FOLDER = SDCARD_ROOT
            + File.separator + "tmp";

    // added by txp,2014-2-19
    public final static String AVATAR_FOLDER = SDCARD_ROOT
            + File.separator + "avatar";


    private CustomDistribution() {
    }

    //  trunk distribution

    /**
     * Does this distribution allow to create other accounts than the one of the
     * distribution
     *
     * @return Whether other accounts can be created
     */
    public static boolean distributionWantsOtherAccounts() {
        return true;
    }

    /**
     * Does this distribution allow to list other providers in other accounts
     * creation
     *
     * @return Whether other provider are listed is wizard picker
     */
    public static boolean distributionWantsOtherProviders() {
        return true;
    }

    /**
     * Email address for support and feedback If none return the feedback
     * feature is disabled
     *
     * @return the email address of support
     */
    public static String getSupportEmail() {
        return "developers@.com";
    }


    /**
     * The default wizard info for this distrib. If none no custom distribution
     * wizard is shown
     *
     * @return the default wizard info
    //     */
//    public static WizardInfo getCustomDistributionWizard() {
//        // modified by txp,2013-5-7 默认向导为nv
//        return WizardUtils.getWizardClass(WizardUtils.NV_WIZARD_TAG);
//    }

    /**
     * Show or not the issue list in help
     *
     * @return whether link to issue list should be displayed
     */
    public static boolean showIssueList() {
        return true;
    }

    /**
     * Get the link to the FAQ. If null or empty the link to FAQ is not
     * displayed
     *
     * @return link to the FAQ
     */
    public static String getFaqLink() {
        return "http://code.google.com/p//wiki/FAQ?show=content,nav#Summary";
    }


    /**
     * Do we want to display messaging feature
     *
     * @return true if the feature is enabled in this distribution
     */
    public static boolean supportMessaging() {
        return true;
    }

    /**
     * Do we want to display the favorites feature
     *
     * @return true if the feature is enabled in this distribution
     */
    public static boolean supportFavorites() {
        return true;
    }

    /**
     * Do we want to display record call option while in call If true the record
     * of conversation will be enabled both in ongoing call view and in settings
     * as "auto record" feature
     *
     * @return true if the feature is enabled in this distribution
     */
    public static boolean supportCallRecord() {
        return true;
    }

    /**
     * Shall we force the no mulitple call feature to be set to false
     *
     * @return true if we don't want to support multiple calls at all.
     */
    public static boolean forceNoMultipleCalls() {
        return false;
    }

    /**
     * Should the wizard list display a given generic wizard
     *
     * @param wizardTag the tag of the generic wizard
     * @return true if you'd like the wizard to be listed
     */
    public static boolean distributionWantsGeneric(String wizardTag) {
        return true;
    }


    // begin:Added by Hanker 2013-06-17

    /**
     * Get the recording folder name. This folder will be used to store records
     *
     * @return the name of the folder to use
     */
    public static String getRecordFolder() {
        return RECORD_FOLDER;
    }

    public static String getTmpFolder() {
        return TMP_FOLDER;
    }

    // begin:Added by Hanker 2013-07-10

    /**
     * Get the rotateImage folder name. This folder will be used to store
     * records
     *
     * @return the name of the folder to use
     */
    public static String getrotateImageFolder() {
        return ROTATE_IMAGE_FOLDER;
    }
}