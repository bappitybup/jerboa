package com.jerboa.ui.components.home

import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.jerboa.*
import com.jerboa.api.API
import com.jerboa.db.Account
import com.jerboa.db.AccountViewModel
import com.jerboa.ui.components.community.CommunityViewModel
import com.jerboa.ui.components.community.communityClickWrapper
import com.jerboa.ui.components.inbox.inboxClickWrapper
import com.jerboa.ui.components.person.PersonProfileViewModel
import com.jerboa.ui.components.person.personClickWrapper
import com.jerboa.ui.components.post.InboxViewModel
import com.jerboa.ui.components.post.PostListings
import com.jerboa.ui.components.post.PostViewModel
import com.jerboa.ui.components.post.postClickWrapper
import kotlinx.coroutines.CoroutineScope

@Composable
fun HomeActivity(
    navController: NavController,
    homeViewModel: HomeViewModel,
    communityViewModel: CommunityViewModel,
    personProfileViewModel: PersonProfileViewModel,
    postViewModel: PostViewModel,
    inboxViewModel: InboxViewModel,
    accountViewModel: AccountViewModel,
    siteViewModel: SiteViewModel,
) {

    Log.d("jerboa", "got to community activity")

    val scope = rememberCoroutineScope()
    val scaffoldState = rememberScaffoldState()
    val ctx = LocalContext.current
    val accounts by accountViewModel.allAccounts.observeAsState()
    val account = getCurrentAccount(accounts = accounts)

    Surface(color = MaterialTheme.colors.background) {
        Scaffold(
            scaffoldState = scaffoldState,
            topBar = {
                MainTopBar(
                    scope = scope,
                    scaffoldState = scaffoldState,
                    homeViewModel = homeViewModel,
                    account = account,
                    ctx = ctx,
                )
            },
            drawerShape = MaterialTheme.shapes.small,
            drawerBackgroundColor = colorShade(
                color = MaterialTheme.colors.background,
                factor = 1.2f
            ),
            drawerContent = {
                MainDrawer(
                    siteViewModel = siteViewModel,
                    accounts = accounts,
                    navController = navController,
                    accountViewModel = accountViewModel,
                    communityViewModel = communityViewModel,
                    homeViewModel = homeViewModel,
                    personProfileViewModel = personProfileViewModel,
                    inboxViewModel = inboxViewModel,
                    scope = scope,
                    scaffoldState = scaffoldState,
                    account = account,
                    ctx = ctx
                )
            },
            content = {
                MainPostListingsContent(
                    homeViewModel = homeViewModel,
                    communityViewModel = communityViewModel,
                    personProfileViewModel = personProfileViewModel,
                    postViewModel = postViewModel,
                    account = account,
                    ctx = ctx,
                    navController = navController,
                )
            }
        )
    }
}

@Composable
fun MainPostListingsContent(
    homeViewModel: HomeViewModel,
    communityViewModel: CommunityViewModel,
    personProfileViewModel: PersonProfileViewModel,
    postViewModel: PostViewModel,
    account: Account?,
    ctx: Context,
    navController: NavController,
) {
    PostListings(
        posts = homeViewModel.posts,
        onUpvoteClick = { postView ->
            homeViewModel.likePost(
                voteType = VoteType.Upvote,
                postView = postView,
                account = account,
                ctx = ctx,
            )
        },
        onDownvoteClick = { postView ->
            homeViewModel.likePost(
                voteType = VoteType.Downvote,
                postView = postView,
                account = account,
                ctx = ctx,
            )
        },
        onSaveClick = { postView ->
            homeViewModel.savePost(
                postView = postView,
                account = account,
                ctx = ctx,
            )
        },
        onPostClick = { postView ->
            postClickWrapper(
                postViewModel = postViewModel,
                postId = postView.post.id,
                account = account,
                navController = navController,
                ctx = ctx,
            )
        },
        onPostLinkClick = { url ->
            openLink(url, ctx)
        },
        onSwipeRefresh = {
            homeViewModel.fetchPosts(
                account = account,
                clear = true,
                ctx = ctx,
            )
        },
        onCommunityClick = { communityId ->
            communityClickWrapper(
                communityViewModel = communityViewModel,
                communityId = communityId,
                account = account,
                navController = navController,
                ctx = ctx,
            )
        },
        onPersonClick = { personId ->
            personClickWrapper(
                personProfileViewModel = personProfileViewModel,
                personId = personId,
                account = account,
                navController = navController,
                ctx = ctx,
            )
        },
        loading = homeViewModel.loading.value &&
            homeViewModel.page.value == 1 &&
            homeViewModel.posts.isNotEmpty(),
        isScrolledToEnd = {
            homeViewModel.fetchPosts(
                account = account,
                nextPage = true,
                ctx = ctx,
            )
        },
    )
}

