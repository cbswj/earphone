package mondo.earphone.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;


import mondo.earphone.R;
import mondo.earphone.tmap.TmapSearch_MyItem;


public class Home_MyAdapter extends RecyclerView.Adapter<Home_MyAdapter.TmapSearchViewHolder> {
    ArrayList<Home_MyItem> items = new ArrayList<Home_MyItem>();



    // 리스너 객체 참조를 저장하는 변수
    private OnItemClickListener mListener = null;

    public Home_MyAdapter(ArrayList<Home_MyItem> items) {
        addItems(items);
    }



    public class TmapSearchViewHolder extends RecyclerView.ViewHolder {  // 상대 메시지 뷰홀더 객체 재사용 하기 위해서 사용

        ImageView detect_img;
        TextView detect_time;
        TextView detect_content;



        public TmapSearchViewHolder(View itemView) {
            super(itemView);

            detect_img =  (ImageView) itemView.findViewById(R.id.detect_img);
            detect_time = (TextView) itemView.findViewById(R.id.detect_time);
            detect_content = (TextView) itemView.findViewById(R.id.detect_content);

            itemView.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    int pos = getAdapterPosition();
                    // 리스너 객체의 메서드 호출
                    if (pos != RecyclerView.NO_POSITION)
                    {
                        mListener.onItemClick(v, pos);
                    }
                }
            });

        }
    }



    @Override
    public TmapSearchViewHolder onCreateViewHolder(ViewGroup parent, int viewType) { // 뷰타입을 구분해서 xml 선언
        View view;
        view = LayoutInflater.from(parent.getContext()).inflate(R.layout.home_log_listview ,parent, false);
        return new TmapSearchViewHolder(view);
    }

    @Override
    public void onBindViewHolder(TmapSearchViewHolder holder, final int position) {  // 뷰타입 구분해서 데이터를 입력해주는곳!
        final Home_MyItem model = items.get(position);

        TmapSearchViewHolder holder1 = (TmapSearchViewHolder) holder;

        if(model.getContent().equals("경보기")){
            holder1.detect_img.setBackgroundResource(R.drawable.danger);
        }else if(model.getContent().equals("자동차")){
            holder1.detect_img.setBackgroundResource(R.drawable.car);
        }else if(model.getContent().equals("오토바이")){
            holder1.detect_img.setBackgroundResource(R.drawable.cycle);
        }

        holder1.detect_content.setText(model.getContent()+" 소리가 감지되었습니다.");

        holder1.detect_time.setText(model.getTime());

    }

    // 클릭 이벤트를 어댑터 말고 밖에서 하기 위해 인터페이스 구현
    public interface OnItemClickListener
    {
        void onItemClick(View v, int pos);
    }

    // OnItemClickListener 객체 참조를 어댑터에 전달하는 메서드
    public void setOnItemClickListener(OnItemClickListener listener)
    {
        this.mListener = listener;
    }

    //아이템을 추가해주고싶을때 이거쓰면됨
    public void addItem(Home_MyItem item) {
        items.add(item);
    }

    //한꺼번에 추가해주고싶을떄
    public void addItems(ArrayList<Home_MyItem> items) {
        this.items = items;
    }


    @Override
    public int getItemCount() {return items.size(); } // 현재 데이터 크기
}
