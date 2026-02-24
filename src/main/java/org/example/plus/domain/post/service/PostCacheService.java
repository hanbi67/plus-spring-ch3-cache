package org.example.plus.domain.post.service;

import lombok.RequiredArgsConstructor;
import org.example.plus.domain.post.model.dto.PostDto;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class PostCacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String CACHE_POST_PREFIX = "post:";

    // 캐시를 조회하는 메서드
    public PostDto getPostCache(long postId) {
        String key = CACHE_POST_PREFIX + postId;   // post:1 post:2 post:3
        return (PostDto) redisTemplate.opsForValue().get(key);
    }

    // 캐시를 저장하는 메서드
    public void savePostCache(long postId, PostDto postDto) {
        String key = CACHE_POST_PREFIX + postId;
        redisTemplate.opsForValue().set(key, postDto, 10, TimeUnit.MINUTES); // TTL 만료기간 10분
    }

    // 캐시를 삭제하는 메서드(무효화)
    public void deletePostCache(long postId) {
        String key = CACHE_POST_PREFIX + postId;
        redisTemplate.delete(key);
    }
}
