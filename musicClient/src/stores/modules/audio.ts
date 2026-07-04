import { defineStore } from 'pinia'
import piniaPersistConfig from '@/stores/helper/persist'
import { trackListData } from '@/mock'
import { AudioState, trackModel } from '@/stores/interface'
import { Song } from '@/api/interface'
/**
 * 音频
 */
export const AudioStore = defineStore({
  id: 'AudioStore',
  state: (): AudioState => ({
    // 歌曲缓存
    trackList: trackListData,
    // 当前播放歌曲索引
    currentSongIndex: 0,
    // 音量
    volume: 50,
    // 音质
    quality: 'exhigh',
    currentPageSongs: [] as Song[], // 当前页面的歌曲列表
  }),
  actions: {
    //set AudioStore
    setAudioStore<T extends keyof AudioState>(key: T, value: AudioState[T]) {
      this[key] = value
    },
    // 新增歌曲或歌曲数组到 trackList
    addTracks(newTracks: trackModel | trackModel[]) {
      // 将参数归一化为数组
      const tracksToAdd = Array.isArray(newTracks) ? newTracks : [newTracks]
      // 收集现有歌曲的ID
      const existingIds = new Set(
        this.trackList.map((track: { id: any }) => track.id)
      )
      let firstDupIndex = -1
      const newIds = new Set()
      for (const track of tracksToAdd) {
        if (existingIds.has(track.id)) {
          // 重复歌曲：仅定位第一个出现的位置
          if (firstDupIndex === -1) {
            firstDupIndex = this.trackList.findIndex(
              (existingTrack: { id: any }) => existingTrack.id === track.id
            )
          }
        } else if (!newIds.has(track.id)) {
          // 新歌：全部追加
          this.trackList.push(track)
          newIds.add(track.id)
        }
      }
      // 定位播放索引：优先定位第一个重复，否则播放第一首新歌
      if (firstDupIndex !== -1) {
        this.currentSongIndex = firstDupIndex
      } else if (newIds.size > 0) {
        this.currentSongIndex = this.trackList.length - newIds.size
      }
    },
    // 删除指定歌曲
    deleteTrack(id: number | string) {
      this.trackList = this.trackList.filter(
        (track: { id: string | number }) => track.id !== id
      )
    },
    removeTrackFromQueue(id: number | string) {
      this.deleteTrack(id)
    },
    // 设置当前页面的歌曲列表
    setCurrentPageSongs(songs: Song[]) {
      this.currentPageSongs = songs
    },
  },
  persist: piniaPersistConfig('AudioStore'),
})
