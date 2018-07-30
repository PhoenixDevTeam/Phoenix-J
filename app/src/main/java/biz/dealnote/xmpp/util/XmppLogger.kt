package biz.dealnote.xmpp.util

import android.util.Log

import biz.dealnote.xmpp.BuildConfig

object XmppLogger {

    private val DEBUG = BuildConfig.DEBUG

    fun d(tag: String, message: String) {
        if (DEBUG) {
            Log.d(tag, message)
        }
    }

    fun v(tag: String, message: String) {
        if (DEBUG) {
            Log.v(tag, message)
        }
    }

    fun i(tag: String, message: String) {
        if (DEBUG) {
            Log.i(tag, message)
        }
    }

    fun i(tag: String, message: String, thr: Throwable) {
        if (DEBUG) {
            Log.i(tag, message, thr)
        }
    }

    fun w(tag: String, message: String) {
        if (DEBUG) {
            Log.w(tag, message)
        }
    }

    fun w(tag: String, message: String, thr: Throwable) {
        if (DEBUG) {
            Log.w(tag, message, thr)
        }
    }

    fun e(tag: String, message: String) {
        if (DEBUG) {
            Log.e(tag, message)
        }
    }

    fun e(tag: String, message: String, thr: Throwable) {
        if (DEBUG) {
            Log.e(tag, message, thr)
        }
    }

}
