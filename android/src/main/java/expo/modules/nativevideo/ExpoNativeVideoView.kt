package expo.modules.nativevideo

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.FrameLayout.LayoutParams
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import expo.modules.kotlin.AppContext
import expo.modules.kotlin.views.ExpoView
import expo.modules.kotlin.records.Field
import expo.modules.kotlin.records.Record
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.DefaultLoadControl
import com.google.android.exoplayer2.DefaultRenderersFactory
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.ui.StyledPlayerView
import com.google.android.exoplayer2.video.VideoSize
import kotlin.math.abs
import kotlin.math.max

class VideoSource : Record {
  @Field var uri: String = ""
}

class VideoSourceArray : Record {
  @Field var videos: ArrayList<VideoSource> = ArrayList()
}

class VideoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
   val playerView: StyledPlayerView = view.findViewById(R.id.player_view)
  private val loadingIndicator: View = view.findViewById(R.id.loading_indicator)
  private var player: ExoPlayer? = null
  private var isPrepared = false

  fun preparePlayer(context: Context, uri: Uri, autoPlay: Boolean = false) {
    loadingIndicator.visibility = View.VISIBLE
    player?.release()
    isPrepared = false

    val loadControl = DefaultLoadControl.Builder()
      .setBufferDurationsMs(
        DefaultLoadControl.DEFAULT_MIN_BUFFER_MS,
        DefaultLoadControl.DEFAULT_MAX_BUFFER_MS,
        DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_MS / 2,
        DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS / 2
      )
      .setPrioritizeTimeOverSizeThresholds(true)
      .build()

    player = ExoPlayer.Builder(context)
      .setLoadControl(loadControl)
      .setRenderersFactory(
        DefaultRenderersFactory(context)
          .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
      )
      .build().apply {
        repeatMode = Player.REPEAT_MODE_ONE
        playWhenReady = autoPlay
        volume = 0f
        setMediaItem(MediaItem.fromUri(uri))
        prepare()

        addListener(object : Player.Listener {
          override fun onPlaybackStateChanged(state: Int) {
            when (state) {
              Player.STATE_READY -> {
                loadingIndicator.visibility = View.GONE
                isPrepared = true
                if (playWhenReady) play()
              }
              Player.STATE_BUFFERING -> loadingIndicator.visibility = View.VISIBLE
              Player.STATE_ENDED -> {
                seekTo(0)
                play()
              }
            }
          }

          override fun onVideoSizeChanged(videoSize: VideoSize) {
            playerView.post {
              val vw = playerView.width
              if (vw > 0 && videoSize.height > 0) {
                val aspect = videoSize.width.toFloat() / videoSize.height
                val newH = (vw / aspect).toInt()
                playerView.layoutParams = playerView.layoutParams.apply {
                  height = newH
                }
                playerView.requestLayout()
              }
            }
          }
        })
      }

    playerView.player = player
    playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
  }

  fun playVideo() {
    if (isPrepared) player?.play() else player?.playWhenReady = true
  }

  fun pauseVideo() {
    player?.pause()
  }

  fun releasePlayer() {
    loadingIndicator.visibility = View.GONE
    player?.release()
    player = null
    isPrepared = false
  }
}

class VideoAdapter(
  private val context: Context,
  private val videos: List<VideoSource>
) : RecyclerView.Adapter<VideoViewHolder>() {

  private val preloadedUris = mutableSetOf<String>()
  private var currentActivePosition = -1

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
    val view = LayoutInflater.from(context)
      .inflate(R.layout.video_item, parent, false)
    view.layoutParams = ViewGroup.LayoutParams(
      ViewGroup.LayoutParams.MATCH_PARENT,
      ViewGroup.LayoutParams.MATCH_PARENT
    )
    return VideoViewHolder(view)
  }

  override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
    val source = videos[position % videos.size]
    
    // Устанавливаем начальную высоту для playerView, чтобы избежать растягивания на весь экран
    val screenWidth = context.resources.displayMetrics.widthPixels
    val initialHeight = (screenWidth * 9 / 16) // Соотношение сторон 16:9 как начальное
    holder.playerView.layoutParams = holder.playerView.layoutParams.apply {
      height = initialHeight
    }
    
    holder.preparePlayer(context, Uri.parse(source.uri), position == currentActivePosition)
    preloadedUris += source.uri
  }

  override fun getItemCount(): Int =
    if (videos.isEmpty()) 0 else Int.MAX_VALUE / 2

  fun getRealPosition(position: Int): Int =
    if (videos.isEmpty()) 0 else position % videos.size

  fun activatePosition(position: Int) {
    currentActivePosition = position
    notifyItemChanged(position)
  }

  fun releaseAllPlayers(recycler: RecyclerView) {
    for (i in 0 until recycler.childCount) {
      (recycler.getChildViewHolder(recycler.getChildAt(i)) as? VideoViewHolder)
        ?.releasePlayer()
    }
    preloadedUris.clear()
    currentActivePosition = -1
  }

  fun preloadAdjacentVideos(currentPosition: Int, recycler: RecyclerView) {
    if (videos.isEmpty()) return
    val next = (currentPosition + 1) % videos.size
    val prev = (currentPosition - 1 + videos.size) % videos.size
    preloadedUris += videos[next].uri
    preloadedUris += videos[prev].uri
  }
}

