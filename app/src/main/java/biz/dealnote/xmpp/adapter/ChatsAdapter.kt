package biz.dealnote.xmpp.adapter

import android.content.Context
import android.graphics.Typeface
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView

import com.squareup.picasso.Transformation

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import biz.dealnote.xmpp.R
import biz.dealnote.xmpp.model.Chat
import biz.dealnote.xmpp.model.AppMessage
import biz.dealnote.xmpp.util.Avatars
import biz.dealnote.xmpp.util.RoundTransformation
import biz.dealnote.xmpp.util.Utils

private const val DIV_DISABLE = 0
private const val DIV_TODAY = 1
private const val DIV_YESTERDAY = 2
private const val DIV_THIS_WEEK = 3
private const val DIV_OLD = 4

class ChatsAdapter(private var data: List<Chat>, private val context: Context) : RecyclerView.Adapter<ChatsAdapter.Holder>() {

    private val transformation: Transformation
    private var clickListener: ClickListener? = null
    private var mStartOfToday: Long = 0

    init {
        this.transformation = RoundTransformation()
        initStartOfTodayDate()
    }

    private fun initStartOfTodayDate() {
        this.mStartOfToday = Utils.startOfTodayMillis()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        return Holder(LayoutInflater.from(context).inflate(R.layout.item_chat, parent, false))
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val chat = data[position]

        val interlocutor = chat.interlocutor

        holder.unreadCount.visibility = if (chat.unreadCount > 0) View.VISIBLE else View.GONE
        holder.unreadCount.text = chat.unreadCount.toString()

        holder.interlocutorName.text = interlocutor.dispayName
        holder.interlocutorName.setTypeface(null, if (chat.unreadCount > 0) Typeface.BOLD else Typeface.NORMAL)

        val body = AppMessage.getMessageBody(context, chat.lastMessageType,
                chat.isLastMessageOut, chat.destination, chat.lastMessageText)

        holder.body.text = body

        Avatars.displayAvatar(context, holder, interlocutor, transformation)

        holder.itemView.setOnClickListener {
            clickListener?.onClick(chat)
        }

        holder.itemView.setOnLongClickListener {clickListener?.onLongClick(chat) ?: false }

        // configure item header
        val previous = if (position == 0) null else data[position - 1]
        val lastMessageJavaTime = chat.lastMessageTime * 1000
        val headerStatus = getDivided(lastMessageJavaTime, if (previous == null) null else previous.lastMessageTime * 1000)

        when (headerStatus) {
            DIV_DISABLE -> holder.header.visibility = View.GONE
            DIV_OLD -> {
                holder.header.visibility = View.VISIBLE
                holder.headerText.setText(R.string.dialog_day_older)
            }
            DIV_TODAY -> {
                holder.header.visibility = View.VISIBLE
                holder.headerText.setText(R.string.dialog_day_today)
            }
            DIV_YESTERDAY -> {
                holder.header.visibility = View.VISIBLE
                holder.headerText.setText(R.string.dialog_day_yesterday)
            }
            DIV_THIS_WEEK -> {
                holder.header.visibility = View.VISIBLE
                holder.headerText.setText(R.string.dialog_day_ten_days)
            }
        }

        DATE.time = lastMessageJavaTime
        holder.time.text = if (lastMessageJavaTime > Utils.startOfTodayMillis()) DF_TODAY.format(DATE) else DF_OLD.format(DATE)
    }

    private fun getDivided(messageDateJavaTime: Long, previousMessageDateJavaTime: Long?): Int {
        val stCurrent = getStatus(messageDateJavaTime)
        return if (previousMessageDateJavaTime == null) {
            stCurrent
        } else {
            val stPrevious = getStatus(previousMessageDateJavaTime)
            if (stCurrent == stPrevious) {
                DIV_DISABLE
            } else {
                stCurrent
            }
        }
    }

    private fun getStatus(time: Long): Int {
        if (time >= mStartOfToday) {
            return DIV_TODAY
        }

        if (time >= mStartOfToday - 86400000) {
            return DIV_YESTERDAY
        }

        return if (time >= mStartOfToday - 864000000) {
            DIV_THIS_WEEK
        } else DIV_OLD
    }

    override fun getItemCount(): Int {
        return data.size
    }

    fun setClickListener(clickListener: ClickListener) {
        this.clickListener = clickListener
    }

    fun setData(data: List<Chat>) {
        this.data = data
        notifyDataSetChanged()
    }

    interface ClickListener {
        fun onClick(chat: Chat)

        fun onLongClick(chat: Chat): Boolean
    }

    inner class Holder(itemView: View) : RecyclerView.ViewHolder(itemView), Avatars.AvatarWithLetter {

        val header: View = itemView.findViewById(R.id.header_root)
        val headerText: TextView = itemView.findViewById(R.id.header_text)
        val avatar: ImageView = itemView.findViewById(R.id.avatar)
        private val avatarLetter: TextView = itemView.findViewById(R.id.avatar_letter)
        val unreadCount: TextView = itemView.findViewById(R.id.has_unread)
        val interlocutorName: TextView = itemView.findViewById(R.id.interlocutor_name)
        val time: TextView = itemView.findViewById(R.id.time)
        val body: TextView = itemView.findViewById(R.id.body)

        override fun getAvatarView(): ImageView {
            return avatar
        }

        override fun getLetterView(): TextView {
            return avatarLetter
        }
    }

    companion object {
        private val DF_TODAY = SimpleDateFormat("HH:mm", Locale.getDefault())
        private val DF_OLD = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

        private val DATE = Date()
    }
}