/*
 *
 *  ******************************************************************
 *  *  * Copyright (C) 2022
 *  *  * KizzyRepositoryImpl.kt is part of Kizzy
 *  *  *  and can not be copied and/or distributed without the express
 *  *  * permission of yzziK(Vaibhav)
 *  *  *****************************************************************
 *
 *
 */

package com.buxi.music.kizzy.repository

import com.buxi.music.kizzy.remote.ApiService
import com.buxi.music.kizzy.utils.toImageAsset
import java.io.File

/**
 * Modified by Zion Huang
 */
class KizzyRepository {
    private val api = ApiService()

    suspend fun getImage(url: String): String? {
        return api.getImage(url).toImageAsset()
    }

    suspend fun uploadImage(file: File): String? {
        return api.uploadImage(file).toImageAsset()
    }
}
