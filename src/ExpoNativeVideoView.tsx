import { ViewProps } from 'react-native';
import { requireNativeViewManager } from 'expo-modules-core';
import * as React from 'react';

export type VideoSource = {
  uri: string;
};

export type Props = ViewProps & {
  source?: VideoSource;
  videos?: VideoSource[];
};

const NativeView = requireNativeViewManager('ExpoNativeVideo');

export default function ExpoNativeVideo(props: Props) {
  // Создаем новый объект для нативных пропсов
  const nativeProps: Record<string, any> = {};
  
  // Копируем все стилевые пропсы
  if (props.style) {
    nativeProps.style = props.style;
  }
  
  // Если передан один источник видео
  if (props.source) {
    nativeProps.source = props.source;
  }
  
  // Если передан массив видео
  if (props.videos && Array.isArray(props.videos)) {
    nativeProps.videos = {
      videos: props.videos
    };
  }
  
  return <NativeView {...nativeProps} />;
}
