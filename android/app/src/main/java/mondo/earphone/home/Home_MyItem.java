package mondo.earphone.home;


public class Home_MyItem {

    private String time; // 시간
    private String content; // 내용



    public Home_MyItem(String time,String content){
        this.content = content;
        this.time = time;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }
}