package com.d4viddf.hyperbridge.service.render

import android.app.KeyguardManager
import android.app.PendingIntent
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.service.notification.StatusBarNotification
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.d4viddf.hyperbridge.models.IslandConfig
import com.d4viddf.hyperbridge.models.NotificationType
import com.d4viddf.hyperbridge.models.RenderBackend
import java.util.concurrent.ConcurrentHashMap

class UniversalOverlayRenderer(
    private val context: Context,
    private val anchorManager: OverlayAnchorManager,
    private val showOnLockscreenProvider: () -> Boolean
) : IslandRenderer {

    override val backend: RenderBackend = RenderBackend.UNIVERSAL_OVERLAY

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val mainHandler = Handler(Looper.getMainLooper())
    private val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager

    private var container: LinearLayout? = null
    private val holders = ConcurrentHashMap<String, OverlayItemHolder>()

    override fun post(
        sourceKey: String,
        sbn: StatusBarNotification,
        type: NotificationType,
        config: IslandConfig,
        payload: Any?
    ): Int? {
        val content = payload as? OverlayIslandContent ?: return null
        if (isLocked() && !showOnLockscreenProvider()) {
            dismiss(sourceKey)
            return null
        }
        val renderId = sourceKey.hashCode()
        mainHandler.post {
            ensureContainer()
            val existing = holders[sourceKey]
            if (existing == null) {
                val holder = createItemHolder(sourceKey, sbn, content)
                holders[sourceKey] = holder
                container?.addView(holder.root)
                holder.root.alpha = 0f
                holder.root.translationY = -dp(10).toFloat()
                holder.root.animate().alpha(1f).translationY(0f).setDuration(180L).start()
            } else {
                bindHolder(existing, sbn, content)
                existing.root.animate().cancel()
                existing.root.alpha = 0.85f
                existing.root.animate().alpha(1f).setDuration(140L).start()
            }
            refreshWindowLayout()
        }
        return renderId
    }

    override fun dismiss(sourceKey: String) {
        mainHandler.post {
            val holder = holders.remove(sourceKey) ?: return@post
            holder.root.animate().cancel()
            holder.root.animate()
                .alpha(0f)
                .translationY(-dp(8).toFloat())
                .setDuration(150L)
                .withEndAction {
                    container?.removeView(holder.root)
                    if (holders.isEmpty()) removeContainer()
                }
                .start()
        }
    }

    override fun dismissByRenderedId(renderedId: Int) {
        val key = holders.keys.firstOrNull { it.hashCode() == renderedId } ?: return
        dismiss(key)
    }

    override fun clearAll() {
        mainHandler.post {
            holders.clear()
            removeContainer()
        }
    }

    private fun ensureContainer() {
        if (container != null) return
        val parent = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
        }
        container = parent
        runCatching { windowManager.addView(parent, anchorManager.buildLayoutParams()) }
    }

    private fun removeContainer() {
        val target = container ?: return
        runCatching { windowManager.removeView(target) }
        container = null
    }

    private fun refreshWindowLayout() {
        val target = container ?: return
        runCatching {
            windowManager.updateViewLayout(target, anchorManager.buildLayoutParams())
        }
    }

    private fun createItemHolder(
        sourceKey: String,
        sbn: StatusBarNotification,
        content: OverlayIslandContent
    ): OverlayItemHolder {
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(10), dp(8), dp(10), dp(8))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(26).toFloat()
                setColor(Color.parseColor("#E6000000"))
            }
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            lp.topMargin = dp(6)
            layoutParams = lp
        }

        val collapsedRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val iconView = ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(dp(24), dp(24))
        }

        val textContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = dp(10)
            }
        }

        val titleView = TextView(context).apply {
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setTypeface(typeface, Typeface.BOLD)
            maxLines = 1
        }
        val textView = TextView(context).apply {
            setTextColor(Color.parseColor("#D9FFFFFF"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            maxLines = 1
        }

        val expandView = TextView(context).apply {
            text = "v"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setTypeface(typeface, Typeface.BOLD)
            setPadding(dp(8), dp(4), dp(8), dp(4))
        }

        val expandedContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            setPadding(0, dp(8), 0, 0)
        }

        val progressView = TextView(context).apply {
            setTextColor(Color.parseColor("#B3FFFFFF"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            visibility = View.GONE
        }

        val actionsContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
            setPadding(0, dp(6), 0, 0)
        }

        textContainer.addView(titleView)
        textContainer.addView(textView)
        collapsedRow.addView(iconView)
        collapsedRow.addView(textContainer)
        collapsedRow.addView(expandView)

        expandedContainer.addView(progressView)
        expandedContainer.addView(actionsContainer)

        root.addView(collapsedRow)
        root.addView(expandedContainer)

        val holder = OverlayItemHolder(
            root = root,
            title = titleView,
            text = textView,
            icon = iconView,
            expand = expandView,
            expanded = expandedContainer,
            progress = progressView,
            actions = actionsContainer,
            sourceKey = sourceKey
        )

        bindHolder(holder, sbn, content)

        expandView.setOnClickListener {
            holder.isExpanded = !holder.isExpanded
            holder.expanded.visibility = if (holder.isExpanded) View.VISIBLE else View.GONE
            holder.expand.text = if (holder.isExpanded) "^" else "v"
        }

        root.setOnClickListener {
            sendPendingIntent(sbn.notification.contentIntent)
        }

        return holder
    }

    private fun bindHolder(
        holder: OverlayItemHolder,
        sbn: StatusBarNotification,
        content: OverlayIslandContent
    ) {
        holder.title.text = content.title
        holder.text.text = if (content.text.isBlank()) content.appLabel else content.text
        holder.icon.setImageBitmap(content.icon)

        if (content.progressPercent != null) {
            holder.progress.visibility = View.VISIBLE
            holder.progress.text = "${content.progressPercent}%"
        } else {
            holder.progress.visibility = View.GONE
        }

        holder.actions.removeAllViews()
        content.actions.take(3).forEach { action ->
            val actionView = TextView(context).apply {
                text = action.title
                setTextColor(Color.WHITE)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                setPadding(dp(10), dp(5), dp(10), dp(5))
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = dp(14).toFloat()
                    setColor(Color.parseColor("#26FFFFFF"))
                }
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginStart = dp(6)
                }
                setOnClickListener { sendPendingIntent(action.intent) }
            }
            holder.actions.addView(actionView)
        }

        if (content.actions.isEmpty() && content.progressPercent == null) {
            holder.expanded.visibility = View.GONE
            holder.isExpanded = false
            holder.expand.text = "v"
        }
    }

    private fun sendPendingIntent(intent: PendingIntent?) {
        runCatching { intent?.send() }
    }

    private fun isLocked(): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            keyguardManager.isDeviceLocked
        } else {
            @Suppress("DEPRECATION")
            keyguardManager.inKeyguardRestrictedInputMode()
        }
    }

    private fun dp(value: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            context.resources.displayMetrics
        ).toInt()
    }
}

private data class OverlayItemHolder(
    val root: LinearLayout,
    val title: TextView,
    val text: TextView,
    val icon: ImageView,
    val expand: TextView,
    val expanded: LinearLayout,
    val progress: TextView,
    val actions: LinearLayout,
    val sourceKey: String,
    var isExpanded: Boolean = false
)
