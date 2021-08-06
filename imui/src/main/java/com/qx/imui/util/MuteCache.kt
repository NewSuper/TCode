package com.qx.imui.util

class MuteCache {

    companion object {

        private var mGroupMuteList = arrayListOf<String>()
        private var mGroupAllMuteList = arrayListOf<String>()
        private var mChatRoomMemberMuteList = arrayListOf<String>()

        private var isGroupGlobalMute = false
        private var isChatRoomGlobalMute = false

        /**
         * 设置群成员禁言
         */
        fun setGroupMute(groupId: String, isMute: Boolean) {
            if (isMute) {
                if (!mGroupMuteList.contains(groupId)) {
                    mGroupMuteList.add(groupId)
                }
            } else {
                if (mGroupMuteList.contains(groupId)) {
                    mGroupMuteList.remove(groupId)
                }
            }
        }

        /**
         * 设置群整体禁言
         */
        fun setGroupAllMute(groupId: String, isMute: Boolean) {
            if (isMute) {
                if (!mGroupAllMuteList.contains(groupId)) {
                    mGroupAllMuteList.add(groupId)
                }
            } else {
                if (mGroupAllMuteList.contains(groupId)) {
                    mGroupAllMuteList.remove(groupId)
                }
            }
        }

        /**
         * 设置群全局禁言
         */
        fun setGroupGlobalMute(isMute: Boolean) {
            isGroupGlobalMute = isMute
        }

        /**
         * 设置群成员禁言
         */
        fun setChatRoomMute(chatRoomid: String, isMute: Boolean) {
            if (isMute) {
                if (!mChatRoomMemberMuteList.contains(chatRoomid)) {
                    mChatRoomMemberMuteList.add(chatRoomid)
                }
            } else {
                if (mChatRoomMemberMuteList.contains(chatRoomid)) {
                    mChatRoomMemberMuteList.remove(chatRoomid)
                }
            }
        }

        /**
         * 设置聊天室全局禁言
         */
        fun setChatRoomGlobalMute(isMute: Boolean) {
            isChatRoomGlobalMute = isMute
        }

        /**
         * 是否在某个群中被禁言，即：你在本群不能发消息
         */
        fun isGroupMute(groupId: String): Boolean {
            return mGroupMuteList.contains(groupId)
        }

        /**
         * 该群组是否启动整体禁言，即：该群所有人都不能发消息
         */
        fun isGroupAllMute(groupId: String): Boolean {
            return mGroupAllMuteList.contains(groupId)
        }

        /**
         * 是否全局群禁言，即：你在所有群中都不能发消息
         */
        fun isGroupGlobalMute(): Boolean {
            return isGroupGlobalMute
        }

        /**
         * 是否在某个聊天室中被禁言，即：你在本聊天室中不能发消息
         */
        fun isChatRoomMute(chatRoomId: String): Boolean {
            return mChatRoomMemberMuteList.contains(chatRoomId)
        }

        /**
         * 是否全局聊天室禁言，即：你在所有聊天室都不能发消息
         */
        fun isChatRoomGlobalMute(): Boolean {
            return isChatRoomGlobalMute
        }

    }

}