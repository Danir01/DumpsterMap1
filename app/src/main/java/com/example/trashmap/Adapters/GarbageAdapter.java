package com.example.trashmap.Adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.trashmap.DBClasses.GarbageType;
import com.example.trashmap.Helpers.Constant;
import com.example.trashmap.R;
import com.google.firebase.database.collection.LLRBNode;
import com.squareup.picasso.Picasso;

import java.util.List;

public class GarbageAdapter extends RecyclerView.Adapter<GarbageAdapter.MyViewHolder>{

    private List<GarbageType> garbageList;
    int row_index = -1;

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

            name = view.findViewById(R.id.text_g_list);
            img = view.findViewById(R.id.img_g_list);
            cardView = view.findViewById(R.id.card_view_g_list);
        }
    }

    public GarbageAdapter(List<GarbageType> garbageList, OnGarbageTypeClickListener onClickListener) {
        this.garbageList = garbageList;
        this.onClickListener = onClickListener;
    }

    @NonNull
    @Override
    public GarbageAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_garbage, parent, false);
        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull GarbageAdapter.MyViewHolder holder, int position) {
        GarbageType garbage = garbageList.get(position);
        holder.name.setText(garbage.nameType);
        if(!garbage.imgUri.equals(Constant.GARBAGE_IMG_ALL)){
            Picasso.get().load(garbage.imgUri).into(holder.img);
        }
        else{
            holder.img.setImageResource(R.drawable.types_icon_recycle);
        }
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onClickListener.onGarbageClick(garbage, holder.getAdapterPosition());
                row_index = holder.getAdapterPosition();
                notifyDataSetChanged();
            }
        });

        if (row_index == position){
            holder.cardView.setCardBackgroundColor(Color.parseColor("#007560"));
            holder.cardView.setRadius(10);
            holder.name.setTextColor(Color.parseColor("#FFFDF2"));
        }
        else {
            holder.cardView.setCardBackgroundColor(Color.parseColor("#FFFDF2"));
            holder.cardView.setRadius(10);
            holder.name.setTextColor(Color.parseColor("#373636"));
        }
    }

    @Override
    public int getItemCount() {
        return garbageList.size();
    }
}
