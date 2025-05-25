package expo.modules.nativevideo
import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition
import expo.modules.nativevideo.ExpoNativeVideoView
import expo.modules.nativevideo.VideoSource
import expo.modules.nativevideo.VideoSourceArray

class ExpoNativeVideoModule : Module() {
  override fun definition() = ModuleDefinition {
    Name("ExpoNativeVideo")

    View(ExpoNativeVideoView::class) {
      // Определяем проп source для передачи одного URL видео (для обратной совместимости)
      Prop("source") { view: ExpoNativeVideoView, source: VideoSource? ->
        view.setSource(source)
      }

      // Определяем проп videos для передачи массива видео
      Prop("videos") { view: ExpoNativeVideoView, videos: VideoSourceArray? ->
        view.setVideos(videos)
      }
    }
  }
}
