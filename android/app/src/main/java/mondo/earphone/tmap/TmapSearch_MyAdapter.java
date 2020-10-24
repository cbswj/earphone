package mondo.earphone.tmap;

import android.content.Intent;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;

import mondo.earphone.R;


public class TmapSearch_MyAdapter extends RecyclerView.Adapter<TmapSearch_MyAdapter.TmapSearchViewHolder> {
    ArrayList<TmapSearch_MyItem> items = new ArrayList<TmapSearch_MyItem>();



    // 리스너 객체 참조를 저장하는 변수
    private OnItemClickListener mListener = null;

    public TmapSearch_MyAdapter(ArrayList<TmapSearch_MyItem> items) {
        addItems(items);
    }



    public class TmapSearchViewHolder extends RecyclerView.ViewHolder {  // 상대 메시지 뷰홀더 객체 재사용 하기 위해서 사용

        TextView search_title;
        TextView search_address;
        TextView search_distance;



        public TmapSearchViewHolder(View itemView) {
            super(itemView);

            search_title =  (TextView) itemView.findViewById(R.id.search_title);
            search_address = (TextView) itemView.findViewById(R.id.search_address);
            search_distance = (TextView) itemView.findViewById(R.id.search_distance);

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
        view = LayoutInflater.from(parent.getContext()).inflate(R.layout.search_listview ,parent, false);
        return new TmapSearchViewHolder(view);
    }

    @Override
    public void onBindViewHolder(TmapSearchViewHolder holder, final int position) {  // 뷰타입 구분해서 데이터를 입력해주는곳!
        final TmapSearch_MyItem model = items.get(position);

        TmapSearchViewHolder holder1 = (TmapSearchViewHolder) holder;

        holder1.search_title.setText(model.getName());
        holder1.search_address.setText(model.getAddress());
        holder1.search_distance.setText(model.getDistance());


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
    public void addItem(TmapSearch_MyItem item) {
        items.add(item);
    }

    //한꺼번에 추가해주고싶을떄
    public void addItems(ArrayList<TmapSearch_MyItem> items) {
        this.items = items;
    }


    @Override
    public int getItemCount() {return items.size(); } // 현재 데이터 크기
}
