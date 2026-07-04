import { defineStore } from 'pinia'
import {
  getFavoritePlaylists as fetchFavoritePlaylistsApi,
  collectPlaylist as collectPlaylistApi,
  cancelCollectPlaylist as cancelCollectPlaylistApi,
} from '@/api/system'
import type { ResultTable } from '@/api/system'
import { ElMessage } from 'element-plus'
import { fixUrl } from '@/utils'
import coverImg from '@/assets/cover.png'

interface FavoritePlaylist {
  id: number
  name: string
  coverImgUrl: string
}

export const useFavoriteStore = defineStore('favorite', {
  state: () => ({
    favoritePlaylists: [] as FavoritePlaylist[],
    loading: false,
  }),

  actions: {
    // 获取收藏的歌单列表
    async getFavoritePlaylists() {
      try {
        this.loading = true
        const res = await fetchFavoritePlaylistsApi({
          pageNum: 1,
          pageSize: 50,
          title: '',
          style: '',
        })
        if (res.code === 0 && res.data) {
          const data = res.data as ResultTable['data']
          if (data?.items) {
            this.favoritePlaylists = (data.items as Array<Record<string, unknown>>).map((item) => ({
              id: item.playlistId as number,
              name: item.title as string,
              coverImgUrl: fixUrl(item.coverUrl as string) || coverImg,
            }))
          }
        }
      } catch (error) {
        ElMessage.error('获取收藏歌单失败')
      } finally {
        this.loading = false
      }
    },

    // 收藏歌单
    async collectPlaylist(playlistId: number) {
      try {
        const res = await collectPlaylistApi(playlistId)
        if (res.code === 0) {
          ElMessage.success('收藏成功')
          // 重新获取收藏列表
          await this.getFavoritePlaylists()
          return true
        }
        return false
      } catch (error) {
        ElMessage.error('收藏失败')
        return false
      }
    },

    // 取消收藏歌单
    async cancelCollectPlaylist(playlistId: number) {
      try {
        const res = await cancelCollectPlaylistApi(playlistId)
        if (res.code === 0) {
          ElMessage.success('取消收藏成功')
          // 从列表中移除
          this.favoritePlaylists = this.favoritePlaylists.filter(
            (item) => item.id !== playlistId
          )
          return true
        }
        return false
      } catch (error) {
        ElMessage.error('取消收藏失败')
        return false
      }
    },

    // 清空收藏列表
    clearFavoritePlaylists() {
      this.favoritePlaylists = []
    },
  },
})
