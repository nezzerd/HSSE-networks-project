package com.searchengine.service;

import com.searchengine.entity.Page;
import com.searchengine.repository.PageRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PageServiceTest {

    @Mock
    PageRepository pageRepository;

    @InjectMocks
    PageService pageService;

    @Test
    void existsByUrlHash_delegatesToRepository() {
        when(pageRepository.existsByUrlHash("abc")).thenReturn(true);
        assertThat(pageService.existsByUrlHash("abc")).isTrue();
    }

    @Test
    void findByUrlHash_returnsEmpty_whenNotFound() {
        when(pageRepository.findByUrlHash("xyz")).thenReturn(Optional.empty());
        assertThat(pageService.findByUrlHash("xyz")).isEmpty();
    }

    @Test
    void countFetched_returnsRepositoryValue() {
        when(pageRepository.countByStatus(Page.PageStatus.FETCHED)).thenReturn(42L);
        assertThat(pageService.countFetched()).isEqualTo(42L);
    }
}
