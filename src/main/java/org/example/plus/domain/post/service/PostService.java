package org.example.plus.domain.post.service;

import java.util.List;
import java.util.stream.Collectors;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.plus.common.entity.Post;
import org.example.plus.common.entity.User;
import org.example.plus.domain.post.model.dto.PostDto;
import org.example.plus.domain.post.model.dto.PostSummaryDto;
import org.example.plus.domain.post.model.request.UpdatePostRequest;
import org.example.plus.domain.post.repository.PostRepository;
import org.example.plus.domain.user.repository.UserRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final PostCacheService postCacheService;

    @Transactional
    public PostDto creatPost(String username, String content) {

        User user = userRepository.findUserByUsername(username).orElseThrow(
            ()-> new IllegalArgumentException("등록된 사용자가 없습니다.")
        );

        Post post = postRepository.save(new Post(content, user.getId()));

        return PostDto.from(post);

    }

    /*
    // @Cacheable이 하는 역할
    // 1단계: postId 기준으로 캐시에 값이 있는지 없는지 확인
    // 2단계: 값이 있으면 바로 리턴
    // 4단계: 가져온 값을 캐시에 저장
    @Cacheable(value = "postCache", key = "'id:' + #postId")
    @Transactional
    public PostDto getPostById(long postId) {
        // 3단계: 값이 없으면 직접 DB 조회
        log.info("postId: {} DB 직접 조회", postId);
        Post post = postRepository.findById(postId).orElseThrow(
                () -> new IllegalArgumentException("등록된 포스트가 없습니다")
        );
        return PostDto.from(post);
    }

    // @CachePut이 하는 역할: 캐시 갱신
    @CachePut(value = "postCache", key = "'id:' + #postId")
    @Transactional
    public PostDto updatePostById(long postId, UpdatePostRequest request) {
        Post post = postRepository.findById(postId).orElseThrow(
                () -> new IllegalArgumentException("등록된 포스트가 없습니다")
        );
        post.update(request);
        postRepository.save(post);

        return PostDto.from(post);
    }

    // @CacheEvit이 하는 역할: 캐시 삭제
    @CacheEvict(value = "postCache", key = "'id:' + #postId")
    @Transactional
    public void deletePostById(long postId) {
        postRepository.deleteById(postId);
    }

     */

    @Transactional
    public PostDto getPostById(long postId) {
        // 1단계: postId 기준으로 캐시에 값이 있는지 없는지 조회
        PostDto cached = postCacheService.getPostCache(postId);

        // 2단계: 값이 있으면 바로 리턴
        if (cached != null) {
            log.info("Redis Data Cache Hit");
            return cached;
        }

        // 3단계: 값이 없으면 직접 DB 조회
        log.info("Redis Data Cache Miss {}", postId);
        Post post = postRepository.findById(postId).orElseThrow(
                () -> new IllegalArgumentException("등록된 포스트가 없습니다")
        );

        // 4단계: DB에서 가져온 값을 캐시에 저장 -> 다음에 캐시에서 바로 가져올 수 있도록
        PostDto postDto = PostDto.from(post);
        postCacheService.savePostCache(postId, postDto);

        return postDto;
    }

    @Transactional
    public PostDto updatePostById(long postId, UpdatePostRequest request) {
        Post post = postRepository.findById(postId).orElseThrow(
                () -> new IllegalArgumentException("등록된 포스트가 없습니다")
        );
        post.update(request);
        postRepository.save(post);

        // 캐시 삭제(무효화)
        postCacheService.deletePostCache(postId);

        return PostDto.from(post);
    }
}


