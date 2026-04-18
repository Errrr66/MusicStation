USE vibe_music;

CREATE TABLE IF NOT EXISTS `tb_user_follow` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '关注发起人',
  `follow_user_id` bigint NOT NULL COMMENT '被关注用户',
  `create_time` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_follow` (`user_id`, `follow_user_id`),
  KEY `idx_follow_user` (`follow_user_id`),
  CONSTRAINT `fk_follow_user_id` FOREIGN KEY (`user_id`) REFERENCES `tb_user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_follow_target_id` FOREIGN KEY (`follow_user_id`) REFERENCES `tb_user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `tb_private_message` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `from_user_id` bigint NOT NULL,
  `to_user_id` bigint NOT NULL,
  `message_type` varchar(20) NOT NULL DEFAULT 'TEXT' COMMENT 'TEXT|SONG|PLAYLIST',
  `content` varchar(1000) DEFAULT NULL,
  `song_id` bigint DEFAULT NULL,
  `playlist_id` bigint DEFAULT NULL,
  `read_status` tinyint NOT NULL DEFAULT 0 COMMENT '0未读,1已读',
  `create_time` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_private_message_sender` (`from_user_id`),
  KEY `idx_private_message_receiver` (`to_user_id`),
  KEY `idx_private_message_pair` (`from_user_id`, `to_user_id`, `id`),
  CONSTRAINT `fk_private_message_from_user` FOREIGN KEY (`from_user_id`) REFERENCES `tb_user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_private_message_to_user` FOREIGN KEY (`to_user_id`) REFERENCES `tb_user` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `fk_private_message_song` FOREIGN KEY (`song_id`) REFERENCES `tb_song` (`id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_private_message_playlist` FOREIGN KEY (`playlist_id`) REFERENCES `tb_playlist` (`id`) ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

