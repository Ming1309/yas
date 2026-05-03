package com.yas.media;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yas.media.controller.MediaController;
import com.yas.media.model.Media;
import com.yas.media.model.dto.MediaDto;
import com.yas.media.service.MediaService;
import com.yas.media.viewmodel.MediaPostVm;
import com.yas.media.viewmodel.MediaVm;
import com.yas.media.viewmodel.NoFileMediaVm;
import java.io.ByteArrayInputStream;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

@DisplayName("MediaController Unit Tests")
class MediaControllerUnitTest {

    @Mock
    private MediaService mediaService;

    private MediaController mediaController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mediaController = new MediaController(mediaService);
    }

    @Test
    @DisplayName("Should create media and return NoFileMediaVm")
    void create_shouldReturnCreatedMedia() {
        Media media = new Media();
        media.setId(1L);
        media.setCaption("Caption");
        media.setFileName("file.jpg");
        media.setMediaType("image/jpeg");

        when(mediaService.saveMedia(any(MediaPostVm.class))).thenReturn(media);

        MockMultipartFile file = new MockMultipartFile("multipartFile", "file.jpg", "image/jpeg", new byte[] {1});
        ResponseEntity<Object> response = mediaController.create(new MediaPostVm("Caption", file, null));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        NoFileMediaVm body = assertInstanceOf(NoFileMediaVm.class, response.getBody());
        assertEquals(1L, body.id());
        assertEquals("Caption", body.caption());
        assertEquals("file.jpg", body.fileName());
        assertEquals("image/jpeg", body.mediaType());
    }

    @Test
    @DisplayName("Should delete media and return no content")
    void delete_shouldReturnNoContent() {
        ResponseEntity<Void> response = mediaController.delete(1L);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertNull(response.getBody());
        verify(mediaService).removeMedia(1L);
    }

    @Test
    @DisplayName("Should return media when found")
    void get_shouldReturnOkWhenMediaExists() {
        MediaVm mediaVm = new MediaVm(1L, "Caption", "file.jpg", "image/jpeg", "http://localhost/file.jpg");
        when(mediaService.getMediaById(1L)).thenReturn(mediaVm);

        ResponseEntity<MediaVm> response = mediaController.get(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mediaVm, response.getBody());
    }

    @Test
    @DisplayName("Should return not found when media does not exist")
    void get_shouldReturnNotFoundWhenMediaMissing() {
        when(mediaService.getMediaById(1L)).thenReturn(null);

        ResponseEntity<MediaVm> response = mediaController.get(1L);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    @DisplayName("Should return media list when ids are found")
    void getByIds_shouldReturnOkWhenMediaExists() {
        List<MediaVm> mediaList = List.of(
            new MediaVm(1L, "Caption", "file.jpg", "image/jpeg", "http://localhost/file.jpg")
        );
        when(mediaService.getMediaByIds(anyList())).thenReturn(mediaList);

        ResponseEntity<List<MediaVm>> response = mediaController.getByIds(List.of(1L));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mediaList, response.getBody());
    }

    @Test
    @DisplayName("Should return not found when media list is empty")
    void getByIds_shouldReturnNotFoundWhenEmpty() {
        when(mediaService.getMediaByIds(anyList())).thenReturn(List.of());

        ResponseEntity<List<MediaVm>> response = mediaController.getByIds(List.of(1L));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    @DisplayName("Should return file with attachment header")
    void getFile_shouldReturnFileResponse() {
        MediaDto mediaDto = MediaDto.builder()
            .content(new ByteArrayInputStream("hello".getBytes()))
            .mediaType(MediaType.IMAGE_JPEG)
            .build();
        when(mediaService.getFile(eq(1L), eq("file.jpg"))).thenReturn(mediaDto);

        ResponseEntity<InputStreamResource> response = mediaController.getFile(1L, "file.jpg");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("attachment; filename=\"file.jpg\"", response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION));
        assertEquals(MediaType.IMAGE_JPEG, response.getHeaders().getContentType());
        assertNotNull(response.getBody());
    }
}