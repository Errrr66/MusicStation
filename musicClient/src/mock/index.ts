// 默认数据
export const trackListData = []

export const defaultSong = {
  id: '0',
  title: '未选择歌曲',
  artist: '未知歌手',
  album: '未知专辑',
  cover: new URL(`@/assets/default_album.jpg`, import.meta.url).href,
  url: '',
  duration: 0,
  likeStatus: 0,
}
