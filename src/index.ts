// Reexport the native module
import ExpoNativeVideo, { Props, VideoSource } from './ExpoNativeVideoView';

export { VideoSource };
export { Props as VideoPlayerProps };
export default ExpoNativeVideo;
export { default as VideoPlayer } from './ExpoNativeVideoView';
export { default as VideoSlider } from './ExpoNativeVideoView'; // Экспортируем как VideoSlider для удобства
