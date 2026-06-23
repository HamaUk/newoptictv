package com.streamvault.data.sync

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.streamvault.data.local.DatabaseTransactionRunner
import com.streamvault.data.local.dao.CategoryDao
import com.streamvault.data.local.dao.ChannelDao
import com.streamvault.data.local.entity.CategoryEntity
import com.streamvault.data.local.entity.ChannelEntity
import com.streamvault.domain.model.ContentType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseGlobalPlaylistSync @Inject constructor(
    private val firebaseDatabase: FirebaseDatabase,
    private val channelDao: ChannelDao,
    private val categoryDao: CategoryDao,
    private val transactionRunner: DatabaseTransactionRunner
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private var isSyncing = false

    companion object {
        const val GLOBAL_PROVIDER_ID = 1L
    }

    fun startSyncing() {
        if (isSyncing) return
        isSyncing = true

        val dbRef = firebaseDatabase.getReference("sync/global")

        dbRef.child("channelGroups").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                scope.launch {
                    val groups = mutableListOf<CategoryEntity>()
                    val children = snapshot.children
                    children.forEachIndexed { index, groupSnapshot ->
                        val key = groupSnapshot.key ?: return@forEachIndexed
                        val name = groupSnapshot.child("name").getValue(String::class.java) ?: "Unknown"
                        val order = groupSnapshot.child("order").getValue(Int::class.java) ?: 999999
                        
                        // We use hash of the key to generate a stable categoryId
                        val categoryId = key.hashCode().toLong() and 0xFFFFFFFFL
                        
                        groups.add(
                            CategoryEntity(
                                categoryId = categoryId,
                                name = name,
                                type = ContentType.LIVE,
                                providerId = GLOBAL_PROVIDER_ID,
                                syncFingerprint = order.toString()
                            )
                        )
                    }

                    transactionRunner.inTransaction {
                        categoryDao.deleteByProviderAndType(GLOBAL_PROVIDER_ID, ContentType.LIVE.name)
                        categoryDao.insertAll(groups)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle cancellation
            }
        })

        dbRef.child("managedPlaylist").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                scope.launch {
                    val channels = mutableListOf<ChannelEntity>()
                    snapshot.children.forEach { channelSnapshot ->
                        val name = channelSnapshot.child("name").getValue(String::class.java) ?: return@forEach
                        val url = channelSnapshot.child("url").getValue(String::class.java) ?: return@forEach
                        val group = channelSnapshot.child("group").getValue(String::class.java) ?: channelSnapshot.child("category").getValue(String::class.java) ?: "General"
                        val logo = channelSnapshot.child("logo").getValue(String::class.java) ?: channelSnapshot.child("icon_url").getValue(String::class.java)
                        val order = channelSnapshot.child("order").getValue(Int::class.java) ?: 999999
                        val type = channelSnapshot.child("type").getValue(String::class.java) ?: "live"

                        // Stable Stream ID based on URL
                        val streamId = url.hashCode().toLong() and 0xFFFFFFFFL
                        val categoryId = group.hashCode().toLong() and 0xFFFFFFFFL

                        channels.add(
                            ChannelEntity(
                                streamId = streamId,
                                name = name,
                                streamUrl = url,
                                categoryId = categoryId,
                                categoryName = group,
                                logoUrl = logo,
                                providerId = GLOBAL_PROVIDER_ID,
                                number = order
                            )
                        )
                    }

                    transactionRunner.inTransaction {
                        channelDao.deleteByProvider(GLOBAL_PROVIDER_ID)
                        channelDao.insertAll(channels)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle cancellation
            }
        })
    }
}
