package com.example.trashmap.Adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.trashmap.DBClasses.GarbageType;
import com.example.trashmap.Helpers.Constant;
import com.example.trashmap.R;
import com.squareup.picasso.Picasso;

import java.util.List;

public class EncyclopediaAdapter extends RecyclerView.Adapter<EncyclopediaAdapter.MyViewHolder>{
    private List<GarbageType> garbageList;

    // Интерфейс для получения нажатого элемента
    public interface OnGarbageTypeClickListener{
        void onGarbageClick(GarbageType garbageType, int position);
    }

    private final OnGarbageTypeClickListener onClickListener;


    class MyViewHolder extends RecyclerView.ViewHolder {
        TextView name;
        ImageView img;
        CardView cardView;
        MyViewHolder(View view) {
            super(view);

            name = view.findViewById(R.id.ei_name_list);
            img = view.findViewById(R.id.ei_img_list);
            cardView = view.findViewById(R.id.card_view_g_list);
        }
    }

    public EncyclopediaAdapter(List<GarbageType> garbageList, OnGarbageTypeClickListener onClickListener) {
        this.garbageList = garbageList;
        this.onClickListener = onClickListener;
    }

    @NonNull
    @Override
    public EncyclopediaAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_encyclopedia_item, parent, false);
        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull EncyclopediaAdapter.MyViewHolder holder, int position) {
        GarbageType garbage = garbageList.get(position);
            holder.name.setText(garbage.nameType);
            if (!garbage.imgUri.equals(Constant.GARBAGE_IMG_ALL)) {
                Picasso.get().load(garbage.imgUri).into(holder.img);
            } else {
                holder.img.setImageResource(R.drawable.types_icon_recycle);
            }
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    onClickListener.onGarbageClick(garbage, holder.getAdapterPosition());
                }
            });

        holder.name.setTextColor(Color.parseColor("#373636"));

    }

    @Override
    public int getItemCount() {
        return garbageList.size();
    }
}
