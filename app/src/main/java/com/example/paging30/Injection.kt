package com.example.paging30

import androidx.lifecycle.ViewModelProvider
import com.example.paging30.api.GithubService
import com.example.paging30.data.GithubRepository
import com.example.paging30.ui.ViewModelFactory

object Injection {
    /**
     * Creates an instance of [GithubRepository] based on the [GithubService] and a
     * [GithubLocalCache]
     */
    private fun provideGithubRepository(): GithubRepository {
        return GithubRepository(GithubService.create())
    }

    /**
     * Provides the [ViewModelProvider.Factory] that is then used to get a reference to
     * [ViewModel] objects.
     */
    fun provideViewModelFactory(): ViewModelProvider.Factory {
        return ViewModelFactory(provideGithubRepository())
    }
}