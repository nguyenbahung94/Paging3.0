package com.example.paging30.data

import com.example.paging30.api.GithubService
import com.example.paging30.api.IN_QUALIFIER
import com.example.paging30.model.Repo
import com.example.paging30.model.RepoSearchResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import retrofit2.HttpException
import java.io.IOException

private const val GITHUB_STARTING_PAGE_INDEX = 1

@ExperimentalCoroutinesApi
class GithubRepository(private val service: GithubService) {

    // keep the list of all results received
    private val inMemoryCache = mutableListOf<Repo>()

    // keep channel of results. The channel allows us to broadcast updates so
    // the subscriber will have the latest data
    private val searchResults = ConflatedBroadcastChannel<RepoSearchResult>()

    // keep the last requested page. When the request is successful, increment the page number.
    private var lastRequestedPage = GITHUB_STARTING_PAGE_INDEX

    // avoid triggering multiple requests in the same time
    private var isRequestInProgress = false


    suspend fun retry(query: String) {
        if (isRequestInProgress) return
        requestAndSaveData(query)
    }

    suspend fun requestMore(query: String) {
        if (isRequestInProgress) return
        val successful = requestAndSaveData(query)
        if (successful) {
            lastRequestedPage++
        }
    }


    suspend fun getSearchResultStream(query: String): Flow<RepoSearchResult> {
        lastRequestedPage = 1
        inMemoryCache.clear()
        requestAndSaveData(query)
        return searchResults.asFlow()
    }

    private suspend fun requestAndSaveData(query: String): Boolean {
        isRequestInProgress = true
        var successful = false
        val apiQueri = query + IN_QUALIFIER
        try {
            val response = service.searchRepos(apiQueri, lastRequestedPage, NETWORK_PAGE_SIZE)
            val repos = response.items ?: emptyList()
            inMemoryCache.addAll(repos)
            val reposeByName = reposByName(query)
            searchResults.offer(RepoSearchResult.Success(reposeByName))
            successful = true

        } catch (exception: IOException) {
            searchResults.offer(RepoSearchResult.Error(exception))
        } catch (exception: HttpException) {
            searchResults.offer(RepoSearchResult.Error(exception))
        }
        isRequestInProgress = false
        return successful
    }

    private fun reposByName(query: String): List<Repo> {
        return inMemoryCache.filter {
            it.name.contains(query, true) || (it.description != null && it.description.contains(
                query,
                true
            ))
        }.sortedWith(compareByDescending<Repo> { it.stars }.thenBy { it.name })
    }

    companion object {
        private const val NETWORK_PAGE_SIZE = 50
    }
}