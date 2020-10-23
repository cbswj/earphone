from keras.models import Sequential, load_model
import librosa
import numpy as np
import tensorflow as tf
import keras
#print(keras.__version__+'a')
#print( tf.__version__)
class manufacturing():
    def __init__(self, filenames):
        self.output(filenames)

    def extract_feature(file_name):
        X, sample_rate = librosa.load(file_name)
        print("???????")

        stft = np.abs(
            librosa.stft(X))  # 단시간 푸리에 변환(Short-time Fourier Transform, STFT) https://darkpgmr.tistory.com/171
        mfccs = np.mean(librosa.feature.mfcc(y=X, sr=sample_rate, n_mfcc=40).T, axis=0)
        chroma = np.mean(librosa.feature.chroma_stft(S=stft, sr=sample_rate).T, axis=0)
        mel = np.mean(librosa.feature.melspectrogram(X, sr=sample_rate).T, axis=0)
        contrast = np.mean(librosa.feature.spectral_contrast(S=stft, sr=sample_rate).T, axis=0)
        tonnetz = np.mean(librosa.feature.tonnetz(y=librosa.effects.harmonic(X), sr=sample_rate).T, axis=0)
        return mfccs, chroma, mel, contrast, tonnetz

    def parse_audio_files(filenames):
        rows = len(filenames)
        print(rows,"row")
        features, labels, groups = np.zeros((rows, 193)), np.zeros((rows, 10)), np.zeros((rows, 1))
        i = 0
        for fn in filenames:
            try:
                mfccs, chroma, mel, contrast, tonnetz = manufacturing.extract_feature(fn)
                ext_features = np.hstack(
                    [mfccs, chroma, mel, contrast, tonnetz])  # hstack 행의 수가 같은 두 개 이상의 배열을 옆으로 연결하여 열의 수가 더 많은 배열을 만든다

            except Exception  as e:
                print(fn + "     ERROR", e)
            else:
                features[i] = ext_features

                i += 1
        return features


    def output(filenames):
        X=manufacturing.parse_audio_files(filenames)
        print('output')
        X = X.reshape(X.shape[0], 1, X.shape[1])

        model = Sequential()

        model = load_model('model_ver2.hdf5')  # 모델을 새로 불러옴

        return (model.predict(X))  # 불러온 모델로 테스트 실행
       # print(model.predict(X))


