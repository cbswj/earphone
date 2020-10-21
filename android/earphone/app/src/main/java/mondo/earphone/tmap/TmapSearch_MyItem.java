package mondo.earphone.tmap;


public class TmapSearch_MyItem {


    private String name; // 장소 이름
    private String address; // 주소
    private String distance; // 거리
    private double Latitude; // 위도
    private double longitude; // 경도

    public TmapSearch_MyItem(String name, String address, String distance, double Latitude, double longitude ){
        this.name = name;
        this.address = address;
        this.distance = distance;
        this.Latitude = Latitude;
        this.longitude = longitude;
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getDistance() {
        return distance;
    }

    public void setDistance(String distance) {
        this.distance = distance;
    }

    public double getLatitude() {
        return Latitude;
    }

    public void setLatitude(double latitude) {
        Latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }
}