class ExpoNativeVideoView(context: Context, appContext: AppContext) :
  ExpoView(context, appContext) {

  companion object {
    private const val PRELOAD_PAGES = 2
    private const val INITIAL_POSITION = Int.MAX_VALUE / 4
  }

  private val viewPager = ViewPager2(context).apply {
    layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
    orientation = ViewPager2.ORIENTATION_HORIZONTAL
    offscreenPageLimit = PRELOAD_PAGES

    (getChildAt(0) as RecyclerView).apply {
      overScrollMode = RecyclerView.OVER_SCROLL_NEVER
      setOnTouchListener { _, event: MotionEvent ->
        when (event.action) {
          MotionEvent.ACTION_DOWN ->
            parent.requestDisallowInterceptTouchEvent(true)
          MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
            parent.requestDisallowInterceptTouchEvent(false)
        }
        false
      }
    }

    setPageTransformer(ZoomOutPageTransformer())
    this@ExpoNativeVideoView.addView(this)
  }

  private var videoAdapter: VideoAdapter? = null
  private var currentPosition = INITIAL_POSITION

  private val pageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
    private var previousPage = INITIAL_POSITION
    private var isFirst = true
    private var settling = false

    override fun onPageScrollStateChanged(state: Int) {
      settling = state == ViewPager2.SCROLL_STATE_SETTLING
      if (state == ViewPager2.SCROLL_STATE_IDLE) activatePage()
    }

    override fun onPageSelected(pos: Int) {
      currentPosition = pos
      if (!settling) activatePage()
    }

    private fun activatePage() {
      val recycler = viewPager.getChildAt(0) as? RecyclerView ?: return
      val real = videoAdapter?.getRealPosition(currentPosition) ?: 0
      videoAdapter?.activatePosition(currentPosition)

      (recycler.findViewHolderForAdapterPosition(currentPosition) as? VideoViewHolder)
        ?.let { holder ->
          if (isFirst) {
            holder.playerView.postDelayed({
              holder.playVideo()
              isFirst = false
            }, 100)
          } else holder.playVideo()
        }

      if (currentPosition != previousPage) {
        (recycler.findViewHolderForAdapterPosition(previousPage) as? VideoViewHolder)
          ?.pauseVideo()
        videoAdapter?.preloadAdjacentVideos(real, recycler)
      }

      previousPage = currentPosition
    }
  }

  private inner class ZoomOutPageTransformer : ViewPager2.PageTransformer {
    private val MIN_SCALE = 0.85f
    private val MIN_ALPHA = 0.5f

    override fun transformPage(view: View, position: Float) {
      view.apply {
        when {
          position < -1 -> alpha = 0f
          position <= 1 -> {
            val scale = max(MIN_SCALE, 1f - abs(position))
            val vertMargin = height * (1 - scale) / 2
            val horzMargin = width * (1 - scale) / 2
            translationX = if (position < 0) horzMargin - vertMargin / 2 else horzMargin + vertMargin / 2
            scaleX = scale; scaleY = scale
            alpha = MIN_ALPHA + (scale - MIN_SCALE) / (1 - MIN_SCALE) * (1 - MIN_ALPHA)
          }
          else -> alpha = 0f
        }
      }
    }
  }

  init {
    viewPager.registerOnPageChangeCallback(pageChangeCallback)
  }

  fun setVideos(arr: VideoSourceArray?) {
    if (arr?.videos.isNullOrEmpty()) return
    (viewPager.getChildAt(0) as? RecyclerView)?.let {
      videoAdapter?.releaseAllPlayers(it)
    }
    videoAdapter = VideoAdapter(context, arr!!.videos)
    viewPager.adapter = videoAdapter
    viewPager.setCurrentItem(INITIAL_POSITION, false)
    currentPosition = INITIAL_POSITION
    viewPager.post { pageChangeCallback.onPageSelected(currentPosition) }
  }

  fun setSource(src: VideoSource?) {
    src?.takeIf { it.uri.isNotEmpty() }?.also {
      val arr = VideoSourceArray().apply { videos.add(it) }
      setVideos(arr)
    }
  }

  override fun onDetachedFromWindow() {
    super.onDetachedFromWindow()
    viewPager.unregisterOnPageChangeCallback(pageChangeCallback)
    (viewPager.getChildAt(0) as? RecyclerView)?.let {
      videoAdapter?.releaseAllPlayers(it)
    }
    videoAdapter = null
  }

  override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
    super.onSizeChanged(w, h, oldw, oldh)
    viewPager.layoutParams = LayoutParams(w, h)
  }
}
