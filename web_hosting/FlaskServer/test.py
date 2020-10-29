import os

import firebase_admin

from firebase_admin import db
from firebase_admin import credentials
from keras.models import Sequential, load_model
import librosa
import numpy as np
from keras import backend as K
import boto3


BUCKET_NAME = 'wav-earphone'  # 저장소 이름
ACCESS_KEY = 'AKIA5PJP5IRPYXVGWWWS'  # 저장소 사용시 필요 키
SECRET_KEY = 'Cxwdq2eOyxGaNOtKIgAAtB5oZ2x8VHCkhOGBlaMM'  # 저장소 사용시 필요 키

config = {   ## 파이어베이스 키 값 설정
    "apiKey": "AIzaSyAWJdB9cR9Ao1h-yDEj7kKFp-2dQtgqB8o",
    "authDomain": "earphone-5d996.firebaseapp.com",
    "databaseURL": "https://earphone-5d996.firebaseio.com",
    "projectId": "earphone-5d996",
    "storageBucket": "earphone-5d996.appspot.com",
    "messagingSenderId": "703763634836",
    "appId": "1:703763634836:web:f6dc8c8c9356687b9cf281",
    "measurementId": "G-S31F3RRC7C"
}

cred = credentials.Certificate('EarPhone-15d7ff043f27.json')  ## 파베 연동
default_app = firebase_admin.initialize_app(cred, {
    'databaseURL': 'https://earphone-5d996.firebaseio.com/'
})
def getdata(uid):   ## 사용자가 올린 파일 이름 가져오기



    ref3 = db.reference('UID')
    snapshot = ref3.child(uid).get()
    if snapshot==None:
        print("데이터 없음")
        return 0
    else:

        print(snapshot)
        file = list(snapshot.values())[0]['FileName']
        print(file)
        return file  ## 파일이름만 가져오기

def setdata(name,uid):  ## 사용자한테 결과값 돌려주기
    ref2 = db.reference('Result')
    ref2.child(uid).push({'Result':
        name
    })


def extract_feature(file_name):  ######### wav 파일 전처리 함수
    X, sample_rate = librosa.load(file_name)
    stft = np.abs(
        librosa.stft(X))  # 단시간 푸리에 변환(Short-time Fourier Transform, STFT) https://darkpgmr.tistory.com/171
    mfccs = np.mean(librosa.feature.mfcc(y=X, sr=sample_rate, n_mfcc=40).T, axis=0)
    chroma = np.mean(librosa.feature.chroma_stft(S=stft, sr=sample_rate).T, axis=0)
    mel = np.mean(librosa.feature.melspectrogram(X, sr=sample_rate).T, axis=0)
    contrast = np.mean(librosa.feature.spectral_contrast(S=stft, sr=sample_rate).T, axis=0)
    tonnetz = np.mean(librosa.feature.tonnetz(y=librosa.effects.harmonic(X), sr=sample_rate).T, axis=0)
    return mfccs, chroma, mel, contrast, tonnetz
def parse_audio_files(filenames): ######### wav 파일 전처리 함수
    rows = len(filenames)
    features, labels, groups = np.zeros((rows, 193)), np.zeros((rows, 4)), np.zeros((rows, 1))
    i = 0
    for fn in filenames:
        try:
            mfccs, chroma, mel, contrast, tonnetz = extract_feature(fn)
            ext_features = np.hstack(
                [mfccs, chroma, mel, contrast, tonnetz])  # hstack 행의 수가 같은 두 개 이상의 배열을 옆으로 연결하여 열의 수가 더 많은 배열을 만든다
        except Exception  as e:
            print(fn + "     ERROR", e)
        else:
            features[i] = ext_features
            i += 1
    return features
def output(filenames):
    #os.remove('test.wav')

    K.clear_session()
    X = parse_audio_files(filenames)
    X = X.reshape(X.shape[0], 1, X.shape[1])
    model = Sequential()
    model = load_model('model_keras_v2.hdf5')  # 모델을 새로 불러옴

    train = np.array(model.predict(X), dtype=np.float32)
    train = train.reshape(-1)

    np.set_printoptions(formatter={'float': lambda x: "{0:0.3f}".format(x)})
    cls = ['car_horn', 'engine_idling', 'siren','mute']
    result=[]
    for i in range(0, 4):
        print(cls[i] + ' :' + str(round(train[i] * 100, 4)))
        result.append(round(train[i] * 100, 4))
    if(max(result) <90):
        return 3


    return (model.predict_classes(X))  # 불러온 모델로 테스트 실행



def downloadFromS3(strBucket, s3_path, local_path):  ## s3파일 다운로드
    s3_client = boto3.client(
        's3',
        aws_access_key_id=ACCESS_KEY,
        aws_secret_access_key = SECRET_KEY
    )
    s3_client.download_file(strBucket, s3_path, local_path)