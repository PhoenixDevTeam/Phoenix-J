package biz.dealnote.xmpp.fragment

import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import biz.dealnote.mvp.core.IPresenterFactory
import biz.dealnote.xmpp.Constants
import biz.dealnote.xmpp.R
import biz.dealnote.xmpp.activity.ActivityUtils
import biz.dealnote.xmpp.adapter.ChatsAdapter
import biz.dealnote.xmpp.callback.PicassoPauseOnScrollListener
import biz.dealnote.xmpp.dialog.ChatContextDialog
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
        recyclerView.addOnScrollListener(PicassoPauseOnScrollListener(requireActivity(), Constants.PICASSO_TAG))

        val manager = LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
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
        presenter.fireChatLongClick(chat)
        return true
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

    override fun displayChatOptionsDialog(chat: Chat) {
        val dialog = ChatContextDialog.newInstance(chat)
        dialog.setTargetFragment(this, REQUEST_CODE_EDIT_CHAT)
        dialog.show(requireFragmentManager(), "edit_chat")
    }

    override fun goToChat(accountId: Int, destination: String, chatId: Int?) {
        PlaceFactory.getChatPlace(accountId, destination, chatId).tryOpenWith(requireActivity())
    }

    companion object {
        private const val REQUEST_CODE_EDIT_CHAT = 13

        @JvmStatic
        fun newInstance(): ChatsFragment {
            return ChatsFragment()
        }
    }
}