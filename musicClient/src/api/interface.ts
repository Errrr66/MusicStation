export interface PlaylistSong {
  songId: number
  songName: string
  artistName: string
  album: string
  duration: string
  coverUrl: string | null
  audioUrl: string
  likeStatus: number
  releaseTime: string | null
}

export interface PlaylistComment {
  commentId: number
  userId?: number
  username: string
  userAvatar: string | null
  content: string
  createTime: string
  likeCount: number
}

export interface PlaylistDetail {
  playlistId: number
  title: string
  coverUrl: string | null
  introduction: string
  songs: PlaylistSong[]
  likeStatus: number
  comments: PlaylistComment[]
  isCollected: boolean
}

// 导出 Song 类型
export interface Song {
  songId: number
  songName: string
  artistName: string
  album: string
  duration: string
  coverUrl: string
  audioUrl: string
  likeStatus: number
  releaseTime: string
}

export interface Comment {
  commentId: number
  userId?: number
  username: string
  userAvatar: string | null
  content: string
  createTime: string
  likeCount: number
}

export interface SongDetail {
  songId: number
  songName: string
  artistName: string
  album: string
  lyric: string | null
  duration: string
  coverUrl: string
  audioUrl: string
  releaseTime: string
  likeStatus: boolean | null
  comments: Comment[]
}

export interface UserSimple {
  userId: number
  username: string
  userAvatar?: string | null
  introduction?: string | null
  following?: boolean
  mutualFollow?: boolean
}

export interface UserProfile {
  userId: number
  username: string
  userAvatar?: string | null
  introduction?: string | null
  following: boolean
  mutualFollow: boolean
  followerCount: number
  followingCount: number
  followers: UserSimple[]
  followingUsers: UserSimple[]
  favoriteSongs: Song[]
  favoritePlaylists: PlaylistDetail[]
}

export interface Conversation {
  friendUserId: number
  friendUsername: string
  friendAvatar?: string | null
  lastMessage?: string | null
  lastMessageType?: string
  unreadCount: number
  updatedAt: string
}

export interface PrivateMessage {
  id: number
  fromUserId: number
  toUserId: number
  messageType: 'TEXT' | 'SONG' | 'PLAYLIST' | string
  content?: string | null
  songId?: number | null
  songName?: string | null
  songArtistName?: string | null
  songCoverUrl?: string | null
  playlistId?: number | null
  playlistTitle?: string | null
  playlistCoverUrl?: string | null
  fromUsername?: string
  fromUserAvatar?: string | null
  createTime: string
}

