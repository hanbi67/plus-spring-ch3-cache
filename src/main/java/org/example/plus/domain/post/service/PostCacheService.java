package org.example.plus.domain.post.service;

import lombok.RequiredArgsConstructor;
import org.example.plus.domain.post.model.dto.PostDto;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class PostCacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String CACHE_POST_PREFIX = "post:";
    private static final String CACHE_POST_VIEW_COUNT_PREFIX = "post:viewCount:";

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

    // 조회된 게시글의 조회수 증가
    public void increasePostViewCount(Long postId) {
        redisTemplate.opsForZSet().incrementScore(CACHE_POST_VIEW_COUNT_PREFIX, postId.toString(), 1);
    }

    // 인기 게시글 N개 조회
    public List<Long> getTopViewPostList(int limit) {
        // Java에는 Sorted Set이라는 개념의 클래스가 없기 때문에 Set 사용
        Set<Object> postIdList = redisTemplate.opsForZSet().reverseRange(CACHE_POST_VIEW_COUNT_PREFIX, 0, limit - 1);

        // NPE 방어 코드
        if (postIdList == null || postIdList.isEmpty()) {
            return Collections.emptyList();
        }

        return postIdList.stream()
                .map(id -> Long.parseLong(id.toString()))
                .toList();
    }
}
