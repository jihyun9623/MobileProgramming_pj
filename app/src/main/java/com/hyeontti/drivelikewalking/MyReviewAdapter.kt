package com.hyeontti.drivelikewalking

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions

class MyReviewAdapter(options: FirebaseRecyclerOptions<Reviews>) :
    FirebaseRecyclerAdapter<Reviews, MyReviewAdapter.ViewHolder>(options) {

        var itemClickListner:OnItemClickListner ?= null

        //viewholder에 관한 클래스
        inner class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
            var nowUser: TextView
            var star: TextView
            var content: TextView
            init {
                nowUser = itemView.findViewById(R.id.nowUser)
                star = itemView.findViewById(R.id.rstar)
                content = itemView.findViewById(R.id.rcontents)

                itemView.setOnClickListener {
                    itemClickListner?.OnItemClick(it,adapterPosition)
                }
            }
        }

        interface OnItemClickListner{
            fun OnItemClick(view: View, position: Int)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.row,parent,false)
            return ViewHolder(v)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int, model: Reviews) {
            holder.nowUser.text = model.uid
            holder.star.text = model.star.toString()
            holder.content.text = model.contents
        }
}