package com.muhex.mumu

import android.content.Context
import android.content.pm.LauncherApps
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.Options

/**
 * A custom Coil Fetcher that loads app icons from the system given an [AppModel].
 */
class AppIconFetcher(
    private val context: Context,
    private val appModel: AppModel
) : Fetcher {

    override suspend fun fetch(): FetchResult? {
        val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        
        // Find the specific activity to get the icon
        val activityList = launcherApps.getActivityList(appModel.packageName, appModel.userHandle)
        val activity = activityList.find { it.name == appModel.className } ?: activityList.firstOrNull()
        
        val icon = activity?.getIcon(0) ?: context.packageManager.defaultActivityIcon

        return DrawableResult(
            drawable = icon,
            isSampled = false,
            dataSource = DataSource.DISK
        )
    }

    class Factory(private val context: Context) : Fetcher.Factory<AppModel> {
        override fun create(data: AppModel, options: Options, imageLoader: ImageLoader): Fetcher {
            return AppIconFetcher(context, data)
        }
    }
}
