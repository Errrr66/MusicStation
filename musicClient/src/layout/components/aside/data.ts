export const MenuData = [
  {
    title: '',
    children: [
      { title: '推荐', icon: 'ri:home-smile-2-line', router: '/' },
      {
        title: '聊天',
        icon: 'ri:question-answer-line',
        router: '/chat',
      },
    ],
  },
  {
    title: '发现',
    children: [
      { title: '曲库', icon: 'ri:music-2-line', router: '/library' },
      {
        title: '艺人',
        icon: 'ri:mic-line',
        router: '/artist',
      },
      {
        title: '歌单',
        icon: 'ri:album-line',
        router: '/playlist',
      },
    ],
  },
  {
    title: '我的',
    children: [
      { title: '喜欢', icon: 'ri:heart-line', router: '/like' },
      { title: '私信', icon: 'ri:chat-private-line', router: '/messages' },
      {
        title: '资料',
        icon: 'mi:user',
        router: '/user',
      },
    ],
  },
]
