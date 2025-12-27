package com.github.yumelira.yumebox.data.store

import com.tencent.mmkv.MMKV

class MMKVProvider {

    fun getDefaultMMKV(): MMKV {
        return MMKV.defaultMMKV()
    }

    fun getMMKV(id: String): MMKV {
        return MMKV.mmkvWithID(id)
    }
}



