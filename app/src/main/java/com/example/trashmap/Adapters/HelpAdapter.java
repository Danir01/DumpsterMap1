package com.example.trashmap.Adapters;

import android.media.Image;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.trashmap.DBClasses.HelpItem;
import com.example.trashmap.DBClasses.Messages;
import com.example.trashmap.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;

public class HelpAdapter extends RecyclerView.Adapter<HelpAdapter.MyViewHolder>{
    private ArrayList<HelpItem> helpItems;

    // Интерфейс для получения нажатого элемента
    public interface OnHelpersClickListener{
        void onHelperClick(HelpItem helpItem, int position);
    }

    private final OnHelpersClickListener onClickListener;


    class MyViewHolder extends RecyclerView.ViewHolder {
        TextView name;
        ImageView img;

        MyViewHolder(View view) {
            super(view);

            name = view.findViewById(R.id.lhi_name);
            img = view.findViewById(R.id.lhi_img);
        }
    }

    public HelpAdapter(ArrayList<HelpItem> helpItems, OnHelpersClickListener onClickListener) {
        this.helpItems = helpItems;
        this.onClickListener = onClickListener;
    }

    @NonNull
    @Override
    public HelpAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_help_items, parent, false);
        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        HelpItem helpItem = helpItems.get(position);

        holder.name.setText(helpItem.getName());

        if(helpItem.getName().equals("Работа с картой")){
            holder.img.setImageResource(R.drawable.types_icon_help_map);
        }
        else if (helpItem.getName().equals("Добавление новой точки")){
            holder.img.setImageResource(R.drawable.types_icon_help_add);
        }
        else if (helpItem.getName().equals("Энциклопедия")){
            holder.img.setImageResource(R.drawable.types_icon_help_encyclopedia);
        }
        else if (helpItem.getName().equals("Подробная информация о точке")){
            holder.img.setImageResource(R.drawable.types_icon_help_info);
        }
        else if (helpItem.getName().equals("Меню")){
            holder.img.setImageResource(R.drawable.types_icon_help_menu);

        }
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onClickListener.onHelperClick(helpItem, holder.getAdapterPosition());
            }
        });
               }

    @Override
    public int getItemCount() {
        return helpItems.size();
    }
}
