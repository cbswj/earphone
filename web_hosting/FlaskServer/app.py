from flask import Flask, render_template ,request

import test
from flask_jsglue import JSGlue
app = Flask(__name__)


jsglue = JSGlue()  ## Jquery 및 ajax 연동하기 위해 설정

app = Flask(__name__)
jsglue.init_app(app)

@app.route("/")
def index():
    print("시작 페이지 접속")
    return render_template('index.html')


#background process happening without any refreshing
@app.route('/background_process_test',  methods=['GET', 'POST'])  ## html에서 감지하고 감지한 데이터로 UID만 가져오기
def background_process_test():
    print ("러닝시작")
    if request.method == "POST":
        data={}
        data =request.json['uid']
        if data ==None:
            return render_template('index.html')

        print(data)
        uid =list(data.keys())[0]




    name = test.getdata(uid)  ## 파일 이름 가져오기
    print(uid,"uid")
    print(name,"파일명")
    if name==0:
        return render_template('index.html')

    test.downloadFromS3('wav-earphone', "wavfile/"+name, 'test.wav')

    pr =test.output(["test.wav"]) ##다운 받은 파일로 러닝 돌리기
    print(pr[0][0])
    test.setdata(str(pr[0][0]),uid) ## 러닝 결과 데베에 올리기

    return render_template('index.html')


if __name__ == "__main__":
    app.run(debug=True)

##  0 경적 1엔진 2사이렌