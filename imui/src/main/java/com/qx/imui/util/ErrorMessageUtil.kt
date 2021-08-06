package com.qx.imui.util

import android.content.Context
import com.qx.imui.R
import com.qx.message.Conversation
import com.qx.message.QXError

object ErrorMessageUtil {
    fun getErrorMessage(type : String, error : QXError, context: Context) : String {
        when(error) {
            QXError.MESSAGE_NO_ACCESS -> {
                if(type == Conversation.Type.TYPE_GROUP) {
                    return context.resources.getString(R.string.qx_msg_send_failed_no_access_group)
                } else if(type == Conversation.Type.TYPE_CHAT_ROOM) {
                    return context.resources.getString(R.string.qx_msg_send_failed_no_access_chat_room)
                }
            }

            QXError.MESSAGE_TARGET_NON_EXIST -> {
                if(type == Conversation.Type.TYPE_GROUP) {
                    return context.resources.getString(R.string.qx_msg_send_failed_no_exist_group)
                } else if(type == Conversation.Type.TYPE_CHAT_ROOM) {
                    return context.resources.getString(R.string.qx_msg_send_failed_no_exist_chat_room)
                }
            }

            QXError.MESSAGE_SEND_TIME_OUT -> {
                return context.resources.getString(R.string.qx_msg_send_failed_time_out)
            }

            QXError.MESSAGE_USER_MUTE -> {
                return context.resources.getString(R.string.qx_msg_send_failed_mute)
            }
            QXError.MESSAGE_UNSUPPORT_TYPE -> {
                return context.resources.getString(R.string.qx_msg_send_failed_not_support_message_type)
            }

            QXError.MESSAGE_PARAM_TOO_LONG -> {
                return context.resources.getString(R.string.qx_msg_send_failed_content_too_long)
            }

            QXError.MESSAGE_BLACK_LIST -> {
                //  return context.resources.getString(R.string.qx_msg_send_failed_black_list)
                return context.resources.getString(R.string.qx_load_request_fail)
            }

            QXError.PARAMS_INCORRECT -> {
                return context.resources.getString(R.string.qx_msg_send_failed_param_error)
            }

            QXError.CONNECTION_DUPLICATE_AUTH -> {
                return context.resources.getString(R.string.qx_error_duplicate_auth)
            }

            QXError.DB_NO_ROW_FOUND -> {
                return context.resources.getString(R.string.qx_error_db_not_found)
            }

            QXError.LOCAL_FILE_URI_ERROR -> {
                return context.resources.getString(R.string.qx_error_file_uri_not_found)
            }

            QXError.OPERATE_RECALL_FAILED -> {
                return context.resources.getString(R.string.qx_error_recall_failed)
            }
        }
        return ""
    }

}