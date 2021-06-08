package io.agora.flat.ui.activity.play

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import io.agora.flat.R
import io.agora.flat.ui.viewmodel.ChatMessage

class MessageAdapter : RecyclerView.Adapter<MessageAdapter.ViewHolder>() {
    private val dataSet: MutableList<ChatMessage> = mutableListOf()

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(viewGroup.context)
        val view = inflater.inflate(R.layout.item_room_message, viewGroup, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val item = dataSet[position]
        if (item.isOwner) {
            viewHolder.rightMessageLayout.isVisible = true
            viewHolder.leftMessageLayout.isVisible = false
            viewHolder.noticeMessageLayout.isVisible = false

            viewHolder.rightMessage.text = item.message
        } else {
            viewHolder.rightMessageLayout.isVisible = false
            viewHolder.leftMessageLayout.isVisible = true
            viewHolder.noticeMessageLayout.isVisible = false

            viewHolder.leftMessage.text = item.message
            viewHolder.leftName.text = item.name
        }
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getItemCount() = dataSet.size

    fun setDataList(it: List<ChatMessage>) {
        dataSet.clear()
        dataSet.addAll(it)
        notifyDataSetChanged()
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val leftMessageLayout: View = view.findViewById(R.id.leftMessageLayout)
        val leftName: TextView = view.findViewById(R.id.name)
        val leftMessage: TextView = view.findViewById(R.id.leftMessage)
        val rightMessageLayout: View = view.findViewById(R.id.rightMessageLayout)
        val rightMessage: TextView = view.findViewById(R.id.rightMessage)
        val noticeMessageLayout: View = view.findViewById(R.id.noticeMessageLayout)
    }
}