@Composable
fun MainDrawer(
    siteViewModel: SiteViewModel,
    accounts: List<Account>?,
    navController: NavController,
    accountViewModel: AccountViewModel,
    communityViewModel: CommunityViewModel,
    personProfileViewModel: PersonProfileViewModel,
    inboxViewModel: InboxViewModel,
    homeViewModel: HomeViewModel,
    scope: CoroutineScope,
    scaffoldState: ScaffoldState,
    account: Account?,
    ctx: Context
) {
    Drawer(
        myUserInfo = siteViewModel.siteRes?.my_user,
        accounts = accounts,
        navController = navController,
        onSwitchAccountClick = {
            accountViewModel.removeCurrent()
            accountViewModel.setCurrent(it.id)
            API.changeLemmyInstance(it.instance)

            // Refetch the site
            siteViewModel.fetchSite(it.jwt)

            // Refetch the front page
            homeViewModel.fetchPosts(
                account = it,
                clear = true,
                ctx = ctx,
            )
            closeDrawer(scope, scaffoldState)
        },
        onSignOutClick = {
            accounts?.also { accounts ->
                getCurrentAccount(accounts)?.also {
                    accountViewModel.delete(it)
                    val updatedList = accounts.toMutableList()
                    updatedList.remove(it)

                    if (updatedList.isNotEmpty()) {
                        accountViewModel.setCurrent(updatedList[0].id)
                    }
                }
            }
        },
        onClickListingType = { listingType ->
            homeViewModel.fetchPosts(
                account = account,
                clear = true,
                changeListingType = listingType,
                ctx = ctx,
            )
            closeDrawer(scope, scaffoldState)
        },
        onCommunityClick = { communityId ->
            communityClickWrapper(
                communityViewModel,
                communityId,
                account,
                navController,
                ctx = ctx,
            )
            closeDrawer(scope, scaffoldState)
        },
        onClickProfile = {
            account?.id?.also {
                personClickWrapper(
                    personProfileViewModel = personProfileViewModel,
                    personId = it,
                    account = account,
                    navController = navController,
                    ctx = ctx,
                )
                closeDrawer(scope, scaffoldState)
            }
        },
        onClickInbox = {
            account?.id?.also {
                inboxClickWrapper(
                    inboxViewModel = inboxViewModel,
                    account = account,
                    navController = navController,
                    ctx = ctx,
                )
                closeDrawer(scope, scaffoldState)
            }
        },
    )
}

@Composable
fun MainTopBar(
    scope: CoroutineScope,
    scaffoldState: ScaffoldState,
    homeViewModel: HomeViewModel,
    account: Account?,
    ctx: Context,
) {
    Column {
        HomeHeader(
            scope, scaffoldState,
            selectedSortType = homeViewModel.sortType.value,
            selectedListingType = homeViewModel.listingType.value,
            onClickSortType = { sortType ->
                homeViewModel.fetchPosts(
                    account = account,
                    clear = true,
                    changeSortType = sortType,
                    ctx = ctx,
                )
            },
            onClickListingType = { listingType ->
                homeViewModel.fetchPosts(
                    account = account,
                    clear = true,
                    changeListingType = listingType,
                    ctx = ctx,
                )
            },
        )
        if (homeViewModel.loading.value) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
    }
}
