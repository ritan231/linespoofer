package my.shanunid.linespoofer.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.collection.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.graphics.Bitmap

object MdSpacing {
    val xxs = 4.dp
    val xs = 8.dp
    val sm = 16.dp
    val md = 24.dp
    val lg = 32.dp
    val xl = 48.dp
}

@Composable
fun SectionTitle(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        modifier = modifier.padding(top = MdSpacing.xs, bottom = MdSpacing.xs),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
fun ExpressiveSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SectionTitle(title)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = MdSpacing.sm, vertical = 12.dp),
                content = content,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ConnectedSettingsRow(
    index: Int,
    count: Int,
    title: String,
    summary: String,
    icon: ImageVector? = null,
    onClick: (() -> Unit)? = null,
    trailingText: String? = null,
    checked: Boolean? = null,
    onCheckedChange: ((Boolean) -> Unit)? = null,
) {
    val modifier = segmentedGapModifier(index, count)
    val shapes = connectedListItemShapes(index, count)
    val colors = ListItemDefaults.segmentedColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
        leadingContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        trailingContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        supportingContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    val leadingContent: (@Composable () -> Unit)? = icon?.let {
        {
            Icon(
                imageVector = it,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
            )
        }
    }
    val supportingContent: (@Composable () -> Unit)? = summary.takeIf(String::isNotBlank)?.let {
        {
            Text(
                text = it,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
    val trailingContent: (@Composable () -> Unit)? = when {
        checked != null && onCheckedChange != null -> {
            { Switch(checked = checked, onCheckedChange = onCheckedChange) }
        }
        !trailingText.isNullOrBlank() -> {
            {
                Text(
                    text = trailingText,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        onClick != null -> {
            {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
        else -> null
    }

    when {
        checked != null && onCheckedChange != null -> {
            SegmentedListItem(
                onClick = { onCheckedChange(!checked) },
                shapes = shapes,
                modifier = modifier,
                leadingContent = leadingContent,
                trailingContent = trailingContent,
                supportingContent = supportingContent,
                colors = colors,
            ) {
                Text(title)
            }
        }
        onClick != null -> {
            SegmentedListItem(
                onClick = onClick,
                shapes = shapes,
                modifier = modifier,
                leadingContent = leadingContent,
                trailingContent = trailingContent,
                supportingContent = supportingContent,
                colors = colors,
            ) {
                Text(title)
            }
        }
        else -> {
            SegmentedListItem(
                shapes = shapes,
                modifier = modifier,
                leadingContent = leadingContent,
                trailingContent = trailingContent,
                supportingContent = supportingContent,
                colors = colors,
            ) {
                Text(title)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ConnectedInfoRow(
    index: Int,
    count: Int,
    label: String,
    value: String,
) {
    SegmentedListItem(
        shapes = connectedListItemShapes(index, count),
        modifier = segmentedGapModifier(index, count),
        supportingContent = { Text(value) },
        colors = ListItemDefaults.segmentedColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface,
            supportingContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    ) {
        Text(label)
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = MdSpacing.xs),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(MdSpacing.xxs))
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

private fun segmentedGapModifier(index: Int, count: Int): Modifier {
    return if (index < count - 1) {
        Modifier.padding(bottom = ListItemDefaults.SegmentedGap)
    } else {
        Modifier
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun connectedListItemShapes(index: Int, count: Int) = if (count == 1) {
    ListItemDefaults.segmentedShapes(
        index = index,
        count = count,
        defaultShapes = ListItemDefaults.shapes(
            shape = MaterialTheme.shapes.large,
            selectedShape = MaterialTheme.shapes.large,
        ),
    )
} else {
    ListItemDefaults.segmentedShapes(index, count)
}

object AppIconLoader {
    private val memoryCache = LruCache<String, ImageBitmap>(120)

    fun getCached(packageName: String): ImageBitmap? = memoryCache[packageName]

    fun loadIcon(context: android.content.Context, packageName: String): ImageBitmap? {
        memoryCache[packageName]?.let { return it }
        val cleanPkg = packageName.substringBefore("#")
        return try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(cleanPkg, 0)
            val drawable = appInfo.loadIcon(pm)
            val bitmap = drawable.toBitmap(96, 96)
            val imageBitmap = bitmap.asImageBitmap()
            memoryCache.put(packageName, imageBitmap)
            imageBitmap
        } catch (t: Throwable) {
            null
        }
    }
}

@Composable
fun AppIcon(
    packageName: String,
    modifier: Modifier = Modifier,
    fallbackBitmap: Bitmap? = null,
) {
    val context = LocalContext.current
    var imageBitmap by remember(packageName) {
        mutableStateOf(AppIconLoader.getCached(packageName))
    }

    LaunchedEffect(packageName) {
        if (imageBitmap == null) {
            withContext(Dispatchers.IO) {
                val loaded = AppIconLoader.loadIcon(context, packageName)
                if (loaded != null) {
                    imageBitmap = loaded
                }
            }
        }
    }

    val current = imageBitmap
    if (current != null) {
        Image(
            bitmap = current,
            contentDescription = null,
            modifier = modifier,
        )
    } else if (fallbackBitmap != null) {
        val cachedFallback = remember(fallbackBitmap) { fallbackBitmap.asImageBitmap() }
        Image(
            bitmap = cachedFallback,
            contentDescription = null,
            modifier = modifier,
        )
    } else {
        Box(
            modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Apps,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}
