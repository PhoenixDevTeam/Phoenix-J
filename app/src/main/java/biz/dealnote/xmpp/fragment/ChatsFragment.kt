package biz.dealnote.xmpp.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import biz.dealnote.mvp.core.IPresenterFactory
import biz.dealnote.xmpp.Constants
import biz.dealnote.xmpp.R
import biz.dealnote.xmpp.activity.ActivityUtils
import biz.dealnote.xmpp.adapter.ChatsAdapter
import biz.dealnote.xmpp.callback.PicassoPauseOnScrollListener
import biz.dealnote.xmpp.fragment.base.BaseMvpFragment
import biz.dealnote.xmpp.model.Chat
import biz.dealnote.xmpp.mvp.presenter.ChatsPresenter
import biz.dealnote.xmpp.mvp.view.IChatsView
import biz.dealnote.xmpp.place.PlaceFactory

class ChatsFragment : BaseMvpFragment<ChatsPresenter, IChatsView>(), ChatsAdapter.ClickListener, IChatsView {

    private var mEmptyText: TextView? = null
    private var mAdapter: ChatsAdapter? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_chats, container, false)
        mEmptyText = view.findViewById(R.id.empty)

        val recyclerView = view.findViewById<RecyclerView>(R.id.list)
        recyclerView.addOnScrollListener(PicassoPauseOnScrollListener(Constants.PICASSO_TAG))

        val manager = LinearLayoutManager(activity, RecyclerView.VERTICAL, false)
        recyclerView.layoutManager = manager

        mAdapter = ChatsAdapter(emptyList(), activity!!)
        mAdapter!!.setClickListener(this)
        recyclerView.adapter = mAdapter
        mEmptyText?.setOnClickListener { println(it)}
        return view
    }

    override fun onResume() {
        super.onResume()
        val actionBar = ActivityUtils.supportToolbarFor(this)
        if (actionBar != null) {
            actionBar.setTitle(R.string.chats)
            actionBar.subtitle = null
        }
    }

    override fun onClick(chat: Chat) {
        presenter.fireChatClick(chat)
    }

    override fun onLongClick(chat: Chat): Boolean {
        //TODO Impl
        return false
    }

    override fun getPresenterFactory(saveInstanceState: Bundle?): IPresenterFactory<ChatsPresenter> {
        return IPresenterFactory { ChatsPresenter(saveInstanceState) }
    }

    override fun displayData(chats: List<Chat>) {
        mAdapter?.setData(chats)
    }

    override fun notifyDataChanged() {
        mAdapter?.notifyDataSetChanged()
    }

    override fun setEmptyTextVisible(visible: Boolean) {
        mEmptyText?.visibility = if (visible) View.VISIBLE else View.INVISIBLE
    }

    override fun goToChat(accountId: Int, destination: String, chatId: Int?) {
        PlaceFactory.getChatPlace(accountId, destination, chatId).tryOpenWith(requireActivity())
    }

    companion object {
        @JvmStatic
        fun newInstance(): ChatsFragment {
            return ChatsFragment()
        }
    }
}