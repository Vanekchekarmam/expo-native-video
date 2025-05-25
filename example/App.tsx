import { VideoSlider } from 'expo-native-video';
import { StyleSheet, View, StatusBar, SafeAreaView, Text } from 'react-native';
import { useEffect } from 'react';
import { Platform } from 'react-native';

// Примеры видео для слайдера
const videoSources = [
  { uri: 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4' },
  { uri: 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4' },
  { uri: 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4' },
  { uri: 'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4' },
];

export default function App() {
  // Скрываем статус-бар для полноэкранного режима
  useEffect(() => {
    StatusBar.setHidden(true);
    return () => {
      StatusBar.setHidden(false);
    };
  }, []);

  // На Android VideoSlider работает только в полноэкранном режиме
  if (Platform.OS === 'android') {
    return (
      <View style={styles.fullScreenContainer}>
        <VideoSlider
          style={styles.videoSlider}
          videos={videoSources}
        />
      </View>
    );
  }
  
  // На других платформах показываем заглушку
  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.notSupportedContainer}>
        <Text style={styles.notSupportedText}>
          Видео слайдер доступен только для Android
        </Text>
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  fullScreenContainer: {
    flex: 1,
    backgroundColor: '#000',
  },
  container: {
    flex: 1,
    backgroundColor: '#fff',
  },
  videoSlider: {
    flex: 1,
  },
  notSupportedContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    padding: 20,
  },
  notSupportedText: {
    fontSize: 18,
    textAlign: 'center',
  },
});
