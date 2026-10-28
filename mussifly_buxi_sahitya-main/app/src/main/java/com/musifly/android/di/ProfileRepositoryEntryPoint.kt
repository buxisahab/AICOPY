package com.musifly.android.di

import com.musifly.android.repository.ProfileRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ProfileRepositoryEntryPoint {
    fun profileRepository(): ProfileRepository
}
