package com.example.trashmap.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.trashmap.DBClasses.GarbageType;
import com.example.trashmap.DBClasses.Messages;
import com.example.trashmap.Helpers.Constant;
import com.example.trashmap.R;
import com.squareup.picasso.Picasso;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MessagesAdapter extends RecyclerView.Adapter<MessagesAdapter.MyViewHolder>{
    private ArrayList<Messages> messageList;

    // Интерфейс для получения нажатого элемента
    public interface OnMessagesClickListener{
        void onGarbageClick(Messages message, int position);
    }

    private final OnMessagesClickListener onClickListener;


    class MyViewHolder extends RecyclerView.ViewHolder {
        TextView subject;
        TextView text;
        TextView date;

        MyViewHolder(View view) {
            super(view);

            subject = view.findViewById(R.id.lm_subject);
            text = view.findViewById(R.id.lm_text);
            date = view.findViewById(R.id.lm_date);
        }
    }

    public MessagesAdapter(ArrayList<Messages> MessageList, OnMessagesClickListener onClickListener) {
        this.messageList = MessageList;
        this.onClickListener = onClickListener;
    }

    @NonNull
    @Override
    public MessagesAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_messages, parent, false);
        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        Messages message = messageList.get(position);
        holder.subject.setText(message.getSubject());
        holder.text.setText((String)message.getContent());
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("E, dd LLL yyyy HH:mm");
        holder.date.setText(simpleDateFormat.format(message.getDate()).toString());
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onClickListener.onGarbageClick(message, holder.getAdapterPosition());
            }
        });
            /*holder.name.setText(garbage.nameType);
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
            });*/

    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }
}
